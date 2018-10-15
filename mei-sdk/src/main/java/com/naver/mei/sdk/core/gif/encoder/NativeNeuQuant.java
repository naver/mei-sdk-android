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
 * Created by tigerbaby on 2016-12-13.
		*/

public class NativeNeuQuant {
	public native void init(byte[] thepic, int len, int sample);

	public native byte[] process();

	public native int map(int b, int g, int r);

	public MapResult map(byte[] pixels, int mapQuality) {
		if (mapQuality == 8) return map(pixels);
		return mapByQuality(pixels, mapQuality);
	}

	private native MapResult map(byte[] pixels);

	private native MapResult mapByQuality(byte[] pixels, int quality);

	static {
		System.loadLibrary("neuquant");
	}
}
