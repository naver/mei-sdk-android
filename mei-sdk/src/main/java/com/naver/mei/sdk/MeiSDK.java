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
package com.naver.mei.sdk;

import android.content.Context;
import android.os.Build;

import com.naver.mei.sdk.core.image.ImageCompositor;
import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.utils.LocalFileCache;
import com.naver.mei.sdk.core.utils.LocalMemoryCache;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.core.video.MeiVideoFrameExtractor;
import com.naver.mei.sdk.core.video.VideoToGifParams;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.listener.MeiFrameListener;

import java.util.List;

/**
 * Meme 편집에 관련된 모든 기능을 제공해주는 class
 * Created by tigerbaby on 2016-10-17.
 */

public class MeiSDK {
	private static long minimumStorageSpace = 128 * 1024 * 1024;
	private static Context context;

	public static void init(Context context) {
		MeiSDK.context = context;
		MeiFileUtils.init();
	}

	public static void setStorageDir(String storageDir) {
		MeiFileUtils.setStorageDir(storageDir);
	}

	/**
	 * 이미지 합성을 위한 ImageBuilder를 생성합니다
	 */
	public static ImageCompositor createImageCompositor() {
		return new ImageCompositor();
	}

	/**
	 * 이미지들을 GIF로 생성합니다.
	 *
	 * @param imagePaths    입력 이미지들의 경로 리스트
	 * @param listener      MeiEventListener
	 * @param savedFilePath 저장될 파일 경로
	 */
	public static void imagesToGif(List<String> imagePaths, MeiEventListener listener, String savedFilePath) {
		imagesToGif(imagePaths, 500, FrameAlignment.KEEP_ORIGINAL_RATIO, PlayDirection.FORWARD, listener, savedFilePath);
	}

	/**
	 * 이미지들을 GIF로 생성합니다
	 *
	 * @param imagePaths       입력 이미지들의 경로 리스트
	 * @param frameDelayMillis 프레임 간 딜레이 (밀리초)
	 * @param frameAlignment   개별 프레임 정렬 방식 (원본비율 유지, 화면맞춤)
	 * @param playDirection    이미지 재생방향 (forward, reverse, boomerang:forward + reverse)
	 * @param listener         MeiEventListener
	 * @param savedFilePath    저장될 파일 경로
	 */
	public static void imagesToGif(List<String> imagePaths, int frameDelayMillis, FrameAlignment frameAlignment, PlayDirection playDirection, MeiEventListener listener, String savedFilePath) {
		createImageCompositor().setBackgroundImages(imagePaths, frameDelayMillis, frameAlignment, playDirection)
				.setEventListener(listener)
				.setSavedFilePath(savedFilePath)
				.composite();
	}

	/**
	 * 비디오에서 특정 구간을 GIF로 생성합니다
	 *
	 * @param videoToGifParams 비디오 경로, 구간, fps등이 담긴 메타 정보
	 * @param listener         MeiEventListener
	 * @param resultFilePath   저장될 파일 경로
	 */

	public static void videoToGif(VideoToGifParams videoToGifParams, MeiEventListener listener, String resultFilePath) {
		VideoToGifParams params = videoToGifParams.clone();

		if (params.fps > 10) {
			throw new MeiSDKException(MeiSDKErrorType.VIDEO_TO_GIF_FPS_CANNOT_EXCEED_10);
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			throw new MeiSDKException(MeiSDKErrorType.NOT_AVAILABLE_OS_VERSION_FOR_DECODING);
		}

		new MeiVideoFrameExtractor(params, listener)
				.setResultFilePath(resultFilePath)
				.execute();
	}

	/**
	 * 비디오에서 특정 구간을 이미지로 뽑아냅니다
	 */
	public static void getFramesFromVideo(VideoToGifParams videoToGifParams, MeiFrameListener listener) {
		if (videoToGifParams.fps > 10) {
			throw new MeiSDKException(MeiSDKErrorType.VIDEO_TO_GIF_FPS_CANNOT_EXCEED_10);
		}

		new MeiVideoFrameExtractor(videoToGifParams, listener).setFrameExtractorMode(true).execute();

	}

	/**
	 * MEI-SDK에서 사용되는 로컬 메모리, 파일 캐시의 최대 용량을 설정합니다.
	 * 해당 값을 설정하지 않으면 각각 5MB, 20MB의 기본 용량으로 동작합니다.
	 *
	 * @param maxMemoryCacheCapacity 최대 메모리 캐시 용량
	 * @param maxFileCacheCapacity   최대 파일 캐시 용량
	 */
	public static void setCacheCapacity(int maxMemoryCacheCapacity, int maxFileCacheCapacity) {
		LocalMemoryCache.getCommonInstance().setMaxCacheCapacity(maxMemoryCacheCapacity);
		LocalFileCache.getInstance().setMaxCacheCapacity(maxFileCacheCapacity);
	}

	/**
	 * 메모리 캐시 및 파일 캐시를 제거합니다.
	 */
	public static void clearCache() {
		LocalMemoryCache.getCommonInstance().evictAll();
		LocalFileCache.getInstance().evictAll();
	}

	public static Context getContext() {
		if (needInit()) {
			throw new MeiSDKException(MeiSDKErrorType.NEED_INITIALIZE);
		} else {
			return context;
		}
	}

	public static void setMinimumStorageSpace(long minimumStorageSpace) {
		MeiSDK.minimumStorageSpace = minimumStorageSpace;
	}

	public static long getMinimumStorageSpace() {
		return minimumStorageSpace;
	}

	private static boolean needInit() {
		return context == null;
	}
}
