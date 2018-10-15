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

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.compositor.element.Frame;
import com.naver.mei.sdk.core.image.meta.FrameMeta;
import com.naver.mei.sdk.core.image.util.IOHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-10-27.
 */

public class AnimatedMultiFrame extends Animated {
	private final MultiFrame multiFrame;
	private final List<FrameMeta> frameMetas;
	private int lastAccessFrameIndex;
	private Frame lastAccessFrame;

	public AnimatedMultiFrame(MultiFrame multiFrame, double resizeRatio) {
		super((int)(multiFrame.width * resizeRatio), (int)(multiFrame.height * resizeRatio));
		this.multiFrame = multiFrame;
		this.frameMetas = resizeFrameMetas(multiFrame.frameMetas, resizeRatio);
		this.frameCount = this.frameMetas.size();
		this.lastAccessFrameIndex = -1;
		calculateDurationAndFrameTimestamps();
	}

	private List<FrameMeta> resizeFrameMetas(List<FrameMeta> frameMetas, double resizeRatio) {
		List<FrameMeta> resizedFrameMetas = new ArrayList<>();
		for (FrameMeta meta : frameMetas) {
			resizedFrameMetas.add(meta.resize(resizeRatio));
		}
		return resizedFrameMetas;
	}

	@Override
	public void next() {
		this.lastAccessFrameIndex = (this.lastAccessFrameIndex + 1) % frameCount;
		this.lastAccessFrame = null;
	}

	@Override
	public int delay() {
		return frameMetas.get(lastAccessFrameIndex).msDelay;
	}

	@Override
	public Bitmap bitmap() {
		return frame().bitmap;
	}

	@Override
	public Frame frame() {
		if (lastAccessFrame == null) {
			lastAccessFrame = getFrame(lastAccessFrameIndex);
		}

		return lastAccessFrame;
	}

	@Override
	public Frame getFrame(int index) {
		if (index < 0 || index > frameCount) {
			throw new MeiSDKException(MeiSDKErrorType.FRAME_INDEX_OUT_OF_BOUND);
		}

		if (index == lastAccessFrameIndex && lastAccessFrame != null) return lastAccessFrame;

		FrameMeta frameMeta = frameMetas.get(index);
		Bitmap frameBitmap = MeiImageProcessor.decodeAndResize(IOHelper.getImageBytes(frameMeta.uri), frameMeta.width, frameMeta.height);

		// align center
		this.lastAccessFrame = new Frame(frameBitmap, (multiFrame.width - frameBitmap.getWidth()) / 2 , (multiFrame.height - frameBitmap.getHeight()) / 2, frameMeta.msDelay);
		this.lastAccessFrameIndex = index;
		return this.lastAccessFrame;
	}
}
