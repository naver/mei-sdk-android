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

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.util.IOHelper;

/**
 * Created by GTPark on 2017-04-26.
 */

public class WatermarkHelper {
	public static Bitmap drawWatermark(Bitmap backgroundBitmap, WatermarkOptions watermarkOptions) {
		// no watermark
		if (watermarkOptions == null || watermarkOptions.uri == null) {
			return backgroundBitmap;
		}

		Bitmap watermarkBitmap = IOHelper.getBitmap(watermarkOptions.uri);
		WatermarkOptions determinedWatermarkOptions = determineWatermarkSize(watermarkBitmap, watermarkOptions);

		watermarkBitmap = MeiImageProcessor.resize(watermarkBitmap, determinedWatermarkOptions.width, determinedWatermarkOptions.height);
		int left = watermarkOptions.position.getLeft(backgroundBitmap.getWidth(), watermarkBitmap.getWidth(), watermarkOptions.margin);
		int top = watermarkOptions.position.getTop(backgroundBitmap.getHeight(), watermarkBitmap.getHeight(), watermarkOptions.margin);

		Canvas canvas = new Canvas(backgroundBitmap);
		canvas.drawBitmap(watermarkBitmap, left, top, null);

		return backgroundBitmap;
	}

	private static WatermarkOptions determineWatermarkSize(Bitmap watermarkBitmap, WatermarkOptions watermarkOptions) {
		int targetWatermarkWidth = watermarkOptions.width;
		int targetWatermarkHeight = watermarkOptions.height;

		if (targetWatermarkWidth <= 0 && targetWatermarkHeight <= 0) {
			targetWatermarkWidth = watermarkBitmap.getWidth();
			targetWatermarkHeight = watermarkBitmap.getHeight();
		} else if (targetWatermarkWidth <= 0) {
			targetWatermarkWidth = (int) (watermarkBitmap.getWidth() * (watermarkOptions.height / (double) watermarkBitmap.getHeight()));
		} else if (targetWatermarkHeight <= 0) {
			targetWatermarkHeight = (int) (watermarkBitmap.getHeight() * (watermarkOptions.width / (double) watermarkBitmap.getWidth()));
		}

		return new WatermarkOptions(watermarkOptions.uri, targetWatermarkWidth, targetWatermarkHeight, watermarkOptions.position, watermarkOptions.margin);
	}
}
