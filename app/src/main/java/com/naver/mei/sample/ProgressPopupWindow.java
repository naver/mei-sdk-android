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
package com.naver.mei.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by tigerbaby on 2016-10-27.
 */

public class ProgressPopupWindow extends PopupWindow {
	private Context context;

	@BindView(R.id.popup_progress_loading_image)
	SimpleDraweeView ivLoadingImage;

	@BindView(R.id.popup_progress_remaining_time)
	TextView tvRemainingTime;

	@BindView(R.id.popup_progress_current_progress)
	TextView tvCurrentProgress;

	public ProgressPopupWindow(Context context) {
		super(context);
		this.context = context;
		initView();
	}

	public void initView() {
		View view = LayoutInflater.from(context).inflate(R.layout.popup_progress, null);
		ButterKnife.bind(this, view);

		ivLoadingImage.setController(Fresco.newDraweeControllerBuilder().setUri("res:///" + R.drawable.animated_loading)
				.setAutoPlayAnimations(true)
				.build());

		setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
		setBackgroundDrawable(new ColorDrawable());
		setContentView(view);
		setFocusable(true);
		setOutsideTouchable(false);
	}

	public void show() {
		showAtLocation(((Activity) context).findViewById(android.R.id.content).getRootView(), Gravity.CENTER, 0, 0);
		showProgressMessage("loading...", 0);
	}

	public void setProgressOfComposition(double progress, long startTime) {
		showProgressMessage("compositing...", progress);
		showRemainingTime(getRemainingTime(progress, startTime));
	}

	public void setProgressOfGifExtraction(double progress, long startTime) {
		showProgressMessage("creating gif from video... ", progress);
		showRemainingTime(getRemainingTime(progress, startTime));
	}

//	public void goPreviewActivityAndFinish(String savedFilePath) {
//		Intent intent = new Intent(context, ImagePreviewActivity.class);
//		intent.putExtra(ImagePreviewActivity.INTENT_PATH_KEY, savedFilePath);
//		context.startActivity(intent);
//		((Activity)context).finish();
//	}

	private void showProgressMessage(final String message, final double progress) {
		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvCurrentProgress.setText(message + "(" + (int) (progress * 100) + "%)");
			}
		});
	}

	private void showRemainingTime(final long remainingTime) {
		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final int remainingSecond = (int) (remainingTime / 1000);
				tvRemainingTime.setText(String.format(Locale.KOREAN, "%02d:%02d:%02d", remainingSecond / 3600, remainingSecond / 60, remainingSecond % 60));
			}
		});
	}

	private long getRemainingTime(double progress, long startTime) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		return (long) (elapsedTime / progress) - elapsedTime;
	}
}
