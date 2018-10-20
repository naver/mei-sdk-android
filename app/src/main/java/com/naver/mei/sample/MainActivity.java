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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.naver.mei.sample.camera.CameraActivity;
import com.naver.mei.sample.util.MeiAlertUtil;
import com.naver.mei.sample.video.VideoToGifActivity;
import com.naver.mei.sdk.core.utils.OsVersionUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 주요 기능 샘플 액티비티간 라우팅을 지원하는 메인 액티비티
 */
public class MainActivity extends Activity {
	public static final int CODE_IMAGES_TO_GIF = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
//		MeiIOUtils.handleStorageSpaceCheck(this, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				finish();
//			}
//		});
	}

	@OnClick(R.id.main_btn_composite)
	void startCompositionActivity() {
		startActivity(CompositeActivity.class);
	}

	@OnClick(R.id.main_btn_images_to_gif)
	void startFramesToGifActivity() {
		startActivity(MultiFrameCompositeActivity.class);
	}

	@OnClick(R.id.main_btn_video_to_gif)
	void startVideoToGifActivity() {
		if (OsVersionUtils.isHigherVersion(Build.VERSION_CODES.JELLY_BEAN)) {
			startActivity(VideoToGifActivity.class);
		} else {
			MeiAlertUtil.show(this, "works at os version 4.1 or higher");
		}
	}

	@OnClick(R.id.main_btn_camera_to_gif)
	void startCameraActivity() {
		startActivity(CameraActivity.class);
	}

	@OnClick(R.id.main_btn_image_crop)
	void startImageCropActivity() {
		startActivity(ImageCropActivity.class);
	}

// for test. not used any more.
//	@OnClick(R.id.btn_image_paths_to_gif)
//	void startImagePathsToGifActivity() {
//		startActivity(ImagePathsToGifActivity.class);
//	}

	private void startActivity(Class targetClass) {
		Intent intent = new Intent(this, targetClass);
		startActivity(intent);
	}
}
