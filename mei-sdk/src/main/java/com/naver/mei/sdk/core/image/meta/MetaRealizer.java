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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.text.StaticLayout;
import android.util.Log;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.image.util.IOHelper;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.gif.decoder.GifDecoder;
import com.naver.mei.sdk.core.image.animated.AnimatedGif;
import com.naver.mei.sdk.core.image.animated.AnimatedMultiFrame;
import com.naver.mei.sdk.core.image.animated.MultiFrame;
import com.naver.mei.sdk.core.image.compositor.element.AnimatedGifElement;
import com.naver.mei.sdk.core.image.compositor.element.AnimatedMultiFrameElement;
import com.naver.mei.sdk.core.image.compositor.element.BitmapElement;
import com.naver.mei.sdk.core.image.compositor.element.CompositionElement;
import com.naver.mei.sdk.core.utils.GIFUtils;

import org.apache.commons.io.IOUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-10-12.
 */

public class MetaRealizer {
	private double resizeRatio = 1.0;

	public MetaRealizer(double resizeRatio) {
		this.resizeRatio = resizeRatio;
	}

	public CompositionElement parse(Composable meta) {
		long startTime = System.currentTimeMillis();
		try {
			if (meta instanceof ComposableImage) {
				return createImageElement((ComposableImage) meta);
			} else if (meta instanceof ComposableText) {
				return createTextElement((ComposableText) meta);
			} else if (meta instanceof ComposableMultiFrame) {
				return createMultiFrameElement((ComposableMultiFrame) meta);
			} else {
				throw new MeiSDKException(MeiSDKErrorType.UNKNOWN_META_TYPE);
			}
		} finally {
			Log.d("GTPARK", "parse time : " + (System.currentTimeMillis() - startTime) + "//" + meta.getClass().getSimpleName());
		}
	}

	public List<CompositionElement> parse(List<Composable> metas) {
		List<CompositionElement> compositionElements = new ArrayList<>();
		for (Composable meta : metas) {
			compositionElements.add(parse(meta));
		}

		return compositionElements;
	}

	private CompositionElement createMultiFrameElement(ComposableMultiFrame meta) {
		return new AnimatedMultiFrameElement(meta, resizeRatio);
	}

	public static AnimatedGif createAnimatedGif(URI uri) {
		byte[] bytesImage = IOHelper.getImageBytes(uri);
		if (!GIFUtils.isGif(bytesImage)) return null;
		return new AnimatedGif(bytesImage);
	}

	public static AnimatedMultiFrame  createAnimatedMultiFrame(MultiFrame multiFrame) {
		return new AnimatedMultiFrame(multiFrame, 1.0);
	}

	private CompositionElement createImageElement(ComposableImage meta) {
		byte[] bytesImage = IOHelper.getImageBytes(meta.uri);
		if (GIFUtils.isGif(bytesImage)) {
			GifDecoder decoder = new GifDecoder();
			decoder.read(bytesImage);
			return new AnimatedGifElement(decoder, meta, resizeRatio);
		} else {
			return new BitmapElement(bytesImage, meta, resizeRatio);
		}
	}

	private BitmapElement createTextElement(ComposableText textMeta) {
		Bitmap bitmap = Bitmap.createBitmap(textMeta.width, textMeta.height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		String textOnCanvas = textMeta.text;
		Rect bounds = canvas.getClipBounds();

		int width = bounds.width() - textMeta.padding;

		//text will be drawn from left
		float textXCoordinate = bounds.left;
		canvas.translate(textXCoordinate + textMeta.padding, textMeta.padding);

//		if (textMeta.outlineTextPaintMeta != null) {
//			StaticLayout outlineText = new StaticLayout(textOnCanvas, textMeta.outlineTextPaintMeta.toTextPaint(), width, Layout.Alignment.ALIGN_CENTER, 1, 0, true);
//			outlineText.draw(canvas);
//		}

		if (textMeta.textPaintMeta != null) {
			StaticLayout inlineText = new StaticLayout(textOnCanvas, textMeta.textPaintMeta.toTextPaint(), width, textMeta.textAlignment, 1, 0, true);
			inlineText.draw(canvas);
		}

		return new BitmapElement(
				bitmap,
				textMeta,
				resizeRatio
		);
	}
}
