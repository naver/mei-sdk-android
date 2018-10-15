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

import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.PlayDirection;

/**
 * Created by GTPark on 2016-10-17.
 */

public abstract class AnimatedElement extends CompositionElement {
	protected int duration;
	protected int frameCount;
	protected int[] frameTimestamps;
	protected PlayDirection playDirection;
	protected int originalFrameCount;

	public AnimatedElement(int width, int height, int left, int top, int zIndex, float degree) {
		super(width, height, left, top, zIndex, degree);
	}

	public AnimatedElement(Composable meta, int frameCount, PlayDirection playDirection, double resizeRatio) {
		super(meta, resizeRatio);
		this.originalFrameCount = frameCount;
		this.frameCount = (int)(frameCount * playDirection.durationMultiplier);
		this.playDirection = playDirection;
	}

	public abstract void next();    // pointer go to next
	public abstract int delay();
	public abstract Bitmap bitmap();
	public abstract Frame frame();

	// random access operation
	public abstract Frame getFrame(int index);

	public int getFrameCount() { return frameCount; }
	public int getDuration() { return duration; }

	public int[] getFrameTimestamps() {
		return frameTimestamps;
	}

	protected void calculateDurationAndFrameTimestamps() {
		frameTimestamps = new int[frameCount];
		duration = 0;

		for (int i=0; i<frameCount; ++i) {
			next();
			duration += delay();
			frameTimestamps[i] = duration;
		}
	}

	public int findFrameByTimestamp(final int timestamp) {
		int left = 0;
		int right = frameTimestamps.length - 1;
		int mid = 0;
		int midValue = 0;

		while (left <= right) {
			mid = (left + right) / 2;
			midValue = frameTimestamps[mid];
			if (midValue > timestamp) right = mid - 1;
			else if (midValue < timestamp) left = mid + 1;
			else {
				return mid;
			}
		}

		return timestamp < midValue ? mid : mid + 1;
	}

	public Frame getFrameByTimestamp(int timestamp) {
		return getFrame(findFrameByTimestamp(timestamp));
	}
}
