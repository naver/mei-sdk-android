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

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * StickerView의 movement를 관리하는 리스너
 * sticker의 이동에 따라 control, delete button의 이동도 같이 고려함
 */
class StickerViewOnTouchListener implements View.OnTouchListener {
	private StickerPoint pushStickerPoint;
	private int lastStickerLeft;
	private int lastStickerTop;

	private FrameLayout.LayoutParams stickerViewLayoutParams;
	private FrameLayout.LayoutParams controlBtnLayoutParams;
	private FrameLayout.LayoutParams deleteBtnLayoutParams;
	private FrameLayout.LayoutParams dummyLeftTopBtnLayoutParams;
	private FrameLayout.LayoutParams dummyLeftBottomBtnLayoutParams;

	private StickerPoint controlBtnStickerPoint;
	private StickerPoint deleteBtnStickerPoint;
	private StickerPoint dummyLeftTopBtnStickerPoint;
	private StickerPoint dummyLeftBottomBtnStickerPoint;

	private int lastStickerWidth;
	private int lastStickerHeight;

	private View controlView;
	private View deleteView;
	private View dummyLeftTop;
	private View dummyLeftBottom;

	private float moveX;
	private float moveY;

	private Context context;

	StickerViewOnTouchListener(Context context, View controlView, View deleteView, View dummyLeftTop, View dummyLeftBottom) {
		this.context = context;
		this.controlView = controlView;
		this.deleteView = deleteView;
		this.dummyLeftTop = dummyLeftTop;
		this.dummyLeftBottom = dummyLeftBottom;

		initPoint();
	}

	private void initPoint() {
		controlBtnLayoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
		controlBtnStickerPoint = new StickerPoint(controlBtnLayoutParams.leftMargin, controlBtnLayoutParams.topMargin);

		deleteBtnLayoutParams = (FrameLayout.LayoutParams) deleteView.getLayoutParams();
		deleteBtnStickerPoint = new StickerPoint(deleteBtnLayoutParams.leftMargin, deleteBtnLayoutParams.topMargin);

		dummyLeftBottomBtnLayoutParams = (FrameLayout.LayoutParams) dummyLeftBottom.getLayoutParams();
		dummyLeftBottomBtnStickerPoint = new StickerPoint(dummyLeftBottomBtnLayoutParams.leftMargin, dummyLeftBottomBtnLayoutParams.topMargin);

		dummyLeftTopBtnLayoutParams = (FrameLayout.LayoutParams) dummyLeftTop.getLayoutParams();
		dummyLeftTopBtnStickerPoint = new StickerPoint(dummyLeftTopBtnLayoutParams.leftMargin, dummyLeftTopBtnLayoutParams.topMargin);
	}

	@Override
	public boolean onTouch(View stickerView, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (null == stickerViewLayoutParams) {
					stickerViewLayoutParams = (FrameLayout.LayoutParams) stickerView.getLayoutParams();
				}

				controlBtnStickerPoint.update(controlBtnLayoutParams.leftMargin, controlBtnLayoutParams.topMargin);
				deleteBtnStickerPoint.update(deleteBtnLayoutParams.leftMargin, deleteBtnLayoutParams.topMargin);
				dummyLeftBottomBtnStickerPoint.update(dummyLeftBottomBtnLayoutParams.leftMargin, dummyLeftBottomBtnLayoutParams.topMargin);
				dummyLeftTopBtnStickerPoint.update(dummyLeftTopBtnLayoutParams.leftMargin, dummyLeftTopBtnLayoutParams.topMargin);

				pushStickerPoint = getRawPoint(event);
				lastStickerLeft = stickerViewLayoutParams.leftMargin;
				lastStickerTop = stickerViewLayoutParams.topMargin;
				lastStickerWidth = stickerView.getWidth();
				lastStickerHeight = stickerView.getHeight();
				break;
			case MotionEvent.ACTION_MOVE:
				if ((stickerView instanceof EditText) && stickerView.hasFocus()) return false;

				setEditable(stickerView, false);
				StickerPoint newStickerPoint = getRawPoint(event);
				moveX = newStickerPoint.x - pushStickerPoint.x;
				moveY = newStickerPoint.y - pushStickerPoint.y;

				stickerViewLayoutParams.leftMargin = (int) (lastStickerLeft + moveX);
				stickerViewLayoutParams.topMargin = (int) (lastStickerTop + moveY);
				stickerViewLayoutParams.width = lastStickerWidth;
				stickerViewLayoutParams.height = lastStickerHeight;
				stickerView.setLayoutParams(stickerViewLayoutParams);

				controlBtnLayoutParams.leftMargin = (int) (controlBtnStickerPoint.x + moveX);
				controlBtnLayoutParams.topMargin = (int) (controlBtnStickerPoint.y + moveY);
				controlView.setLayoutParams(controlBtnLayoutParams);

				deleteBtnLayoutParams.leftMargin = (int) (deleteBtnStickerPoint.x + moveX);
				deleteBtnLayoutParams.topMargin = (int) (deleteBtnStickerPoint.y + moveY);
				deleteView.setLayoutParams(deleteBtnLayoutParams);

				dummyLeftTopBtnLayoutParams.leftMargin = (int) (dummyLeftTopBtnStickerPoint.x + moveX);
				dummyLeftTopBtnLayoutParams.topMargin = (int) (dummyLeftTopBtnStickerPoint.y + moveY);
				dummyLeftTop.setLayoutParams(dummyLeftTopBtnLayoutParams);

				dummyLeftBottomBtnLayoutParams.leftMargin = (int) (dummyLeftBottomBtnStickerPoint.x + moveX);
				dummyLeftBottomBtnLayoutParams.topMargin = (int) (dummyLeftBottomBtnStickerPoint.y + moveY);
				dummyLeftBottom.setLayoutParams(dummyLeftBottomBtnLayoutParams);
				break;

			case MotionEvent.ACTION_UP:
				if (Math.abs(moveX) < 10 && Math.abs(moveY) < 10) {
					setEditable(stickerView, true);
				}

				break;
		}
		return false;
	}


	private StickerPoint getRawPoint(MotionEvent event) {
		return new StickerPoint((int) event.getRawX(), (int) event.getRawY());
	}

	private void setEditable(View stickerView, boolean editable) {
		if (!(stickerView instanceof EditText)) return;

		EditText editTextStickerView = (EditText)stickerView;

		if (editable) {
			editTextStickerView.setKeyListener(new EditText(context).getKeyListener());
		} else {
			editTextStickerView.clearFocus();
			editTextStickerView.setKeyListener(null);
		}
		editTextStickerView.setFocusable(editable);
		editTextStickerView.setFocusableInTouchMode(editable);
		editTextStickerView.setClickable(editable);
	}
}
