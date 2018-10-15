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
package com.naver.mei.sdk.core.image.compositor.strategy;

import android.graphics.Bitmap;

import com.naver.mei.sdk.core.image.compositor.element.AnimatedElement;
import com.naver.mei.sdk.core.image.compositor.element.BitmapElement;
import com.naver.mei.sdk.core.image.compositor.element.CompositionElement;

/**
 * Created by GTPark on 2016-10-20.
 */

public abstract class FramePickStrategy {
	public Bitmap pick(CompositionElement compositionElement, int timestamp, int duration) {
		if (compositionElement instanceof BitmapElement)
			return pickForBitmap((BitmapElement) compositionElement);
		if (compositionElement instanceof AnimatedElement)
			return pickForAnimated((AnimatedElement) compositionElement, timestamp, duration);
		return null;
	}

	public abstract Bitmap pickForAnimated(AnimatedElement compositionElement, int timestamp, int duration);

	private Bitmap pickForBitmap(BitmapElement bitmapElement) {
		return bitmapElement.bitmap;
	}
}