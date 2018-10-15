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

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

/**
 * 로컬 스토리지를 바탕으로 파일 캐시를 지원하는 클래스
 * Created by GTPark on 2017-01-18.
 */

public class LocalFileCache extends LocalCache<File> {
	private static final String CACHE_EXTENSION = ".mch";   // 캐시 파일 확장자
	private static final String FILE_NAME_SPLITTER = "__mch__";  // 캐시 파일명 분리자
	private static final long DEFAULT_MAX_CACHE_CAPACITY = 20 * 1024 * 1024;
	private static final int DEFAULT_MAX_CACHE_COUNT = 180;

	private volatile File cacheDir;

	private static LocalFileCache instance;

	// support singleton
	public static LocalFileCache getInstance() {
		// duplicated check for performance
		if (instance != null)
			return instance;

		synchronized (LocalFileCache.class) {
			if (instance == null)
				instance = new LocalFileCache();
		}
		return instance;
	}

	private LocalFileCache() {
		loadCacheDirectory();
		loadCacheFiles();
		setMaxCacheCapacity(DEFAULT_MAX_CACHE_CAPACITY);
		setMaxCacheCount(DEFAULT_MAX_CACHE_COUNT);
	}

	private void loadCacheDirectory() {
		cacheDir = MeiSDK.getContext().getExternalCacheDir();
		if (cacheDir == null)
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_CACHE_DIRECTORY);
	}

	// 기 캐싱 데이터 로드
	private void loadCacheFiles() {
		File[] cacheFiles = this.cacheDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(CACHE_EXTENSION);
			}
		});

		for (File cacheFile : cacheFiles) {
			String fileName = cacheFile.getName();
			int keyLength = cacheFile.getName().indexOf(FILE_NAME_SPLITTER);

			// 파일명 : [KEY]_[TIMESTAMP].mch
			// 명시적 키가 존재하면 해당 키를 사용하되 키가 없다면 파일이름을 키로 사용한다.
			String key = keyLength > 0 ? fileName.substring(0, keyLength)
					: (keyLength == 0 ? fileName.substring(FILE_NAME_SPLITTER.length() - 1) : fileName);
			registerCache(key, cacheFile);
		}
	}

	/**
	 * Bitmap을 파일 스토리지에 캐싱한다.
	 * @param key Bitmap을 가리키는 Key
	 * @param bitmap 캐시 데이터
	 * @return 캐시 파일
	 */
	public void put(String key, Bitmap bitmap, CompressFormat compressFormat) {
		File cacheFile = createNewCacheFile(key);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(cacheFile);
			bitmap.compress(compressFormat.bitmapCompressFormat, compressFormat.quality, out);
			registerCache(key, cacheFile);
		} catch (IOException ex) {
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_CACHE_FILE);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * Byte Array 데이터를 파일 스토리지에 캐싱한다.
	 * @param key 데이터를 가리키는 Key
	 * @param data 캐시 데이터
	 */
	public void put(String key, byte[] data) {
		File cacheFile = createNewCacheFile(key);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(cacheFile);
			out.write(data);
			registerCache(key, cacheFile);
		} catch (IOException ex) {
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_CACHE_FILE);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * 파일을 캐싱한다. 원본 파일은 캐싱 영역으로 복사된다. 해당 파일의 key 값으로 파일 절대 경로의 해시코드 값이 사용된다.
	 * @param file 원본 파일
	 */
	@Override
	public void put(String key, File file) {
		File cacheFile = copyToCacheDir(file);
		registerCache(key, cacheFile);
	}

	private File copyToCacheDir(File file) {
		File copyFile = new File(cacheDir, file.getName() + CACHE_EXTENSION);

		try {
			FileUtils.copyFile(file, copyFile);
		} catch (IOException ex) {
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_CACHE_FILE);
		}

		return copyFile;
	}

	private File createNewCacheFile(String key) {
		Calendar calendar = Calendar.getInstance();
		return new File(cacheDir,
				key + FILE_NAME_SPLITTER
						+ calendar.get(Calendar.YEAR)
						+ calendar.get(Calendar.MONTH)
						+ calendar.get(Calendar.DATE)
						+ calendar.get(Calendar.HOUR_OF_DAY)
						+ calendar.get(Calendar.MINUTE)
						+ calendar.get(Calendar.SECOND)
						+ calendar.get(Calendar.MILLISECOND)
						+ CACHE_EXTENSION);
	}
}
