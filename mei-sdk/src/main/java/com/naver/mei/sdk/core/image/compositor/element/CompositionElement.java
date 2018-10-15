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

import com.naver.mei.sdk.core.image.meta.Composable;

/**
 * Created by GTPark on 2016-10-11.
 */

public abstract class CompositionElement {
	public final int width;
	public final int height;
	public final int left;
	public final int top;
	public final int zIndex;
	public final float degree;

	public CompositionElement(int width, int height, int left, int top, int zIndex, float degree) {
		this.width = width;
		this.height = height;
		this.left = left;
		this.top = top;
		this.zIndex = zIndex;
		this.degree = degree;
	}

	public CompositionElement(Composable meta, double resizeRatio) {
		this.width = (int)(meta.width * resizeRatio);
		this.height = (int)(meta.height * resizeRatio);
		this.left = (int)(meta.left * resizeRatio);
		this.top = (int)(meta.top * resizeRatio);
		this.zIndex = meta.zIndex;
		this.degree = meta.degree;
	}
}
