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
package com.naver.mei.sdk.core.image.compositor.element;

import android.graphics.Bitmap;

import com.naver.mei.sdk.core.image.meta.ComposableText;
import com.naver.mei.sdk.core.image.meta.ComposableImage;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;

/**
 * Created by GTPark on 2016-10-11.
 */

public class BitmapElement extends CompositionElement {
	public final Bitmap bitmap;

	public BitmapElement(byte[] bytesImage, ComposableImage meta, double resizeRatio) {
		super(meta, resizeRatio);
		int orientationDegree = MeiImageProcessor.getImageOrientationDegree(bytesImage);
		int rWidth = orientationDegree == 0 || orientationDegree == 180 ? width : height;
		int rHeight = orientationDegree == 0 || orientationDegree == 180 ? height : width;

		this.bitmap = MeiImageProcessor.rotate(MeiImageProcessor.decodeAndResize(bytesImage, rWidth, rHeight), orientationDegree);
	}

	public BitmapElement(Bitmap bitmap, ComposableText meta, double resizeRatio) {
		super(meta, resizeRatio);
		this.bitmap = MeiImageProcessor.resize(bitmap, resizeRatio);
	}
}
