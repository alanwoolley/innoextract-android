LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := innoextract

SRC_DIR := src
LIBICONV_INC_DIR := ../libiconv-1.13.1/include

LOCAL_C_INCLUDES :=	$(LOCAL_PATH)/$(SRC_DIR) \
					$(LOCAL_PATH)/ \
					$(LOCAL_PATH)/$(LIBICONV_INC_DIR)

LOCAL_STATIC_LIBRARIES := libbz2 liblzma libboost_program_options libiconv libboost_date_time libboost_filesystem libboost_system iconv libboost_iostreams libboost_iostreams_zlib libboost_iostreams_bzip2
LOCAL_CPP_FEATURES := exceptions rtti
LOCAL_LDLIBS := -lz -llog
LOCAL_C_FLAGS := -DUSE_LZMA=1 -DUSE_STATIC_LIBS=1

# Add your application source files here...
LOCAL_SRC_FILES :=	$(SRC_DIR)/cli/debug.cpp \
					$(SRC_DIR)/cli/main.cpp \
					$(SRC_DIR)/crypto/adler32.cpp \
					$(SRC_DIR)/crypto/checksum.cpp \
					$(SRC_DIR)/crypto/crc32.cpp \
					$(SRC_DIR)/crypto/hasher.cpp \
					$(SRC_DIR)/crypto/md5.cpp \
					$(SRC_DIR)/crypto/sha1.cpp \
					$(SRC_DIR)/loader/offsets.cpp \
					$(SRC_DIR)/loader/exereader.cpp \
					$(SRC_DIR)/setup/component.cpp \
					$(SRC_DIR)/setup/data.cpp \
					$(SRC_DIR)/setup/delete.cpp \
					$(SRC_DIR)/setup/directory.cpp \
					$(SRC_DIR)/setup/expression.cpp \
					$(SRC_DIR)/setup/file.cpp \
					$(SRC_DIR)/setup/filename.cpp \
					$(SRC_DIR)/setup/header.cpp \
					$(SRC_DIR)/setup/icon.cpp \
					$(SRC_DIR)/setup/info.cpp \
					$(SRC_DIR)/setup/ini.cpp \
					$(SRC_DIR)/setup/item.cpp \
					$(SRC_DIR)/setup/language.cpp \
					$(SRC_DIR)/setup/message.cpp \
					$(SRC_DIR)/setup/permission.cpp \
					$(SRC_DIR)/setup/registry.cpp \
					$(SRC_DIR)/setup/run.cpp \
					$(SRC_DIR)/setup/task.cpp \
					$(SRC_DIR)/setup/type.cpp \
					$(SRC_DIR)/setup/windows.cpp \
					$(SRC_DIR)/setup/version.cpp \
					$(SRC_DIR)/stream/block.cpp \
					$(SRC_DIR)/stream/chunk.cpp \
					$(SRC_DIR)/stream/file.cpp \
					$(SRC_DIR)/stream/lzma.cpp \
					$(SRC_DIR)/stream/slice.cpp \
					$(SRC_DIR)/util/console.cpp \
					$(SRC_DIR)/util/load.cpp \
					$(SRC_DIR)/util/log.cpp \
					$(SRC_DIR)/util/time.cpp \
					$(SRC_DIR)/release.cpp \
					native_interface.cpp
																				
include $(BUILD_SHARED_LIBRARY)

