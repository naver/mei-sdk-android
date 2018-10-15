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

package com.naver.mei.sdk.core.gif.encoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.OutputStream;
import java.util.List;

/**
 * Created by GTPark on 2017-01-03.
 */

public class GifBatchEncoderAsyncTask extends GifEncoderAsyncTask {
	private FrameIterator frameIterator;

	public GifBatchEncoderAsyncTask(FrameIterator frameIterator, int learnQuality, int mapQuality, int delay, OutputStream out, EncodingListener encodingListener) {
		super(learnQuality, mapQuality, delay, out, encodingListener);
		this.frameIterator = frameIterator;
	}

	protected boolean encodeFrames(AnimatedGifEncoder encoder) {
		int frameCount = frameIterator.getCount();

		publishProgress(0.0);

		for (int i = 0; i < frameCount; ++i) {
			encoder.addFrame(frameIterator.getNextBitmap());
			publishProgress((i + 1) / (double) frameCount);
		}

		return frameCount > 0;
	}

	interface FrameIterator {
		Bitmap getNextBitmap();

		int getCount();
	}

	public static class BitmapIterator implements FrameIterator {
		private List<Bitmap> bitmaps;
		private int next = 0;

		public BitmapIterator(List<Bitmap> bitmaps) {
			this.bitmaps = bitmaps;
		}

		@Override
		public Bitmap getNextBitmap() {
			if (next >= bitmaps.size())
				return null;

			return bitmaps.get(next++);
		}

		@Override
		public int getCount() {
			return bitmaps.size();
		}
	}

	public static class ImagePathIterator implements FrameIterator {
		private List<String> imagePaths;
		private int next = 0;

		public ImagePathIterator(List<String> imagePaths) {
			this.imagePaths = imagePaths;
		}

		@Override
		public Bitmap getNextBitmap() {
			if (next >= getCount())
				return null;

			return BitmapFactory.decodeFile(imagePaths.get(next++));
		}

		@Override
		public int getCount() {
			return imagePaths.size();
		}
	}
}
