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
package com.naver.mei.sdk.view;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.compositor.type.SizeOptions;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrame;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrameHelper;
import com.naver.mei.sdk.core.image.meta.ComposableText;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.image.meta.TextPaintMeta;
import com.naver.mei.sdk.view.stickerview.ImageStickerView;
import com.naver.mei.sdk.view.stickerview.StickerView;
import com.naver.mei.sdk.view.stickerview.TextStickerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-10-24.
 */

public class MeiCanvasView extends RelativeLayout {
	private static final double AR_AUTO_FIT = 0.0;
	private static final int DEFAULT_FRAME_DELAY = 200;
	private static final FrameAlignment DEFAULT_FRAME_ALIGNMENT = FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP;

	private MeiImageView backgroundImageView;
	private int zIndex;
	private AnimationSynchronizer animationSynchronizer;
	private double aspectRatio = AR_AUTO_FIT;
	private double originalAspectRatio;

	private int frameDelay = DEFAULT_FRAME_DELAY;
	private FrameAlignment frameAlignment = DEFAULT_FRAME_ALIGNMENT;
	private PlayDirection playDirection = PlayDirection.FORWARD;
	private List<String> imagePaths;

	public MeiCanvasView(Context context) {
		super(context);
		init();
	}

	public MeiCanvasView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MeiCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public MeiCanvasView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		this.animationSynchronizer = new AnimationSynchronizer();
		this.backgroundImageView = new MeiImageView(getContext());
		this.backgroundImageView.supportTransparent(false);
		this.backgroundImageView.setFocusable(true);
		this.backgroundImageView.setClickable(true);
		this.backgroundImageView.setFocusableInTouchMode(true);
		addView(backgroundImageView);

	}

	public void setBackgroundImageURI(Uri uri) {
		backgroundImageView.setImageURI(uri);
		backgroundImageView.setAnimationSynchronizer(animationSynchronizer);
		animationSynchronizer.setBackgroundDuration(backgroundImageView.getDuration());
		originalAspectRatio = backgroundImageView.getOriginalAspectRatio();

		updateLayout();
		// TODO aspect ration에 따른 width, height 결정 필요
	}

	public void setBackgroundMultiFrame(ComposableMultiFrame composableMultiFrame) {
		backgroundImageView.setMultiFrame(composableMultiFrame);
		backgroundImageView.setAnimationSynchronizer(animationSynchronizer);
		animationSynchronizer.setBackgroundDuration(backgroundImageView.getDuration());
		originalAspectRatio = backgroundImageView.getOriginalAspectRatio();

		updateLayout();
	}

	public void setBackgroundMultiFrame(List<String> imagePaths, int frameDelay, FrameAlignment frameAlignment, PlayDirection playDirection) {
		this.frameDelay = frameDelay;
		this.frameAlignment = frameAlignment;
		this.imagePaths = imagePaths;

		updateBackgroundMultiFrame();
		originalAspectRatio = backgroundImageView.getOriginalAspectRatio();
	}

	private void updateBackgroundMultiFrame() {
		ComposableMultiFrame composableMultiFrame = aspectRatio == AR_AUTO_FIT
				? ComposableMultiFrameHelper.createComposableMultiFrame(imagePaths, SizeOptions.MAX_WIDTH_HEIGHT, frameDelay, frameAlignment, playDirection)
				: ComposableMultiFrameHelper.createComposableMultiFrame(imagePaths, getWidth(), (int) (getWidth() / aspectRatio), frameDelay, frameAlignment, playDirection);

		backgroundImageView.setMultiFrame(composableMultiFrame);
		backgroundImageView.setAnimationSynchronizer(animationSynchronizer);
		animationSynchronizer.setBackgroundDuration(backgroundImageView.getDuration());

		updateLayout();
	}

	public void setBackgroundMultiFrame(List<String> imagePaths, int frameDelay) {
		setBackgroundMultiFrame(imagePaths, frameDelay, FrameAlignment.KEEP_ORIGINAL_RATIO, PlayDirection.FORWARD);
	}

	public void setBackgroundMultiFrameAlignment(FrameAlignment frameAlignment) {
		this.frameAlignment = frameAlignment;
		if (imagePaths == null) return;
		updateBackgroundMultiFrame();
	}

	public void setBackgroundPlayDirection(PlayDirection playDirection) {
		this.playDirection = playDirection;
		this.backgroundImageView.setPlayDirection(playDirection);
		animationSynchronizer.setBackgroundDuration(backgroundImageView.getDuration());
	}

	public void setAspectRatio(double aspectRatio) {
		this.aspectRatio = aspectRatio;


		if (imagePaths == null) {
			updateLayout();
		} else {
			updateBackgroundMultiFrame();
		}
	}

	public double getOriginalAspectRatio() {
		return originalAspectRatio;
	}

	public void setSpeedRatio(double ratio) {
		animationSynchronizer.setSpeedRatio(ratio);
	}

	public double getSpeedRatio() {
		return animationSynchronizer.getSpeedRatio();
	}

	public void clearAllFocus() {
		List<TextStickerView> textStickerViews = getAllTextStickers();
		for (TextStickerView textStickerView : textStickerViews) {
			textStickerView.getEditText().clearFocus();
		}
	}

	private void updateLayout() {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) backgroundImageView.getLayoutParams();
		params.width = LayoutParams.MATCH_PARENT;
		params.height = aspectRatio == AR_AUTO_FIT ? LayoutParams.WRAP_CONTENT : (int) (getWidth() / aspectRatio);
		params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		backgroundImageView.setLayoutParams(params);
		backgroundImageView.setAdjustViewBounds(true);
		backgroundImageView.setScaleType(ImageView.ScaleType.FIT_XY);
	}

	public void addStickerView(StickerView stickerView) {
		addStickerView(stickerView, (int) backgroundImageView.getX(), (int) backgroundImageView.getY());
		stickerView.requestFocus();
	}

	public void addStickerView(StickerView stickerView, int x, int y) {
		stickerView.setZIndex(zIndex++);
		stickerView.setLocation(x, y);
		addView(stickerView);

		if (stickerView instanceof ImageStickerView) {
			ImageStickerView imageStickerView = (ImageStickerView) stickerView;
			imageStickerView.setAnimationSynchronizer(animationSynchronizer);
			animationSynchronizer.setMaxDurationIfGreatThen((int) (imageStickerView.getDuration() * imageStickerView.getPlayDirection().durationMultiplier));
		}
	}

	public ArrayList<Composable> getComposables() {
		ArrayList<Composable> metas = new ArrayList<>();
		List<StickerView> stickerViews = getAllStickers();

		metas.add(backgroundImageView.getComposable());

		for (StickerView stickerView : stickerViews) {
			try {
				if (stickerView instanceof TextStickerView) {
					TextStickerView textStickerView = (TextStickerView) stickerView;
					EditText editText = textStickerView.getEditText();
					int width = editText.getWidth() - (editText.getPaddingLeft() + editText.getPaddingRight());
					int height = editText.getLineHeight() * editText.getLineCount();
					int left = getRelativeLeft(editText) + editText.getPaddingLeft();
					int top = getRelativeTop(editText) + (editText.getMeasuredHeight() - height) / 2 + editText.getPaddingTop();

					metas.add(new ComposableText(editText.getText().toString(), width, height, left, top, new TextPaintMeta(editText.getPaint(), null), Layout.Alignment.ALIGN_CENTER, 0, textStickerView.getZIndex(), editText.getRotation()));
				} else {
					ImageStickerView imageStickerView = (ImageStickerView) stickerView;
					MeiImageView imageView = imageStickerView.getStickerImageView();
					metas.add(imageView.getComposable(getRelativeLeft(imageView), getRelativeTop(imageView), imageStickerView.getZIndex(), imageView.getRotation()));
				}
			} catch (Exception e) {
			}
		}

		return metas;
	}

	private List<StickerView> getAllStickers() {
		List<StickerView> allStickers = new ArrayList<>();
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			if (view instanceof StickerView) {
				StickerView stickerView = (StickerView) view;
				allStickers.add(stickerView);
			}
		}

		return allStickers;

	}

	private List<TextStickerView> getAllTextStickers() {
		List<StickerView> allStickers = getAllStickers();
		List<TextStickerView> allTextStickers = new ArrayList<>();

		for (StickerView stickerView : allStickers) {
			if (stickerView instanceof TextStickerView) {
				allTextStickers.add((TextStickerView)stickerView);
			}
		}

		return allTextStickers;
	}

	public MeiImageView getBackgroundImageView() {
		return backgroundImageView;
	}

	private int getRelativeLeft(View view) {
		return (int) view.getX() - (int) backgroundImageView.getX();
	}

	private int getRelativeTop(View view) {
		return (int) view.getY() - (int) backgroundImageView.getY();
	}
}
