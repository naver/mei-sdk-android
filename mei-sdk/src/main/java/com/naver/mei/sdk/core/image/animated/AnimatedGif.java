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
package com.naver.mei.sdk.core.image.animated;

import android.graphics.Bitmap;
import android.util.Log;

import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.gif.decoder.GifDecoder;
import com.naver.mei.sdk.core.image.compositor.element.Frame;

/**
 * Created by GTPark on 2016-10-27.
 */

public class AnimatedGif extends Animated {
	private GifDecoder decoder;
	private int lastAccessFrameIndex;
	private Frame lastAccessFrame;

	public AnimatedGif(byte[] bytesGif) {
		this(bytesGif, -1, -1);
	}

	/**
	 *
	 * @param bytesGif Gif ByteArray
	 * @param resizeWidth getFrame시 반횐될 Bitmap size의 width
	 * @param resizeHeight getFrame시 반횐될 Bitmap size의 height
	 */
	public AnimatedGif(byte[] bytesGif, int resizeWidth, int resizeHeight) {
		super(resizeWidth, resizeHeight);

		int sampleSize = 1;
		if (resizeWidth >= 1 && resizeHeight >= 1) {
			sampleSize = MeiImageProcessor.getResizeOptions(MeiImageProcessor.getImageBoundsOptions(bytesGif), resizeWidth, resizeHeight).inSampleSize;
		}

		GifDecoder decoder = new GifDecoder();
		decoder.read(bytesGif, sampleSize);
		this.decoder = decoder;
		this.frameCount = decoder.getFrameCount();
		this.lastAccessFrameIndex = -1;
		calculateDurationAndFrameTimestamps();
	}

	/**
	 * getFrame시 반환될 bitmap size를 정확히 모를때 활용. width, height가 비율을 유지하되, maxWidth, maxHeight보다는 작은 이미지를 반환한다.
	 */
	public static AnimatedGif createInstanceWithMaxSize(byte[] bytesGif, int maxWidth, int maxHeight) {
		AnimatedGif instance = new AnimatedGif(bytesGif, maxWidth, maxHeight);
		instance.resizeWidth = -1;
		instance.resizeHeight = -1;
		return instance;
	}

	@Override
	public void next() {
		decoder.advance();
		lastAccessFrameIndex = (lastAccessFrameIndex + 1) % getFrameCount();
		lastAccessFrame = null;
	}

	@Override
	public int delay() {
		return decoder.getNextDelay();
	}

	@Override
	public Bitmap bitmap() {
		return frame().bitmap;
	}

	@Override
	public Frame frame() {
		if (lastAccessFrame == null) {
			lastAccessFrame = new Frame(resize(decoder.getNextFrame()), decoder.getNextDelay());
		}

		return lastAccessFrame;
	}

	@Override
	public Frame getFrame(int index) {
		if (index < 0 || index > getFrameCount()) {
			throw new MeiSDKException(MeiSDKErrorType.FRAME_INDEX_OUT_OF_BOUND);
		}

		// cache hit
		if (lastAccessFrameIndex == index) return frame();

		// move to frame pointer
		while (lastAccessFrameIndex != index) {
			next();
		}

		return frame();
	}

	@Override
	public int getFrameCount() {
		return decoder.getFrameCount();
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public int[] getFrameTimestamps() {
		return frameTimestamps;
	}
}
