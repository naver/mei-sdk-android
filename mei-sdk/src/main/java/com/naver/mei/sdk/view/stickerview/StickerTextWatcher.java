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

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.naver.mei.sdk.core.utils.PixelUtils;
import com.naver.mei.sdk.core.utils.TrigonometryUtils;

/**
 * Created by Naver on 2016-09-23.
 */
public class StickerTextWatcher implements TextWatcher {
	TextStickerView stickerView;
	EditText editText;
	ImageView controlButton;
	ImageView deleteButton;
	int nowLineCount;
	int textHeight;
	int textWidth;
	int lastStickerAngle;
	int editTextMinWidth;
	int editTextMaxWidth;
	String changedText;
	int space;

	private FrameLayout.LayoutParams editTextLayoutParams;
	private FrameLayout.LayoutParams controlBtnLayoutParams;
	private FrameLayout.LayoutParams deleteBtnLayoutParams;

	private int controlImgWidth;
	private int controlImgHeight;
	private int deleteImgWidth;
	private int deleteImgHeight;

	public StickerTextWatcher(TextStickerView stickerView, int maxEditTextWidth) {
		this(stickerView.getEditText(), stickerView.getControlButton(), stickerView.getDeleteButton(), maxEditTextWidth);
	}

	public StickerTextWatcher(EditText editText, ImageView controlButton, ImageView deleteButton, int maxEditTextWidth) {
		this.editText = editText;
		this.controlButton = controlButton;
		this.deleteButton = deleteButton;
		this.editTextMinWidth = editText.getLayoutParams().width;
		this.editTextMaxWidth = maxEditTextWidth;
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		editTextLayoutParams = (FrameLayout.LayoutParams) editText.getLayoutParams();

		if (nowLineCount != 0 && nowLineCount < editText.getLineCount()) {
			editTextLayoutParams.height = editTextLayoutParams.height + textHeight;
			editText.setLayoutParams(editTextLayoutParams);

			refreshButtonPosition();
		} else if (nowLineCount != 0 && nowLineCount > editText.getLineCount()) {
			editTextLayoutParams.height = editTextLayoutParams.height - textHeight;
			editText.setLayoutParams(editTextLayoutParams);

			refreshButtonPosition();
		}

		nowLineCount = editText.getLineCount();
		lastStickerAngle = (int) editText.getRotation();
		textHeight = ((int) editText.getTextSize()) + PixelUtils.dp2px(5);


		if (count > 0 && after == 0) { // delete
			changedText = TextUtils.substring(s.toString(), start, start + count);
			space = (int) editText.getPaint().measureText(changedText);

		}

	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		String text = s.toString();
		textWidth = (int) editText.getPaint().measureText(text);

		if (count > 0 && before == 0) { // add
			changedText = TextUtils.substring(text, start, start + count);
			space = (int) editText.getPaint().measureText(changedText);
		} else if (isKorean(changedText) && (count == before)) {
			space = 0;
		}

		if (textWidth > editTextLayoutParams.width) {
			if (editTextLayoutParams.width >= editTextMaxWidth) {
				editTextLayoutParams.width = editTextMaxWidth;
			} else {
				editTextLayoutParams.width = editTextLayoutParams.width + space;
			}

			editText.setLayoutParams(editTextLayoutParams);

			refreshButtonPosition();
		} else if (textWidth < editTextLayoutParams.width) {
			if (editTextLayoutParams.width <= editTextMinWidth) {
				editTextLayoutParams.width = editTextMinWidth;
			} else {
				if (textWidth == 0) {
					editTextLayoutParams.width = editTextMinWidth;
				} else {
					editTextLayoutParams.width = editTextLayoutParams.width - space;
				}
			}

			editText.setLayoutParams(editTextLayoutParams);

			refreshButtonPosition();
		}

	}

	private boolean isKorean(String text) {
		if (!TextUtils.isEmpty(text)) {
			return text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
		}
		return false;
	}

	private void refreshButtonPosition() {
		controlBtnLayoutParams = (FrameLayout.LayoutParams) controlButton.getLayoutParams();
		deleteBtnLayoutParams = (FrameLayout.LayoutParams) deleteButton.getLayoutParams();

		controlImgWidth = controlBtnLayoutParams.width;
		controlImgHeight = controlBtnLayoutParams.height;

		deleteImgWidth = deleteBtnLayoutParams.width;
		deleteImgHeight = deleteBtnLayoutParams.height;

		StickerPoint O = getCenterPoint();
		StickerPoint imageRB = new StickerPoint(editText.getLeft() + editTextLayoutParams.width, editText.getTop() + editTextLayoutParams.height);
		StickerPoint angleStickerPointForArrow = TrigonometryUtils.getAnglePoint(O, imageRB, lastStickerAngle);

		StickerPoint imageLB = new StickerPoint(editText.getLeft(), editText.getTop() + editTextLayoutParams.height);
		float degree = TrigonometryUtils.getDegree(O, imageLB, imageRB);
		StickerPoint angleStickerPointForDelete = TrigonometryUtils.getAnglePoint(O, imageRB, lastStickerAngle - (180 - degree));

		controlBtnLayoutParams.leftMargin = (int) (angleStickerPointForArrow.x - controlImgWidth / 2);
		controlBtnLayoutParams.topMargin = (int) (angleStickerPointForArrow.y - controlImgHeight / 2);
		controlButton.setLayoutParams(controlBtnLayoutParams);

		deleteBtnLayoutParams.leftMargin = (int) (angleStickerPointForDelete.x - deleteImgWidth / 2);
		deleteBtnLayoutParams.topMargin = (int) (angleStickerPointForDelete.y - deleteImgHeight / 2);
		deleteButton.setLayoutParams(deleteBtnLayoutParams);
	}

	@Override
	public void afterTextChanged(Editable editable) {
	}

	private StickerPoint getCenterPoint() {
		int x = editText.getLeft() + editTextLayoutParams.width / 2;  // getwidth()??
		int y = editText.getTop() + editTextLayoutParams.height / 2;
		return new StickerPoint(x, y);
	}
}
