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

import android.graphics.BitmapFactory;

import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.compositor.type.SizeOptions;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-12-19.
 */

public class ComposableMultiFrameHelper {
	public static ComposableMultiFrame createComposableMultiFrame(List<String> imagePaths, int multiFrameWidth, int multiFrameHeight, int frameDelay, FrameAlignment frameAlignment, PlayDirection playDirection) {
		List<FrameMeta> frameMetas = new ArrayList<>();
		for (String imagePath : imagePaths) {
			frameMetas.add(
					frameAlignment.strategy.createFrameMeta(
							imagePath,
							multiFrameWidth,
							multiFrameHeight,
							frameDelay)
			);
		}

		return new ComposableMultiFrame(frameMetas, multiFrameWidth, multiFrameHeight, 0, 0, 0, 0, playDirection);
	}

	public static ComposableMultiFrame createComposableMultiFrame(List<String> imagePaths, SizeOptions sizeOptions, int frameDelay, FrameAlignment frameAlignment, PlayDirection playDirection) {
		int maxWidth = 0;
		int maxHeight = 0;

		for (String imagePath : imagePaths) {
			URI uri = URIUtils.pathToUri(imagePath);
			BitmapFactory.Options options = MeiImageProcessor.getImageBoundsOptions(uri);
			int orientationDegree = MeiImageProcessor.getImageOrientationDegree(uri);
			int width = orientationDegree == 0 || orientationDegree == 180 ? options.outWidth : options.outHeight;
			int height = orientationDegree == 0 || orientationDegree == 180 ? options.outHeight : options.outWidth;

			if (width > maxWidth) maxWidth = width;
			if (height > maxHeight) maxHeight = height;
		}

		return createComposableMultiFrame(imagePaths, maxWidth, maxHeight, frameDelay, frameAlignment, playDirection);
	}
}
