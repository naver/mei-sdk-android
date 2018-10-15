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
package com.naver.mei.sdk.core.image.compositor.element;

import android.graphics.Bitmap;

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.gif.decoder.GifDecoder;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.meta.ComposableImage;

/**
 * Created by GTPark on 2016-10-17.
 * Adaptor class (GifDecoder to AnimatedElement)
 */

public class AnimatedGifElement extends AnimatedElement {
	private GifDecoder decoder;
	private int lastAccessFrameIndex;
	private Frame lastAccessFrame;

	public AnimatedGifElement(GifDecoder decoder, ComposableImage meta, double resizeRatio) {
		super(meta, decoder.getFrameCount(), meta.playDirection, resizeRatio);
		this.decoder = decoder;
		this.lastAccessFrameIndex = -1;
		calculateDurationAndFrameTimestamps();
	}


	@Override
	public void next() {
		// TODO gif decoder가 정방향 advance밖에 정상적으로 지원하지 않는 것인지 확인 후 최적화가 필요하다.
		decoder.advance();
		lastAccessFrameIndex = (lastAccessFrameIndex + 1) % getFrameCount();
		lastAccessFrame = null;
	}

	private int getIncrease() {
		switch (playDirection) {
			case REVERSE:
				return -1;
			case BOOMERANG:
				return lastAccessFrameIndex >= originalFrameCount - 1 ? -1 : 1;
		}

		return 1;
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
			lastAccessFrame = new Frame(MeiImageProcessor.resize(decoder.getNextFrame(), width, height), decoder.getNextDelay());
		}

		return lastAccessFrame;
	}

	@Override
	public Frame getFrame(int index) {
		if (index < 0 || index > getFrameCount()) {
			throw new MeiSDKException(MeiSDKErrorType.FRAME_INDEX_OUT_OF_BOUND);
		}

		index = adjustFrameIndex(index);

		// cache hit
		if (lastAccessFrameIndex == index) return frame();

		// move to frame pointer
		while (lastAccessFrameIndex != index) {
			next();
		}

		return frame();
	}

	private int adjustFrameIndex(int index) {
		switch (playDirection) {
			case FORWARD:
				return index;
			case REVERSE:
				return (frameCount-1) - index;
			case BOOMERANG:
				return index < originalFrameCount ? index : frameCount - index;
		}
		return index;
	}
}
