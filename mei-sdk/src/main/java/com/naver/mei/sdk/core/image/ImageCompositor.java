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

import android.net.Uri;
import android.text.Layout;
import android.widget.EditText;
import android.widget.ImageView;

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.core.image.compositor.ImageCompositionAsyncTask;
import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.compositor.type.SizeOptions;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.ComposableImage;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrame;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrameHelper;
import com.naver.mei.sdk.core.image.meta.ComposableText;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.image.meta.TextPaintMeta;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.view.stickerview.ImageStickerView;
import com.naver.mei.sdk.view.stickerview.StickerView;
import com.naver.mei.sdk.view.stickerview.TextStickerView;
import com.naver.mei.sdk.view.MeiCanvasView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tigerbaby on 2016-10-20.
 */

public class ImageCompositor {
	private static final String TAG = "ImageCompositor";
	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_DELAY_MS = 500;

	private ComposableImage composableImage;
	private ComposableMultiFrame composableMultiFrame;
	private List<StickerView> stickerViews;
	private MeiEventListener meiEventListener;
	private boolean backgroundIsGif;
	private String savedFilePath;
	private MeiCanvasView meiCanvasView;
	private int outputWidth;
	private double speedRatio = 1.0;

	public ImageCompositor() {
		stickerViews = new ArrayList<>();
	}

	public ImageCompositor setMeiCanvasView(MeiCanvasView meiCanvasView) {
		this.meiCanvasView = meiCanvasView;
		this.speedRatio = meiCanvasView.getSpeedRatio();
		return this;
	}

	public ImageCompositor setBackgroundImage(BackgroundImage backgroundImage) {
		this.composableImage = BackgroundImageConverter.toComposableImage(backgroundImage);
		this.backgroundIsGif = false;
		return this;
	}

	public ImageCompositor setBackgroundImages(List<String> imagePaths) {
		return setBackgroundImages(imagePaths, DEFAULT_DELAY_MS, FrameAlignment.KEEP_ORIGINAL_RATIO, PlayDirection.FORWARD);
	}

	public ImageCompositor setBackgroundImages(List<String> imagePaths, int frameDelayMillis, FrameAlignment frameAlignment, PlayDirection playDirection) {
		this.composableMultiFrame = ComposableMultiFrameHelper.createComposableMultiFrame(imagePaths, SizeOptions.MAX_WIDTH_HEIGHT, frameDelayMillis, frameAlignment, playDirection);
		this.backgroundIsGif = true;
		return this;
	}

	public ImageCompositor addStickerView(StickerView stickerView) {
		this.stickerViews.add(stickerView);
		return this;
	}

	public ImageCompositor addStickerViews(List<StickerView> stickerViews) {
		this.stickerViews.addAll(stickerViews);
		return this;
	}

	public ImageCompositor setEventListener(MeiEventListener meiEventListener) {
		this.meiEventListener = meiEventListener;
		return this;
	}

	public ImageCompositor setSavedFilePath(String path) {
		this.savedFilePath = path;
		return this;
	}

	public ImageCompositor setOutputWidth(int width) {
		this.outputWidth = width;
		return this;
	}

	public ImageCompositor setSpeedRatio(double speedRatio) {
		this.speedRatio = speedRatio;
		return this;
	}

	/**
	 * 입력된 이미지와 스티커들을 합성한다.
	 * 결과는 ImageMakerListener의 callback으로 전달된다.
	 */
	public void composite() {
		final List<Composable> composables = new ArrayList<>();

		if (meiEventListener == null) {
			throw new MeiSDKException(MeiSDKErrorType.NEED_EVENT_LISTENER);
		}

		if (meiCanvasView != null) {
			composables.addAll(meiCanvasView.getComposables());
		} else {
			if (composableImage == null && composableMultiFrame == null) {
				meiEventListener.onFail(MeiSDKErrorType.FAILED_TO_LOAD_BACKGROUND);
				return;
			}

			composables.add(backgroundIsGif ? composableMultiFrame : composableImage);
			composables.addAll(getComposableFromStickerVies());
		}

		if (outputWidth == 0) {
			outputWidth = DEFAULT_WIDTH;
		}

		new ImageCompositionAsyncTask(composables, meiEventListener, savedFilePath, outputWidth, speedRatio).execute();
	}

	private List<Composable> getComposableFromStickerVies() {
		List<Composable> stickerComposables = new ArrayList<>();

		for (StickerView stickerView : stickerViews) {
			try {
				if (stickerView instanceof TextStickerView) {
					TextStickerView textStickerView = (TextStickerView) stickerView;
					EditText editText = textStickerView.getEditText();
					int width = editText.getWidth() - (editText.getPaddingLeft() + editText.getPaddingRight());
					int height = editText.getLineHeight() * editText.getLineCount();
					int left = editText.getLeft() + editText.getPaddingLeft();
					int top = editText.getTop() + (editText.getMeasuredHeight() - height) / 2 + editText.getPaddingTop();

					stickerComposables.add(new ComposableText(editText.getText().toString(), width, height, left, top, new TextPaintMeta(editText.getPaint(), null), Layout.Alignment.ALIGN_CENTER, 0, textStickerView.getZIndex(), editText.getRotation()));
				} else {
					ImageStickerView imageStickerView = (ImageStickerView) stickerView;
					Uri uri = imageStickerView.getUri();
					ImageView imageView = imageStickerView.getStickerImageView();
					stickerComposables.add(new ComposableImage(new URI(uri.toString()), imageView.getWidth(), imageView.getHeight(), imageView.getLeft(), imageView.getTop(), imageStickerView.getZIndex(), imageView.getRotation()));
				}
			} catch (Exception e) {
			}
		}

		return stickerComposables;
	}
}
