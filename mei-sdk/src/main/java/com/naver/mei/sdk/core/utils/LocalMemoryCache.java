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
package com.naver.mei.sdk.core.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;

/**
 * 로컬 스토리지를 바탕으로 파일 캐시를 지원하는 클래스
 * Created by GTPark on 2017-01-18.
 */

public class LocalMemoryCache extends LocalCache<byte[]> {
	private static final long DEFAULT_MAX_CACHE_CAPACITY = 10 * 1024 * 1024;
	private static final int DEFAULT_MAX_CACHE_COUNT = 200;
	private static LocalMemoryCache instance;

	private LocalMemoryCache() {
		setMaxCacheCount(DEFAULT_MAX_CACHE_COUNT);
		setMaxCacheCapacity(DEFAULT_MAX_CACHE_CAPACITY);
		setNextLevelCache(LocalFileCache.getInstance());
	}

	// support singleton
	public static LocalMemoryCache getCommonInstance() {
		// duplicated check for performance
		if (instance != null)
			return instance;

		synchronized (LocalMemoryCache.class) {
			if (instance == null)
				instance = new LocalMemoryCache();
		}
		return instance;
	}

	// support new memory cache instance. without file cache
	public static LocalMemoryCache getNewInstance() {
		LocalMemoryCache newInstance = new LocalMemoryCache();
		newInstance.setNextLevelCache(null);
		return newInstance;
	}



	@Override
	public void put(String key, Bitmap bitmap, CompressFormat compressFormat) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(compressFormat.bitmapCompressFormat, compressFormat.quality, baos);
		byte[] data = baos.toByteArray();
		IOUtils.closeQuietly(baos);
		put(key, data);
	}

	/**
	 * Bitmap을 파일 스토리지에 캐싱한다.
	 *
	 * @param data  bytes array
	 * @return cache key
	 */
	public void put(String key, byte[] data) {
		registerCache(key, data);
	}

	@Override
	public void put(String key, File file) {
		try {
			put(key, IOUtils.toByteArray(file.toURI()));
		} catch (Exception ex) {
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_CACHE_ORIGINAL_FILE);
		}
	}
}
