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
package com.naver.mei.sdk.core.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by GTPark on 2016-03-31.
 */
abstract public class SoftKeyboardHelper {
	boolean isOpened = false;

	public SoftKeyboardHelper(Window window) {
		final View activityRootView = window.getDecorView().findViewById(android.R.id.content);
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int rootHeight = activityRootView.getRootView().getHeight();
				int viewHeight = activityRootView.getHeight();
				int heightDiff = rootHeight - viewHeight;
				if (heightDiff > (rootHeight * 0.15)) { // root height와 view height간의 차이가 15% 이상 나는 경우 (통상 키보드가 뜨는 경우는 50%)
					onKeyboardChangeEvent(true);

					if (isOpened == false) {
						//Do two things, make the view top visible and the editText smaller
					}
					isOpened = true;
				} else if (isOpened == true) {
					onKeyboardChangeEvent(false);
					isOpened = false;
				}
			}
		});
	}

	public static void showKeyboard(Context context, EditText targetEditText) {
		showKeyboard(context, targetEditText, true);
	}

	public static void showKeyboard(Context context, EditText targetEditText, boolean isCursorPositionEnd) {
		targetEditText.requestFocus();
		if (isCursorPositionEnd) {
			targetEditText.setSelection(targetEditText.getText().length());
		}
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(targetEditText, InputMethodManager.SHOW_IMPLICIT);
	}

	public static void hideKeyboard(Context context, View targetView) {
		targetView.clearFocus();
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(targetView.getWindowToken(), 0);
	}

	abstract public void onKeyboardChangeEvent(boolean isVisible);
}
