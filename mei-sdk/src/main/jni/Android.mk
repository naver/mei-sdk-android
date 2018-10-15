# first neuquant library build as static
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := neuquant-native
LOCAL_SRC_FILES := neuQuant/neuquant.c
include $(BUILD_STATIC_LIBRARY)

# second bridge library build as shared

include $(CLEAR_VARS)
LOCAL_MODULE := neuquant
LOCAL_SRC_FILES := neuQuant/NativeNeuQuant.c
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES :=neuquant-native
include $(BUILD_SHARED_LIBRARY)

# LZW Encoder
include $(CLEAR_VARS)
LOCAL_MODULE := lzwEncoder
LOCAL_SRC_FILES := lzw/LZWEncoder.c
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
