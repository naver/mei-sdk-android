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

package com.naver.mei.sdk.core.gif.encoder;

/**
 * Created by tigerbaby on 2017-04-25.
 */

public class GifEncodingOptions {
	private static int DEFAULT_QUALITY = 10;
	private static int DEFAULT_COLOR_LEVEL = 7;

	private int quality;
	private int colorLevel;

	public GifEncodingOptions(int quality, int colorLevel) {
		this.quality = quality;
		this.colorLevel = colorLevel;
	}

	public static GifEncodingOptions asDefault() {
		return new GifEncodingOptions(DEFAULT_QUALITY, DEFAULT_COLOR_LEVEL);
	}

	public int getQuality() {
		return quality;
	}

	public void setQuality(int quality) {
		this.quality = quality;
	}

	public int getColorLevel() {
		return colorLevel;
	}

	public void setColorLevel(int colorLevel) {
		this.colorLevel = colorLevel;
	}
}
