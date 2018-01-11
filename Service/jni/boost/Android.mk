LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libboost_date_time
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_date_time-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_iostreams
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_iostreams-clang-mt-1_65_1.a
LOCAL_STATIC_LIBRARIES := libbz2 liblzma
LOCAL_CPP_FLAGS := -lz
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_iostreams_zlib
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_zlib-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_iostreams_bzip2
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_bzip2-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_filesystem
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_filesystem-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_program_options
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_program_options-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libboost_system
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libboost_system-clang-mt-1_65_1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(TARGET_ARCH_ABI)/include/boost-1_65_1
include $(PREBUILT_STATIC_LIBRARY)