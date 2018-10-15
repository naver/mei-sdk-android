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

/**
 * Created by GTPark on 2017-01-03.
 */

import android.graphics.Bitmap;

import com.naver.mei.sdk.core.gif.encoder.EncodingListener;
import com.naver.mei.sdk.core.gif.encoder.GifBatchEncoderAsyncTask;
import com.naver.mei.sdk.core.gif.encoder.GifQueuingEncodable;
import com.naver.mei.sdk.core.gif.encoder.GifQueuingEncoderAsyncTask;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import static com.naver.mei.sdk.error.MeiSDKErrorType.INVALID_COLOR_LEVEL_VALUE;

/**
 * MEI-SDK에서 사용되는 향상된 GIF Encoder
 */
public class MeiGifEncoder {
	private int quality = 10;  // (or sampling factor) default 10. (10-30)
	private int colorLevel = 7; // (or map quality) default 7 (8-6)
	private int delay = 100;

	private static final int MAX_QUEUE_SIZE = 100;
	private static final long MAX_QUEUE_CAPACITY = 64 * 1024 * 1024;

	public MeiGifEncoder() {
	}

	public static MeiGifEncoder newInstance() {
		return new MeiGifEncoder();
	}

	public MeiGifEncoder setQuality(int quality) {
		this.quality = quality;
		return this;
	}

	public MeiGifEncoder setColorLevel(int colorLevel) {
		if (colorLevel > 8 || colorLevel < 6)
			throw new MeiSDKException(INVALID_COLOR_LEVEL_VALUE);

		this.colorLevel = colorLevel;
		return this;
	}

	public MeiGifEncoder setDelay(int delay) {
		this.delay = delay;
		return this;
	}

	public void encodeByBitmaps(List<Bitmap> bitmaps, String outputFilePath, EncodingListener encodingListener) {
		try {
			encodeByBitmaps(bitmaps, new FileOutputStream(outputFilePath), encodingListener);
		} catch (Exception ex) {
			MeiLog.e(ex.getMessage(), ex);
			encodingListener.onError(new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_GIF));
		}
	}

	public void encodeByBitmaps(List<Bitmap> bitmaps, OutputStream outputStream, EncodingListener encodingListener) {
		new GifBatchEncoderAsyncTask(
				new GifBatchEncoderAsyncTask.BitmapIterator(bitmaps),
				quality,
				colorLevel,
				delay,
				outputStream,
				encodingListener).execute();
	}

	public void encodeByImagePaths(List<String> imagePaths, String outputFilePath, EncodingListener encodingListener) {
		try {
			encodeByImagePaths(imagePaths, new FileOutputStream(outputFilePath), encodingListener);
		} catch (Exception ex) {
			MeiLog.e(ex.getMessage(), ex);
			encodingListener.onError(new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_GIF));
		}
	}

	public void encodeByImagePaths(List<String> imagePaths, OutputStream outputStream, EncodingListener encodingListener) {
		new GifBatchEncoderAsyncTask(
				new GifBatchEncoderAsyncTask.ImagePathIterator(imagePaths),
				quality,
				colorLevel,
				delay,
				outputStream,
				encodingListener).execute();
	}

	public GifQueuingEncodable encodeWithQueuing(OutputStream outputStream, EncodingListener encodingListener) {
		MeiLog.d("create gif, quality: " + quality + ", colorLevel : " + colorLevel + ", delay: " + delay);
		GifQueuingEncoderAsyncTask task = new GifQueuingEncoderAsyncTask(MAX_QUEUE_SIZE, MAX_QUEUE_CAPACITY, quality, colorLevel, delay, outputStream, encodingListener);
		task.execute();
		return task;
	}
}
