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
package com.naver.mei.sdk.core.image.meta;

import java.io.Serializable;

/**
 * Created by GTPark on 2016-10-18.
 */

public class Composable implements Serializable {
	public final int width;
	public final int height;
	public final int left;
	public final int top;
	public final int zIndex;
	public final float degree;

	public Composable(int width, int height, int left, int top, int zIndex, float degree) {
		this.width = width;
		this.height = height;
		this.left = left;
		this.top = top;
		this.zIndex = zIndex;
		this.degree = degree;
	}

	public Composable(RectSize size, int left, int top, int zIndex, float degree) {
		this(size.width, size.height, left, top, zIndex, degree);
	}

	public static class RectSize {
		public final int width;
		public final int height;

		public RectSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}
}
