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
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * MultiLevel LocalCache
 * cache level(memory, file)이 존재하며, 최적화된 캐싱, 스왑 아웃 지원
 * Created by GTPark on 2017-01-26.
 */

public abstract class LocalCache<T> {
	private LocalCache nextLevelCache;
	private long currentCacheCapacity;
	private CacheHelper cacheHelper;
	private CacheMap cacheMap;
	private long maxCacheCapacity;
	private int maxCacheCount;

	protected LocalCache<T> getNextLevelCache() {
		return nextLevelCache;
	}

	protected void setNextLevelCache(LocalCache nextLevelCache) {
		this.nextLevelCache = nextLevelCache;
	}

	public LocalCache() {
		this(null);
	}

	public LocalCache(LocalCache nextLevelCache) {
		this.nextLevelCache = nextLevelCache;
		this.cacheMap = new CacheMap();
		this.cacheHelper = new CacheHelper();

	}

	public void put(String key, Bitmap bitmap) {
		this.put(key, bitmap, CompressFormat.DEFAULT_JPEG_COMPRESS_FORMAT);
	}

	public abstract void put(String key, Bitmap bitmap, CompressFormat compressFormat);

	public abstract void put(String key, byte[] data);

	public abstract void put(String key, File file);

	public void setMaxCacheCapacity(long maxCacheCapacity) {
		this.maxCacheCapacity = maxCacheCapacity;
	}

	public void setMaxCacheCount(int maxCacheCount) {
		this.maxCacheCount = maxCacheCount;
	}

	/**
	 * 키 값에 대응되는 캐시 데이터를 반환한다.
	 *
	 * @param key 캐시 파일의 키 값
	 * @return 캐시 데이터
	 */
	public byte[] get(String key) {
		byte[] cache = cacheHelper.getCacheByteArray(cacheMap.get(key));

		if (cache != null || nextLevelCache == null) return cache;

		return nextLevelCache.get(key);
	}

	public Bitmap getBitmap(String key) {
		byte[] cache = get(key);

		if (cache == null) return null;

		return BitmapFactory.decodeByteArray(cache, 0, cache.length);
	}

	/**
	 * 키 값에 해당하는 캐시 파일을 삭제한다.
	 *
	 * @param key 캐시의 키 값
	 * @return 성공 여부
	 */
	public synchronized boolean evict(String key) {
		T cache = cacheMap.get(key);
		if (cache == null) return false;

		cacheMap.remove(key);
		removeCache(cache);
		return true;
	}


	private void removeCache(T cache) {
		cacheHelper.remove(cache);
		currentCacheCapacity -= cacheHelper.getSize(cache);
	}

	/**
	 * 전체 캐시 파일을 삭제한다.
	 */
	public void evictAll() {
		for (String s : cacheMap.keySet()) {
			evict(s);
		}
	}

	/**
	 * 해당 키 값의 캐싱 여부 반환
	 *
	 * @param key 키 값
	 * @return 캐싱 여부
	 */
	public boolean isCached(String key) {
		return cacheMap.get(key) != null;
	}

	public static class CompressFormat {
		private static final int DEFAULT_COMPRESS_QUALITY = 80;
		public static final CompressFormat DEFAULT_JPEG_COMPRESS_FORMAT = new CompressFormat(Bitmap.CompressFormat.JPEG, DEFAULT_COMPRESS_QUALITY);
		public static final CompressFormat DEFAULT_WEBP_COMPRESS_FORMAT = new CompressFormat(Bitmap.CompressFormat.WEBP, DEFAULT_COMPRESS_QUALITY);
		public static final CompressFormat DEFAULT_PNG_COMPRESS_FORMAT = new CompressFormat(Bitmap.CompressFormat.PNG, DEFAULT_COMPRESS_QUALITY);

		public final Bitmap.CompressFormat bitmapCompressFormat;
		public final int quality;

		public CompressFormat(Bitmap.CompressFormat bitmapCompressFormat, int quality) {
			this.bitmapCompressFormat = bitmapCompressFormat;
			this.quality = quality;
		}

		public static CompressFormat create(Bitmap.CompressFormat compressFormat, int quality) {
			return new CompressFormat(compressFormat, quality);
		}

		public static CompressFormat getDefaultCompressFormat(boolean hasTransparent) {
			return !hasTransparent ? DEFAULT_JPEG_COMPRESS_FORMAT
					: Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ?
							DEFAULT_PNG_COMPRESS_FORMAT : DEFAULT_WEBP_COMPRESS_FORMAT;
		}
	}


	protected void registerCache(String key, T cache) {
		this.currentCacheCapacity += cacheHelper.getSize(cache);
		this.cacheMap.put(key, cache);
	}

	private class CacheMap extends LinkedHashMap<String, T> {
		@Override
		protected boolean removeEldestEntry(Entry<String, T> eldestEntry) {
			synchronized (this) {
				if (currentCacheCapacity <= maxCacheCapacity && this.size() <= maxCacheCount)
					return false;   // not full, no evict

				// swap out to next level cache
				String key = eldestEntry.getKey();
				T cache = eldestEntry.getValue();
				if (nextLevelCache != null) {
					cacheHelper.putToNextLevelCache(key, cache);
				}

				LocalCache.this.removeCache(cache);
			}
			return true;
		}
	}

	private class CacheHelper {
		private long getSize(T cache) {
			if (cache instanceof byte[]) {
				return ((byte[]) cache).length;
			} else if (cache instanceof File) {
				return ((File) cache).length();
			} else {
				throw new MeiSDKException(MeiSDKErrorType.UNKNOWN_CACHE_TYPE);
			}
		}

		private byte[] getCacheByteArray(T cache) {
			if (cache instanceof byte[]) {
				return (byte[]) cache;
			} else if (cache instanceof File) {
				try {
					return IOUtils.toByteArray(((File) cache).toURI());
				} catch (Exception ex) {
					Log.e("MEI", "failed to read cache");
					return null;
				}
			} else if (cache == null) {
				return null;
			} else {
				MeiLog.e("cache type : " + cache.getClass().getSimpleName());
				throw new MeiSDKException(MeiSDKErrorType.UNKNOWN_CACHE_TYPE);
			}
		}

		private void remove(T cache) {
			if (cache instanceof File) {
				((File) cache).delete();
			}
			// else case do nothing
		}

		private void putToNextLevelCache(String key, T cache) {
			if (cache instanceof byte[]) {
				nextLevelCache.put(key, (byte[]) cache);
			} else if (cache instanceof File) {
				nextLevelCache.put(key, (File) cache);
			} else {
				throw new MeiSDKException(MeiSDKErrorType.UNKNOWN_CACHE_TYPE);
			}
		}
	}
}
