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
#include <stdio.h>
#include <cstdlib>
#include <android/log.h>
#include "native_interface.hpp"
#include <pthread.h>
#include <sstream>

#define LOGI(...) __android_log_write(2,"innoextract", __VA_ARGS__)

JavaVM* jvm;

int filedes_stdout[2], filedes_stderr[2];
FILE *inputFile_stdout, *inputFile_stderr;
pthread_t readThread_stdout, readThread_stderr;

jclass mainClass;
jobject mainObject;

std::stringstream outStream;
std::stringstream errStream;

typedef struct {
	FILE *inputFile;
	int streamNumber;

} thread_data;

extern "C" int Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeDoExtract(
		JNIEnv* env, jclass cls, jstring toExtractObj, jstring extractDirObj) {

	// Get parameters

	const char *toExtract = env->GetStringUTFChars(toExtractObj, NULL);
	const char *extractDir = env->GetStringUTFChars(extractDirObj, NULL);

	if (toExtract == NULL) {
		LOGI("Extract file path is null");
		return -1;
	}
	if (extractDir == NULL) {
		LOGI("Extract target dir is null");
		return -1;
	}

	char* args[9];
	args[0] = (char*) "Extractor";
	args[1] = (char*) "-e";
	args[2] = (char*) toExtract;
	args[3] = (char*) "-d";
	args[4] = (char*) extractDir;
	args[5] = (char*) "-T";
	args[6] = (char*) "none";
	args[7] = (char*) "-p0";
	args[8] = (char*) "-c0";

	int out = main(9, (char**) args);
	fflush (stdout);
	fflush (stderr);

	// Release strings
	env->ReleaseStringUTFChars(toExtractObj, toExtract);
	env->ReleaseStringUTFChars(extractDirObj, extractDir);

	usleep(1000);
	return out;
}

extern "C" int Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeCheckInno(
		JNIEnv* env, jclass cls, jstring toExtractObj) {
		// Get parameters

        const char *toExtract = env->GetStringUTFChars(toExtractObj, NULL);

        if (toExtract == NULL) {
            LOGI("Extract file path is null");
            return -1;
        }

        char* args[3];
        args[0] = (char*) "Extractor";
        args[1] = (char*) "--check";
        args[2] = (char*) toExtract;

        int out = main(3, (char**) args);
        fflush (stdout);
        fflush (stderr);

        // Release strings
        env->ReleaseStringUTFChars(toExtractObj, toExtract);

        return out;
}

void *readchar(void* data) {

	JNIEnv* env;

	thread_data* td = (thread_data*) data;

	char c;
	while ((c = fgetc(td->inputFile))) {
		std::stringstream * stream;
		if (td->streamNumber == 1) {
			stream = &outStream;

		} else if (td->streamNumber == 2) {
			stream = &errStream;
		}

		*stream << c;

		if (c == '\n') {

			// Attach thread
			jvm->AttachCurrentThread(&env, NULL);
			jmethodID mid = env->GetMethodID(mainClass, "gotString",
					"(Ljava/lang/String;I)V");

			if (env->ExceptionCheck()) {
				env->ExceptionDescribe();
				env->ExceptionClear();
			}

			if (mid == NULL) {
				LOGI("Method is null");
			}

			std::string newStr = stream->str();
			stream->str("");
			jstring toSend = env->NewStringUTF(newStr.c_str());
			env->CallVoidMethod(mainObject, mid, toSend, td->streamNumber);
			env->DeleteLocalRef(toSend);
		}
	}

    pthread_exit(NULL);
}

extern "C" void Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeInit(
		JNIEnv *env, jobject obj, jclass clazz, jint fileno) {
	int data = 0;

	env->GetJavaVM(&jvm);
	jclass cls = env->GetObjectClass(obj);

	mainClass = (jclass) env->NewGlobalRef(cls);
	mainObject = (jobject) env->NewGlobalRef(obj);

    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    pipe(filedes_stdout);
	pipe(filedes_stderr);

	dup2(filedes_stdout[1], STDOUT_FILENO);
	dup2(filedes_stderr[1], STDERR_FILENO);

	inputFile_stdout = fdopen(filedes_stdout[0], "r");
	inputFile_stderr = fdopen(filedes_stderr[0], "r");

	thread_data *outdata = (thread_data*) malloc(sizeof(thread_data));
	outdata->inputFile = inputFile_stdout;
	outdata->streamNumber = STDOUT_FILENO;

	thread_data *errdata = (thread_data*) malloc(sizeof(thread_data));
	errdata->inputFile = inputFile_stderr;
	errdata->streamNumber = STDERR_FILENO;

	pthread_create(&readThread_stdout, NULL, readchar, (void*) outdata);
	pthread_create(&readThread_stderr, NULL, readchar, (void*) errdata);
}

