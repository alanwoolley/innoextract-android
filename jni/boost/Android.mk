LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libboost_date_time
LOCAL_SRC_FILES := lib/libboost_date_time-gcc-mt-1_53.a
LOCAL_C_INCLUDES := include/boost-1_53/boost

include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := libboost_iostreams
LOCAL_SRC_FILES := lib/libboost_iostreams-gcc-mt-1_53.a
LOCAL_STATIC_LIBRARIES := libbz2 liblzma
LOCAL_CPP_FLAGS := -lz
LOCAL_C_INCLUDES := include/boost-1_53/boost

include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := libboost_filesystem
LOCAL_SRC_FILES := lib/libboost_filesystem-gcc-mt-1_53.a
LOCAL_C_INCLUDES := include/boost-1_53/boost

include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := libboost_program_options
LOCAL_SRC_FILES := lib/libboost_program_options-gcc-mt-1_53.a
LOCAL_C_INCLUDES := include/boost-1_53/boost

include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := libboost_system
LOCAL_SRC_FILES := lib/libboost_system-gcc-mt-1_53.a
LOCAL_C_INCLUDES := include/boost-1_53/boost

include $(PREBUILT_STATIC_LIBRARY)