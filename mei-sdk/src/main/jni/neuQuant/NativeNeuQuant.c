/*
Copyright 2018 NAVER Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

#include "NativeNeuQuant.h"
#include "neuquant.h"
#include <android/log.h>
#include <stdlib.h>

#define MAX_COLOR_SIZE 256
#define MAX_CACHE_SIZE 2097152

static unsigned char mapCache[MAX_CACHE_SIZE];

JNIEXPORT void JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeNeuQuant_init(JNIEnv *env, jobject object, jbyteArray array, jint len, jint sample) {
//    __android_log_print(ANDROID_LOG_ERROR, "JNI", "Native Method Called : init");
    jbyte* b = (*env)->GetByteArrayElements(env, array, 0);
    initnet((unsigned char*)b, len, sample);
    (*env)->ReleaseByteArrayElements(env, array, b, 0);
}


JNIEXPORT jintArray JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeNeuQuant_process(JNIEnv *env, jobject object) {
//    __android_log_print(ANDROID_LOG_ERROR, "JNI", "Native Method Called : process");
    unsigned char color[768];

    learn();
    unbiasnet();
    inxbuild();

    writecolormap(color);

    jbyteArray palette = (*env)->NewByteArray(env, 768);
    (*env)->SetByteArrayRegion(env, palette, 0, 768, (jbyte*)color);

    return palette;
};

JNIEXPORT jint JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeNeuQuant_map__III(JNIEnv *env, jobject object, jint b, jint g, jint r) {
    return inxsearch(b, g, r);
}

JNIEXPORT jobject JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeNeuQuant_map___3B(JNIEnv *env, jobject object, jbyteArray jpixels) {
    jbyte* pixels = (*env)->GetByteArrayElements(env, jpixels, NULL);
    jboolean usedEntry[MAX_COLOR_SIZE];

    register int nPix = (*env)->GetArrayLength(env, jpixels) / 3;
    register jbyte* indexedPixels = malloc(sizeof(jboolean) * nPix);
    register jboolean* pUsedEntry = usedEntry;

    register int i = 0, k = 0;
    register int index;

    for (; i < nPix; ++i) {
        index = inxsearch(pixels[k] & 0xff, pixels[k + 1] & 0xff, pixels[k + 2] & 0xff);
        pUsedEntry[index] = JNI_TRUE;
        indexedPixels[i] = (jbyte)index;
        k += 3;
    }

    jbyteArray jIndexedPixels = (*env)->NewByteArray(env, nPix);
    jbooleanArray  jusedEntry = (*env)->NewBooleanArray(env, MAX_COLOR_SIZE);

    (*env)->SetByteArrayRegion(env, jIndexedPixels, 0, nPix, indexedPixels);
    (*env)->SetBooleanArrayRegion(env, jusedEntry, 0, MAX_COLOR_SIZE, usedEntry);

    (*env)->ReleaseByteArrayElements(env, jpixels, pixels, 0);
    free(indexedPixels);

    // create java object
    jclass mapResultClass = (*env)->FindClass(env, "com/naver/mei/sdk/core/gif/encoder/MapResult");
    jmethodID mapResultConstructorId = (*env)->GetMethodID(env, mapResultClass, "<init>", "()V");
    jobject mapResult = (*env)->NewObject(env, mapResultClass, mapResultConstructorId, "()V");
    jfieldID indexedPixelsFieldId = (*env)->GetFieldID(env, mapResultClass, "indexedPixels", "[B");
    jfieldID usedEntryFieldId = (*env)->GetFieldID(env, mapResultClass, "usedEntry", "[Z");

    (*env)->SetObjectField(env, mapResult, indexedPixelsFieldId, jIndexedPixels);
    (*env)->SetObjectField(env, mapResult, usedEntryFieldId, jusedEntry);

    return mapResult;
}

JNIEXPORT jobject JNICALL Java_com_naver_mei_sdk_core_gif_encoder_NativeNeuQuant_mapByQuality(JNIEnv *env, jobject object, jbyteArray jpixels, jint mapQuality) {
    jbyte* pixels = (*env)->GetByteArrayElements(env, jpixels, NULL);
    jboolean usedEntry[MAX_COLOR_SIZE];

    register int nPix = (*env)->GetArrayLength(env, jpixels) / 3;
    register jbyte* indexedPixels = malloc(sizeof(jboolean) * nPix);
    register jboolean* pUsedEntry = usedEntry;

    memset(mapCache, 0, MAX_CACHE_SIZE);

    register unsigned char* pMapCache = mapCache;
    register int i = 0, k = 0;
    register int index;
    register int key;
    register int rShifter = 16 - (8 - mapQuality) * 3;
    register int gShifter = 8 - (8 - mapQuality) * 2;
    register int bShifter = 8 - mapQuality;
    register unsigned char mask = (unsigned char)(255 - ((1 << (8 - mapQuality)) - 1));

    for (; i < nPix; ++i) {
        key = ((pixels[k+2] & mask) << rShifter ) | ((pixels[k+1] & mask) << gShifter) | ((pixels[k] & mask) >> bShifter);
        if (pMapCache[key] == 0) {
            index = inxsearch(pixels[k] & 0xff, pixels[k + 1] & 0xff, pixels[k + 2] & 0xff);
            pMapCache[key] = (unsigned char)(index + 1);
            pUsedEntry[index] = JNI_TRUE;
        }

        indexedPixels[i] = (jbyte)(pMapCache[key] - 1);
        k += 3;
    }

    jbyteArray jIndexedPixels = (*env)->NewByteArray(env, nPix);
    jbooleanArray  jusedEntry = (*env)->NewBooleanArray(env, MAX_COLOR_SIZE);

    (*env)->SetByteArrayRegion(env, jIndexedPixels, 0, nPix, indexedPixels);
    (*env)->SetBooleanArrayRegion(env, jusedEntry, 0, MAX_COLOR_SIZE, usedEntry);

    (*env)->ReleaseByteArrayElements(env, jpixels, pixels, 0);
    free(indexedPixels);

    // create java object
    jclass mapResultClass = (*env)->FindClass(env, "com/naver/mei/sdk/core/gif/encoder/MapResult");
    jmethodID mapResultConstructorId = (*env)->GetMethodID(env, mapResultClass, "<init>", "()V");
    jobject mapResult = (*env)->NewObject(env, mapResultClass, mapResultConstructorId, "()V");
    jfieldID indexedPixelsFieldId = (*env)->GetFieldID(env, mapResultClass, "indexedPixels", "[B");
    jfieldID usedEntryFieldId = (*env)->GetFieldID(env, mapResultClass, "usedEntry", "[Z");

    (*env)->SetObjectField(env, mapResult, indexedPixelsFieldId, jIndexedPixels);
    (*env)->SetObjectField(env, mapResult, usedEntryFieldId, jusedEntry);

    return mapResult;
}