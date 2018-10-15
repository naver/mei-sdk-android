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

import com.naver.mei.sdk.core.image.animated.Animated;
import com.naver.mei.sdk.core.image.animated.AnimatedGif;
import com.naver.mei.sdk.core.image.animated.AnimatedMultiFrame;
import com.naver.mei.sdk.core.image.animated.MultiFrame;
import com.naver.mei.sdk.core.image.meta.Composable;

/**
 * Created by GTPark on 2016-10-17.
 * Adaptor class (GifDecoder to AnimatedElement)
 */

public class AnimatedElement2 extends CompositionElement {
	public Animated animated;

	public AnimatedElement2(byte[] bytesGif, Composable meta, double resizeRatio) {
		super(meta, resizeRatio);
		animated = new AnimatedGif(bytesGif, (int)(meta.width * resizeRatio), (int)(meta.height * resizeRatio));
	}

	public AnimatedElement2(MultiFrame multiFrame, Composable meta, double resizeRatio) {
		super(meta, resizeRatio);
		animated = new AnimatedMultiFrame(multiFrame, resizeRatio);
	}
}
