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
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.naver.mei.sdk.R;
import com.naver.mei.sdk.core.utils.SoftKeyboardHelper;

/**
 * Created by Naver on 2016-10-20.
 */

public class TextStickerView extends StickerView {
	public TextStickerView(Context context) {
		super(context, R.layout.sticker_text_view);
		getEditText().setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					SoftKeyboardHelper.hideKeyboard(getContext(), v);
					showOnlySticker();
				} else {
					showWholeSticker();
				}
			}
		});
	}

	public EditText getEditText() {
		return (EditText) sticker;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		int backImageWidth = ((ViewGroup) getParent()).getWidth();
		int maxEditTextWidth = (int) (backImageWidth * maxEditTextWidthRatio);
		getEditText().addTextChangedListener(new StickerTextWatcher(this, maxEditTextWidth));
	}

	public Editable getText() {
		return getEditText().getText();
	}

	public void setText(CharSequence text) {
		getEditText().setText(text);
	}

	public void setTextSize(float size) {
		getEditText().setTextSize(size);
	}

	public void setTextColor(int color) {
		getEditText().setTextColor(color);
	}

	public void setMaxEditTextWidthRatio(float ratio) {
		maxEditTextWidthRatio = ratio;
		validate();
	}

}
