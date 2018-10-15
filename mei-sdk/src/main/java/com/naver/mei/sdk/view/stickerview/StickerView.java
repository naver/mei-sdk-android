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
package com.naver.mei.sdk.view.stickerview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.naver.mei.sdk.R;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by Naver on 2016-09-12.
 */
public class StickerView extends LinearLayout {  // framely
	protected View sticker;
	private ImageView controlButton; // on off
	private ImageView deleteButton; // on off
	private ImageView dummyLeftTopButton;
	private ImageView dummyLeftBottomButton;

	private boolean hasSetParamsForView = false;
	private float stickerHeight;
	private float stickerWidth;
	private float controlButtonHeight;
	private float controlButtonWidth;
	private float deleteButtonHeight;
	private float deleteButtonWidth;
	private int leftMargin = 0;
	private int topMargin = 0;
	private int zIndex = 0;

	protected float maxEditTextWidthRatio = 0.9f;	// edit text의 배경이미지 대비 최대 너비 비율

	public StickerView(Context context, int resourceId) {
		super(context);
		initView(resourceId);
	}

	public StickerView(Context context, AttributeSet attrs, int resourceId) {
		super(context, attrs);

		if (attrs != null) {
			TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StickerView);

			int controlImageResourceId = typedArray.getResourceId(R.styleable.StickerView_controlImage, R.drawable.rotate);
			controlButton.setImageResource(controlImageResourceId);

			int deleteImageResourceId = typedArray.getResourceId(R.styleable.StickerView_deleteImage, R.drawable.delete);
			deleteButton.setImageResource(deleteImageResourceId);

			maxEditTextWidthRatio = typedArray.getFloat(R.styleable.StickerView_maxEditTextWidthRatio, 0.9f);

			validate();

			typedArray.recycle();

		}

		initView(resourceId);
	}

	private void initView(int resourceId) {

		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
		final View v = li.inflate(resourceId, this, false);
		addView(v);

		LayoutParams layoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
		setLayoutParams(layoutParams);

		sticker = findViewById(R.id.sticker);
		deleteButton = (ImageView) findViewById(R.id.sticker_delete);
		controlButton = (ImageView) findViewById(R.id.sticker_control);
		dummyLeftTopButton = (ImageView) findViewById(R.id.sticker_dummy_left_top);
		dummyLeftBottomButton = (ImageView) findViewById(R.id.sticker_dummy_left_bottom);

		controlButton.setBackgroundResource(R.drawable.rotate);
		deleteButton.setBackgroundResource(R.drawable.delete);
		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (StickerView.this.getParent() != null) {
					ViewGroup myCanvas = ((ViewGroup) StickerView.this.getParent());
					myCanvas.removeView(StickerView.this);
				}
			}
		});

		controlButton.setOnTouchListener(new StickerControlTouchListener(sticker, deleteButton, dummyLeftTopButton, dummyLeftBottomButton, getDisplayWidth()));
		sticker.setOnTouchListener(new StickerViewOnTouchListener(getContext(), controlButton, deleteButton, dummyLeftTopButton, dummyLeftBottomButton));
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setParamsForView();
	}

	private void setParamsForView() {
		ViewGroup.LayoutParams layoutParams = getLayoutParams();
		if (null != layoutParams && !hasSetParamsForView) {
			hasSetParamsForView = true;
			this.stickerHeight = sticker.getMeasuredHeight();
			this.stickerWidth = sticker.getMeasuredWidth();
			this.controlButtonHeight = controlButton.getMeasuredHeight();
			this.controlButtonWidth = controlButton.getMeasuredWidth();
			this.deleteButtonHeight = deleteButton.getMeasuredHeight();
			this.deleteButtonWidth = deleteButton.getMeasuredWidth();

			FrameLayout.LayoutParams stickerLayoutParams = (FrameLayout.LayoutParams) this.sticker.getLayoutParams();

			stickerLayoutParams.leftMargin = leftMargin > 0 ? leftMargin : 0;
			stickerLayoutParams.topMargin = topMargin > 0 ? topMargin : 0;

			FrameLayout.LayoutParams controlButtonLayoutParams = (FrameLayout.LayoutParams) controlButton.getLayoutParams();
			controlButtonLayoutParams.leftMargin = (int) (stickerLayoutParams.leftMargin + stickerWidth - controlButtonWidth / 2);
			controlButtonLayoutParams.topMargin = (int) (stickerLayoutParams.topMargin + stickerHeight - controlButtonHeight / 2);
			controlButton.setLayoutParams(controlButtonLayoutParams);

			FrameLayout.LayoutParams deleteButtonLayoutParams = (FrameLayout.LayoutParams) deleteButton.getLayoutParams();
			deleteButtonLayoutParams.leftMargin = (int) (stickerLayoutParams.leftMargin + stickerWidth - deleteButtonWidth / 2);
			deleteButtonLayoutParams.topMargin = (int) (stickerLayoutParams.topMargin - deleteButtonHeight / 2);
			deleteButton.setLayoutParams(deleteButtonLayoutParams);
		}

	}

	public void setLocation(int left, int top) {
		this.leftMargin = left;
		this.topMargin = top;
	}

	public void setControlButtonImage(int controlButtonImage) {
		controlButton.setBackgroundResource(controlButtonImage);
	}

	public void setDeleteButtonImage(int deleteButtonImage) {
		deleteButton.setBackgroundResource(deleteButtonImage);
	}

	public View getSticker() {
		return sticker;
	}


	public ImageView getControlButton() {
		return controlButton;
	}

	public ImageView getDeleteButton() {
		return deleteButton;
	}

	public void showOnlySticker() {
		sticker.setBackgroundResource(0);
		controlButton.setVisibility(INVISIBLE);
		deleteButton.setVisibility(INVISIBLE);
	}

	public void showWholeSticker() {
		sticker.setBackgroundResource(R.drawable.image_border);
		controlButton.setVisibility(VISIBLE);
		deleteButton.setVisibility(VISIBLE);
		bringToFront();
	}

	public int getZIndex() {
		return zIndex;
	}

	public void setZIndex(int zIndex) {
		this.zIndex = zIndex;
	}

	private int getDisplayWidth() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()	.getMetrics(displayMetrics);
		return displayMetrics.widthPixels;

	}

	protected void validate() {
		if (maxEditTextWidthRatio < 0.5 || maxEditTextWidthRatio >= 1.0) {
			throw new MeiSDKException(MeiSDKErrorType.INVALID_MAX_WIDTH_RATIO_VALUE);
		}
	}


}