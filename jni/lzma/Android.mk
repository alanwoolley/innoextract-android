LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := liblzma
LOCAL_SRC_FILES := lib/liblzma.a
LOCAL_C_INCLUDES := include/ \
					/ 

include $(PREBUILT_STATIC_LIBRARY)