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

import java.net.URI;

/**
 * Created by GTPark on 2016-10-18.
 */

public class ComposableImage extends Composable {
	public final URI uri;
	public final PlayDirection playDirection;
	public final int orientationDegree;

	public ComposableImage(URI uri, int width, int height, int left, int top, int zIndex, float degree) {
		this(uri, width, height, left, top, zIndex, degree, 0, PlayDirection.FORWARD);
	}

	public ComposableImage(URI uri, int width, int height, int left, int top, int zIndex, float degree, int orientationDegree, PlayDirection direction) {
		super(width, height, left, top, zIndex, degree);
		this.uri = uri;
		this.playDirection = direction;
		this.orientationDegree = orientationDegree;
	}
}
