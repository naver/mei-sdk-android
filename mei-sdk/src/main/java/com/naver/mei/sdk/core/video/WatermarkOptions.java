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
package com.naver.mei.sdk.core.video;

/**
 * Created by tigerbaby on 2017-04-14.
 */

public class WatermarkOptions {
	public final String uri;
	public final int width;
	public final int height;
	public final int margin;
	public final WatermarkPosition position;

	public WatermarkOptions(String uri, WatermarkPosition position, int margin) {
		this(uri, 0, 0, position, margin);
	}

	public WatermarkOptions(String uri, int width, int height, WatermarkPosition position, int margin) {
		this.uri = uri;
		this.width = width;
		this.height = height;
		this.position = position == null ? WatermarkPosition.RIGHT_TOP : position;  //as default RIGHT_TOP
		this.margin = margin;
	}
}
