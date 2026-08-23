/* Copyright (c) 2013 Alan Woolley
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#include <jni.h>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <android/log.h>
#include "native_interface.hpp"
#include <pthread.h>
#include <sstream>
#include <codecvt>

#define LOGI(...) __android_log_write(2,"innoextract", __VA_ARGS__)

JavaVM *jvm;

int filedes_stdout[2], filedes_stderr[2];
FILE *inputFile_stdout, *inputFile_stderr;
pthread_t readThread_stdout, readThread_stderr;

jclass serviceCls;
jobject serviceObj;
jclass fileProxyCls;

std::stringstream outStream;
std::stringstream errStream;

long gogId;
std::string name;
std::string version;

typedef struct {
  FILE *inputFile;
  int streamNumber;

} thread_data;

extern "C" int Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeExtract(
    JNIEnv *env, jobject obj, jint toExtractFd, jstring extractDirObj) {

  // Get parameters

  int fd = (int) toExtractFd;

  if (fd == 0) {
    LOGI("Extract file path is null");
    return -1;
  }

  std::string fdStr = std::to_string(fd);
  const char *extractDir = env->GetStringUTFChars(extractDirObj, nullptr);

  if (extractDir == nullptr) {
    LOGI("Extract target dir is null");
    return -1;
  }

  char *args[9];
  args[0] = (char *) "Extractor";
  args[1] = (char *) "-e";
  args[2] = (char *) fdStr.c_str();
  args[3] = (char *) "-d";
  args[4] = (char *) extractDir;
  args[5] = (char *) "-T";
  args[6] = (char *) "none";
  args[7] = (char *) "-p0";
  args[8] = (char *) "-c0";

  int out = main(9, (char **) args);
  fflush(stdout);
  fflush(stderr);

  // Release strings
  env->ReleaseStringUTFChars(extractDirObj, extractDir);

  usleep(1000);
  return out;
}

extern "C" jobject Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeCheck(
    JNIEnv *env, jobject obj, jint toExtractObj) {
  // Get parameters

  int fd = (int) toExtractObj;

  gogId = -1;
  version = "";
  name = "";
  int out = -1;

  if (fd == 0) {
    LOGI("Extract file path is null");
  } else {
    std::string fdStr = std::to_string(fd);
    char *args[3];
    args[0] = (char *) "Extractor";
    args[1] = (char *) "--info";
    args[2] = (char *) fdStr.c_str();

    out = main(3, (char **) args);
    fflush(stdout);
    fflush(stderr);
  }

  jclass outCls = env->FindClass("uk/co/armedpineapple/innoextract/service/InnoValidationResult");
  jmethodID constructor = env->GetMethodID(outCls, "<init>", "(ZJ[B[B)V");

  jbyteArray nameArray = env->NewByteArray(name.size());
  env->SetByteArrayRegion(nameArray,0,name.size(), (const jbyte*) name.c_str());

  jbyteArray versionArray = env->NewByteArray(version.size());
  env->SetByteArrayRegion(versionArray,0,version.size(), (const jbyte*) version.c_str());

  jobject outObj = env->NewObject(outCls, constructor, (out == 0), (jlong) gogId, nameArray, versionArray);
  return outObj;
}

void *readchar(void *data) {

  JNIEnv *env;

  auto *td = (thread_data *) data;

  char c;
  while ((c = fgetc(td->inputFile))) {
    std::stringstream *stream;
    if (td->streamNumber == 1) {
      stream = &outStream;

    } else if (td->streamNumber == 2) {
      stream = &errStream;
    }

    *stream << c;

    if (c == '\n') {

      // Attach thread
      jvm->AttachCurrentThread(&env, nullptr);
      jmethodID mid = env->GetMethodID(serviceCls, "gotString",
                                       "(Ljava/lang/String;I)V");

      if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
      }

      if (mid == nullptr) {
        LOGI("Method is null");
      }

      std::string newStr = stream->str();
      stream->str("");
      jstring toSend = env->NewStringUTF(newStr.c_str());
      env->CallVoidMethod(serviceObj, mid, toSend, td->streamNumber);
      env->DeleteLocalRef(toSend);
    }
  }

  pthread_exit(nullptr);
}

extern "C" void Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativePrepare(
    JNIEnv *env, jobject obj) {
  env->GetJavaVM(&jvm);
  jclass cls = env->GetObjectClass(obj);

  serviceCls = (jclass) env->NewGlobalRef(cls);
  serviceObj = (jobject) env->NewGlobalRef(obj);
  fileProxyCls = static_cast<jclass>(env->NewGlobalRef(env->FindClass(
      "uk/co/armedpineapple/innoextract/service/OutputFile")));

  setvbuf(stdout, 0, _IOLBF, 0);
  setvbuf(stderr, 0, _IONBF, 0);

  pipe(filedes_stdout);
  pipe(filedes_stderr);

  dup2(filedes_stdout[1], STDOUT_FILENO);
  dup2(filedes_stderr[1], STDERR_FILENO);

  inputFile_stdout = fdopen(filedes_stdout[0], "r");
  inputFile_stderr = fdopen(filedes_stderr[0], "r");

  auto *outdata = (thread_data *) malloc(sizeof(thread_data));
  outdata->inputFile = inputFile_stdout;
  outdata->streamNumber = STDOUT_FILENO;

  auto *errdata = (thread_data *) malloc(sizeof(thread_data));
  errdata->inputFile = inputFile_stderr;
  errdata->streamNumber = STDERR_FILENO;

  pthread_create(&readThread_stdout, nullptr, readchar, (void *) outdata);
  pthread_create(&readThread_stderr, nullptr, readchar, (void *) errdata);
}

jobject getOutputFile(const std::string &path) {
  JNIEnv *env;
  jvm->AttachCurrentThread(&env, nullptr);
  jmethodID mid = env->GetMethodID(serviceCls,
                                   "newFile",
                                   "([B)Luk/co/armedpineapple/innoextract/service/OutputFile;");
  jbyteArray arr = env->NewByteArray(path.length());
  env->SetByteArrayRegion(arr, 0, path.length(), (jbyte *) path.c_str());

  jobject fileProxy = env->CallObjectMethod(serviceObj, mid, arr);
  env->DeleteLocalRef(arr);
  return env->NewGlobalRef(fileProxy);
}

void closeFileProxy(const jobject &proxy) {
  JNIEnv *env;
  jvm->AttachCurrentThread(&env, nullptr);
  jmethodID mid = env->GetMethodID(fileProxyCls, "close", "()V");
  env->CallVoidMethod(const_cast<jobject>(proxy), mid);
}

std::string getFileProxyPath(const jobject &proxy) {
  JNIEnv *env;
  jvm->AttachCurrentThread(&env, nullptr);
  jmethodID mid = env->GetMethodID(fileProxyCls, "getPathUtf8", "()[B");
  auto result = (jbyteArray) env->CallObjectMethod(const_cast<jobject>(proxy), mid);
  int len = env->GetArrayLength(result);
  std::string buff(len, 0);
  env->GetByteArrayRegion(result, 0, len,
                          const_cast<jbyte *>(reinterpret_cast<const jbyte *>(buff.data())));
  return buff;
}

void updateGogId(const std::string& newId) {
  gogId = std::stol(newId);
}

void updateName(const std::string& newName) {
  name = newName;
}

void updateVersion(const std::string& newVersion) {
  version = newVersion;
}

void updateProgress(const uint64_t extracted, const uint64_t total) {
  JNIEnv *env;
  jvm->AttachCurrentThread(&env, nullptr);
  jmethodID mid = env->GetMethodID(serviceCls,
                                   "updateProgress",
                                   "(JJ)V");
  env->CallVoidMethod(serviceObj, mid, static_cast<jlong>(extracted), static_cast<jlong>(total));
}

void updateCurrentFile(const std::string& currentFile) {
  JNIEnv* env;
  jvm->AttachCurrentThread(&env, nullptr);

  jstring jCurrentFile = env->NewStringUTF(currentFile.c_str());
  jmethodID mid = env->GetMethodID(serviceCls, "updateCurrentFile", "(Ljava/lang/String;)V");
  env->CallVoidMethod(serviceObj, mid, jCurrentFile);

  env->DeleteLocalRef(jCurrentFile);
}
