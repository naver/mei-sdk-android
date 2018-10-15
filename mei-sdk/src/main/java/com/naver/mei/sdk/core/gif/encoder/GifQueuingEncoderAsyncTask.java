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

import com.naver.mei.sdk.error.MeiLog;

import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by GTPark on 2017-01-03.
 */

public class GifQueuingEncoderAsyncTask extends GifEncoderAsyncTask implements GifQueuingEncodable {
	private static final double STOP_SIGNAL = Double.MIN_VALUE;
	private static final File BREAK_FILE = new File("/");

	protected BlockingQueue<File> encodingQueue;
	protected long totalCapacity = 0;
	protected boolean isRunning = true;
	protected int maxQueueSize;
	protected long maxCapacity;
	protected int totalFrameCount = 0;
	protected int currentFrameNumber = 0;

	public GifQueuingEncoderAsyncTask(int maxQueueSize, long maxCapacity, int learnQuality, int mapQuality, int delay, OutputStream out, EncodingListener encodingListener) {
		super(learnQuality, mapQuality, delay, out, encodingListener);
		this.encodingQueue = new LinkedBlockingQueue<>();
		this.maxQueueSize = maxQueueSize;
		this.maxCapacity = maxCapacity;
	}

	protected boolean encodeFrames(AnimatedGifEncoder encoder) {
		try {
			while (isRunning || encodingQueue.size() > 0) {
				File file = encodingQueue.take();
				if (file == BREAK_FILE) break; // break marker

				Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
				encoder.addFrame(bitmap);
				file.deleteOnExit();

				synchronized (this) {
					totalCapacity -= file.length();
				}

				++currentFrameNumber;

				publishProgress((double) currentFrameNumber, (double) totalFrameCount);
			}
		} catch (InterruptedException iex) {
			throw new RuntimeException(iex);
		}

		return currentFrameNumber > 0;
	}

	public void addFrame(File frameFile) {
		if (encodingQueue.size() >= maxQueueSize || totalCapacity >= maxCapacity) {
			stop();
			return;
		}

		synchronized (this) {
			totalCapacity += frameFile.length();
		}

		++totalFrameCount;

		try {
			encodingQueue.put(frameFile);
		} catch (Exception ex) {
			MeiLog.e("addFrame error", ex);
			stop();
		}

		MeiLog.d("addFrame : " + encodingQueue.size());
	}

	public void stop() {
		this.isRunning = false;
		encodingQueue.add(BREAK_FILE);
		publishProgress(STOP_SIGNAL);
	}

	@Override
	protected void onProgressUpdate(Double... values) {
		if (values[0] == STOP_SIGNAL) {
			encodingListener.onStop(totalFrameCount);
			return;
		}

		encodingListener.onFrameProgress(values[0].intValue(), values[1].intValue());
	}
}
