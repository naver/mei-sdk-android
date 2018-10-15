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

import java.io.Serializable;

/**
 * Created by tigerbaby on 2016-10-21.
 */

public class VideoToGifParams implements Serializable {
	final public String videoUri;
	final public long startMs;
	final public long endMs;
	final public int fps;
	public int targetWidth;
	public int targetHeight;
	public CropOptions cropOptions;
	public WatermarkOptions watermarkOptions;

	public VideoToGifParams(String videoUri, long startMs, long endMs, int fps) {
		this.videoUri = videoUri;
		this.startMs = startMs;
		this.endMs = endMs;
		this.fps = fps;
	}

	public VideoToGifParams setTargetSize(int targetWidth, int targetHeight) {
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		return this;
	}

	public VideoToGifParams setCropOptions(CropOptions cropOptions) {
		this.cropOptions = cropOptions;
		return this;
	}

	public VideoToGifParams setWatermarkOptions(WatermarkOptions watermarkOptions) {
		this.watermarkOptions = watermarkOptions;
		return this;
	}

	public VideoToGifParams clone() {
		return new VideoToGifParams(videoUri, startMs, endMs, fps)
				.setTargetSize(targetWidth, targetHeight)
				.setCropOptions(cropOptions)
				.setWatermarkOptions(watermarkOptions);
	}

}
