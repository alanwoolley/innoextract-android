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

#ifndef NATIVE_INTERFACE_H_
#define NATIVE_INTERFACE_H_

#include <string>
#include "jni.h"

int main(int argc, char * argv[]);

void updateGogId(const std::string& newId);
void updateName(const std::string& newName);
void updateVersion(const std::string& newVersion);
void updateProgress(const uint64_t extracted, const uint64_t total);
void updateCurrentFile(const std::string& currentFile);

jobject getOutputFile(const std::string &path);
std::string getFileProxyPath(const jobject &proxy);
void closeFileProxy(const jobject &proxy);

extern "C" void Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativePrepare(
		JNIEnv *env, jobject obj);

extern "C" jobject Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeCheck(
        JNIEnv* env, jobject obj, jint toExtractObj);

extern "C" int Java_uk_co_armedpineapple_innoextract_service_ExtractService_nativeExtract(
		JNIEnv* env, jobject obj, jint toExtractFd, jstring extractDirObj);

#endif