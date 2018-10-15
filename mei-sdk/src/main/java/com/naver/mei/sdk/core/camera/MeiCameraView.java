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
package com.naver.mei.sdk.core.camera;

import android.Manifest;
import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.naver.mei.sdk.R;
import com.naver.mei.sdk.core.utils.MeiPermissionUtils;
import com.naver.mei.sdk.core.utils.PixelUtils;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.naver.mei.sdk.core.camera.CameraRatio.FOUR_BY_THREE_USER;
import static com.naver.mei.sdk.core.camera.CameraRatio.ONE_BY_ONE;
import static com.naver.mei.sdk.core.camera.CameraRatio.THREE_BY_FOUR_USER;

/**
 * Created by Naver on 2016-11-29.
 */

public class MeiCameraView extends RelativeLayout {
	public static final String DEFAULT_DIR = "/mei/";
	private CameraRatio cameraRatio;
	private CameraView cameraView;
	private Context context;
	private FrameLayout cameraViewLayout;
	private View topView;
	private View bottomView;
	private ImageView ivFocusArea;

	public MeiCameraView(Context context) {
		super(context);
		this.context = context;
	}

	public MeiCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public MeiCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.context = context;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public MeiCameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context = context;
	}

	public void init(String dir) {
		if (!MeiPermissionUtils.checkPermission(Manifest.permission.CAMERA)) {
			throw new MeiSDKException(MeiSDKErrorType.NEED_PERMISSION_CAMERA);
		}

		View layout = LayoutInflater.from(context).inflate(R.layout.mei_camera_view, this, true);
		cameraView = new CameraView((Activity) context, "/" + dir + "/");
		cameraViewLayout = (FrameLayout) layout.findViewById(R.id.camera_view);
		cameraViewLayout.addView(cameraView);
		topView = layout.findViewById(R.id.top_view);
		bottomView = layout.findViewById(R.id.bottom_view);
		ivFocusArea = (ImageView) layout.findViewById(R.id.focus_area);
		enableTouchFocus();
	}

	public void init() {
		init(DEFAULT_DIR);
	}

	public void release() {
		cameraView.releaseCamera();
	}

	public void capture(CaptureCallback callback) {
		cameraView.capture(callback);
	}

	/**
	 * 연속된 사진 촬영 지원. 연사 시작
	 * @param callback   사진 촬영 결과를 전달 받을 callback
	 * @param intervalMs 연사 촬영 주기 (millisecond)
	 */
	public void startCapturing(ContinuousCaptureCallback callback, int intervalMs) {
		cameraView.startCapturing(callback, intervalMs);
	}

	/**
	 * 연속된 사진 촬영 지원. 연사 종료
	 */
	public void finishCapturing() {
		cameraView.finishCapturing();
	}

	public void setAutoFocusEnabled(boolean enable) {
		if (enable) {
			setOnTouchListener(null);
			cameraView.enableAutoFocus();
		} else {
			cameraView.disableAutoFocus();
			enableTouchFocus();
		}

	}

	private void enableTouchFocus() {
		setOnTouchListener(new CameraFocusTouchListener(this));
	}

	public void flashOn() {
		cameraView.flashOn();
	}

	public void flashOff() {
		cameraView.flashOff();
	}

	public boolean isFlashOn() {
		return cameraView.isFlashOn();
	}

	public void flashAuto() {
		cameraView.flashAuto();
	}

	public boolean isFlashAuto() {
		return cameraView.isFlashAuto();
	}

	public void switchCamera() {
		cameraView.switchCamera();
	}

	public boolean isFacingFront() {
		return cameraView.isFacingFront();
	}

	public void focusOnTouch(MotionEvent event) {
		cameraView.focusOnTouch(event);

		int areaSize = PixelUtils.dp2px(75);
		animateFocusArea(event.getX() - areaSize / 2, event.getY() - areaSize / 2);
	}

	public void setPictureAspectRatio(CameraRatio cameraRatio) {
		setPictureAspectRatio(cameraRatio, 0, 0);
	}

	public void setPictureAspectRatio(int width, int height) {
		setPictureAspectRatio(null, width, height);
	}

	/**
	 * 사용자의 선택에 따라서 사진의 비율을 조정한다.
	 * cameraRatio 가 null 이면 카메라뷰는 비율에 따라 크기가 조정
	 * null 이 아니라면 카메라의 비율은 디폴트이며 커튼뷰의 크기로 crop
	 *
	 * @param cameraRatio
	 * @param width
	 * @param height
	 */
	private void setPictureAspectRatio(CameraRatio cameraRatio, int width, int height) {
		if (cameraRatio == null) {  // 사용자가 정의한 비율 사용을 안하고 카메라에서 제공하는 w, h로만 카메라 ui 세팅함
			setCameraPictureSize(width, height);
		} else {
			setCurtainViewUI(cameraRatio);
		}
	}

	/**
	 * 카메라 촬영 비율 getter, setter 정의
	 *
	 * @param cameraRatio
	 */
	private void setCameraRatio(CameraRatio cameraRatio) {
		this.cameraRatio = cameraRatio;
	}

	public CameraRatio getCameraRatio() {
		return cameraRatio;
	}

	private void animateFocusArea(float x, float y) {
		ivFocusArea.bringToFront();
		ivFocusArea.setX(x);
		ivFocusArea.setY(y);
		ivFocusArea.setScaleX(1f);
		ivFocusArea.setScaleY(1f);
		ivFocusArea.setVisibility(View.VISIBLE);
		ivFocusArea.animate().scaleX(.8f).scaleY(.8f).setDuration(200).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				postDelayed(new Runnable() {
					@Override
					public void run() {
						ivFocusArea.setVisibility(View.GONE);
					}
				}, 400);
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				ivFocusArea.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}
		});
	}

	/**
	 * 카메라 기본 제공 사진 비율을 오름차순으로 16:9, 4:3을 각 2개씩 지원한다.
	 * 갤럭시 s6 기준 1920*1080(16:9), 1280*720(16:9), 960*720(4:3) 640*480(4"3)
	 * 16: 카메라 높이, 9: 카메라 너비
	 *
	 * @return
	 */
	public List<Camera.Size> getSupportedPictureSizes() {
		List<Camera.Size> sizeList = cameraView.getSupportedPictureSizes();
		Collections.reverse(sizeList);

		List<Camera.Size> sizeList16by9 = new ArrayList<>();
		List<Camera.Size> sizeList4by3 = new ArrayList<>();
		List<Camera.Size> newList = new ArrayList<>();

		for (Camera.Size size : sizeList) {
			float previewRatio = getRound2Ratio((float) size.width / size.height);
			if (previewRatio == CameraRatio.FOUR_BY_THREE_CAM.getValue()) {
				sizeList4by3.add(size);
			} else if (previewRatio == CameraRatio.SIXTEEN_BY_NINE_CAM.getValue()) {
				sizeList16by9.add(size);
			}

			if (sizeList4by3.size() == 2 && sizeList16by9.size() == 2) {
				newList.addAll(sizeList4by3);
				newList.addAll(sizeList16by9);
				break;
			}
		}

		return newList;
	}

	private void setCurtainViewUI(CameraRatio cameraRatio) {
		topView.setVisibility(View.GONE);
		bottomView.setVisibility(View.GONE);

		ViewGroup.LayoutParams params = cameraViewLayout.getLayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = ViewGroup.LayoutParams.MATCH_PARENT;
		cameraViewLayout.setLayoutParams(params);

		ViewGroup.LayoutParams bottomViewParams = bottomView.getLayoutParams();
		ViewGroup.LayoutParams topViewParams = topView.getLayoutParams();

		int widthPx = cameraViewLayout.getWidth();
		int heightPx = cameraViewLayout.getHeight();
		int diff;

		switch (cameraRatio) {
			case THREE_BY_FOUR_USER:
				diff = heightPx - (widthPx * 4 / 3);
				topViewParams.height = 0;
				bottomViewParams.height = diff;
				setCameraRatio(THREE_BY_FOUR_USER);
				break;

			case ONE_BY_ONE:
				diff = heightPx - widthPx;
				topViewParams.height = diff / 2;
				bottomViewParams.height = diff / 2;
				setCameraRatio(ONE_BY_ONE);
				break;

			case FOUR_BY_THREE_USER:
				diff = heightPx - (widthPx * 3 / 4);
				topViewParams.height = diff / 2;
				bottomViewParams.height = diff / 2;
				setCameraRatio(FOUR_BY_THREE_USER);
				break;

			default:
				diff = heightPx - (widthPx * 4 / 3);
				topViewParams.height = 0;
				bottomViewParams.height = diff;
				setCameraRatio(THREE_BY_FOUR_USER);

		}

		topView.setLayoutParams(topViewParams);
		topView.setVisibility(View.VISIBLE);

		bottomView.setLayoutParams(bottomViewParams);
		bottomView.setVisibility(View.VISIBLE);
	}

	private void setCameraPictureSize(int width, int height) {
		cameraView.setCameraPictureSize(width, height);
		Camera.Size bestSize = cameraView.getOptimalPreviewSize(width, height);
		cameraView.setCameraPreviewSize(bestSize.width, bestSize.height);

		float pictureRatio = (float) width / height;

		if (getRound2Ratio(pictureRatio) == CameraRatio.FOUR_BY_THREE_CAM.getValue()) {
			setCameraRatio(CameraRatio.FOUR_BY_THREE_CAM);
		} else {
			setCameraRatio(CameraRatio.SIXTEEN_BY_NINE_CAM);
		}

		topView.setVisibility(View.GONE);
		bottomView.setVisibility(View.GONE);

		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

		ViewGroup.LayoutParams cameraViewLayoutParams = cameraViewLayout.getLayoutParams();
		cameraViewLayoutParams.width = PixelUtils.dp2px(Math.round(dpWidth));
		cameraViewLayoutParams.height = PixelUtils.dp2px(Math.round(dpWidth * pictureRatio));
		cameraViewLayout.setLayoutParams(cameraViewLayoutParams);
	}

	private float getRound2Ratio(float ratio) {
		return Math.round(ratio * 100f) / 100f;
	}

	public void setOnCameraLoadListener(CameraLoadListener cameraLoadListener) {
		cameraView.setOnCameraLoadListener(cameraLoadListener);
	}

}
