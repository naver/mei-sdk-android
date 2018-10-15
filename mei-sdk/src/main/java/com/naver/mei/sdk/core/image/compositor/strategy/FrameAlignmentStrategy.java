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
package com.naver.mei.sdk.core.image.compositor.strategy;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.meta.FrameMeta;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.net.URI;

/**
 * Created by GTPark on 2016-12-19.
 */

public abstract class FrameAlignmentStrategy {
	public FrameMeta createFrameMeta(String imagePath, int multiFrameWidth, int multiFrameHeight, int msDelay) {
		URI imageURI = URIUtils.pathToUri(imagePath);
		BitmapFactory.Options options = MeiImageProcessor.getImageBoundsOptions(imageURI);
		double multiFrameAspectRatio = multiFrameWidth / (double)multiFrameHeight;
		int orienDegree = MeiImageProcessor.getImageOrientationDegree(imageURI);
		int frameWidth = orienDegree == 0 || orienDegree == 180 ? options.outWidth : options.outHeight;
		int frameHeight = orienDegree == 0 || orienDegree == 180 ? options.outHeight : options.outWidth;
		double frameAspectRatio = frameWidth / (double)frameHeight;

		return createFrameMeta(URIUtils.pathToUri(imagePath), multiFrameWidth, multiFrameHeight, multiFrameAspectRatio, frameAspectRatio, msDelay, orienDegree);
	}

	abstract protected FrameMeta createFrameMeta(URI uri, int multiFrameWidth, int multiFrameHeight, double multiFrameAspectRatio, double frameAspectRatio, int msDelay, int degree);

	/**
	 * MultiFrame AspectRatio > Frame AspectRatio : width fit
	 * MultiFrame AspectRatio < Frame AspectRatio : height fit
	 */
	public static class FitShortAxisCenterCrop extends FrameAlignmentStrategy {
		@Override
		protected FrameMeta createFrameMeta(URI uri, int multiFrameWidth, int multiFrameHeight, double multiFrameAspectRatio, double frameAspectRatio, int msDelay, int degree) {
			int width = (int)(multiFrameAspectRatio >= frameAspectRatio ? multiFrameWidth : multiFrameHeight * frameAspectRatio);
			int height = (int)(multiFrameAspectRatio >= frameAspectRatio ? multiFrameWidth / frameAspectRatio : multiFrameHeight);

			return new FrameMeta(uri, width, height, (multiFrameWidth - width) / 2, (multiFrameHeight - height) / 2, msDelay, degree);
		}
	}

	/**
	 * MultiFrame AspectRatio > Frame AspectRatio : width resize, height fit
	 * MultiFrame AspectRatio < Frame AspectRatio : width fit, height resize
	 */

	public static class KeepOriginalRatio extends FrameAlignmentStrategy {
		@Override
		protected FrameMeta createFrameMeta(URI uri, int multiFrameWidth, int multiFrameHeight, double multiFrameAspectRatio, double frameAspectRatio, int msDelay, int degree) {
			int width = (int)(multiFrameAspectRatio >= frameAspectRatio ? multiFrameHeight * frameAspectRatio : multiFrameWidth);
			int height = (int)(multiFrameAspectRatio >= frameAspectRatio ? multiFrameHeight : multiFrameWidth / frameAspectRatio);

			return new FrameMeta(uri, width, height, (multiFrameWidth - width) / 2, (multiFrameHeight - height) / 2, msDelay, degree);
		}
	}
}
