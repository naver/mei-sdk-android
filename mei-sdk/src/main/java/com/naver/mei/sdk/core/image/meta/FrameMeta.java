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
import java.net.URI;

/**
 * Created by GTPark on 2016-10-12.
 */

public class FrameMeta implements Serializable {
	public final URI uri;
	public final int width;
	public final int height;
	public final int left;
	public final int top;
	public final int msDelay;
	public final int orientationDegree;

	public FrameMeta(URI uri, int width, int height, int left, int top, int msDelay) {
		this(uri, width, height, left, top, msDelay, 0);

	}

	public FrameMeta(URI uri, int width, int height, int left, int top, int msDelay, int orientationDegree) {
		this.uri = uri;
		this.width = width;
		this.height = height;
		this.left = left;
		this.top = top;
		this.msDelay = msDelay;
		this.orientationDegree = orientationDegree;
	}

	public FrameMeta resize(double resizeRatio) {
		return new FrameMeta(uri, (int)(width * resizeRatio), (int)(height * resizeRatio), (int)(left * resizeRatio), (int)(top * resizeRatio), msDelay, orientationDegree);
	}
}
