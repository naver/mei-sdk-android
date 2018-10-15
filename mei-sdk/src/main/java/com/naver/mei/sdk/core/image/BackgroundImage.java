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
package com.naver.mei.sdk.core.image;

import java.net.URI;

/**
 * Created by GTPark on 2016-10-20.
 */

public class BackgroundImage {
	private static final int DEFAULT_DELAY_MS = 500;
	private URI uri;
	private int width;
	private int height;
	private int delay;

	public BackgroundImage(URI uri, int width, int height) {
		this.uri = uri;
		this.width = width;
		this.height = height;
	}

	public BackgroundImage(URI uri, int width, int height, int delay) {
		this(uri, width, height);
		this.delay = delay;
	}

	public URI getUri() {
		return uri;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDelay() {
		if (delay == 0) {
			delay = DEFAULT_DELAY_MS;
		}

		return delay;
	}
}
