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
import android.util.Log;

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.meta.FrameMeta;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by GTPark on 2016-10-17.
 */

public class AnimatedMultiFrameElement extends AnimatedElement {
	private final List<FrameMeta> frameMetas;
	private int lastAccessFrameIndex;
	private Frame lastAccessFrame;

	public AnimatedMultiFrameElement(ComposableMultiFrame meta, double resizeRatio) {
		super(meta, meta.frameMetas.size(), meta.playDirection, resizeRatio);
		this.frameMetas = resizeFrameMetas(meta.frameMetas, resizeRatio);
		this.lastAccessFrameIndex = -1;
		applyPlayDirection();
		calculateDurationAndFrameTimestamps();
	}

	private void applyPlayDirection() {
		switch (playDirection) {
			case REVERSE:
				Collections.reverse(frameMetas);
				break;
			case BOOMERANG:
				List<FrameMeta> reverseFrameMetas = new ArrayList<>();
				Collections.copy(reverseFrameMetas, frameMetas);
				Collections.reverse(reverseFrameMetas);
				reverseFrameMetas.remove(0);
				frameMetas.addAll(reverseFrameMetas);
		}
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
		if (index < 0 || index > frameCount)
			throw new MeiSDKException(MeiSDKErrorType.FRAME_INDEX_OUT_OF_BOUND);

		if (index == lastAccessFrameIndex && lastAccessFrame != null)
			return lastAccessFrame;

		FrameMeta frameMeta = frameMetas.get(index);
		Bitmap frameBitmap = MeiImageProcessor.rotate(
				MeiImageProcessor.decodeAndResize(
						frameMeta.uri,
						frameMeta.orientationDegree == 0 || frameMeta.orientationDegree == 180 ? frameMeta.width : frameMeta.height,
						frameMeta.orientationDegree == 0 || frameMeta.orientationDegree == 180 ? frameMeta.height : frameMeta.width),
				frameMeta.orientationDegree);

		// align center
		this.lastAccessFrame = new Frame(frameBitmap, frameMeta.left, frameMeta.top, frameMeta.msDelay);
		this.lastAccessFrameIndex = index;
		return this.lastAccessFrame;
	}

	public FrameMeta getFrameMeta(int index) {
		return frameMetas.get(index);
	}
}
