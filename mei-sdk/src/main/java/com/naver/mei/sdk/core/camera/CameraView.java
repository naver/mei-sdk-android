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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Naver on 2016-11-07.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
	public static final int BITMAP_STANDARD_SIZE_500 = 500;
	private static final String DEFAULT_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
	private static final String TEMP_STORAGE = "temp/";
	private static String dir;
	private Activity activity;
	private SurfaceHolder holder;

	private Camera camera;
	private Camera.Size previewSize;
	private CameraRatio cameraRatio;
	private CameraLoadListener cameraLoadListener;
	private ContinuousCaptureCallback continuousCaptureCallback;
	private Timer captureTimer;
	private TimerTask captureTimerTask;
	private HashMap<Integer, String> captureImageFileMap = new HashMap<>();
	private int continuousCaptureSequence;
	private boolean finishContinuousCapturing;

	private int width;
	private int height;
	private int degree;
	private int currentCameraId = 0;

	private static final int FOCUS_AREA_SIZE = 300;
	private Matrix matrix;

	public CameraView(Activity activity, String dir) {
		super(activity);
		this.activity = activity;
		this.dir = dir;
		holder = getHolder();
		holder.addCallback(this);
	}

	private void setPreviewCallback(final CaptureCallback callback) {
		camera.setPreviewCallback(new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				camera.setPreviewCallback(null);
				new CameraCaptureAsyncTask(callback).execute(data);
			}
		});
	}

	private void setPreviewCallback(final int sequence) {
		camera.setPreviewCallback(new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				MeiLog.d("onPrevieFrame Called");
				camera.setPreviewCallback(null);
				new CameraCaptureAsyncTask(sequence, continuousCaptureCallback).execute(data);
			}
		});
	}

	public void setCameraRatio(CameraRatio cameraRatio) {
		this.cameraRatio = cameraRatio;
	}

	public void capture(CaptureCallback callback) {
		setPreviewCallback(callback);
	}

	public void startCapturing(final ContinuousCaptureCallback callback, int intervalMs) {
		continuousCaptureSequence = 0;
		captureImageFileMap.clear();
		continuousCaptureCallback = callback;

		captureTimerTask = new TimerTask() {
			@Override
			public void run() {
				setPreviewCallback(++continuousCaptureSequence);
			}
		};

		captureTimer = new Timer();
		captureTimer.schedule(captureTimerTask, 0, intervalMs);
	}

	public void finishCapturing() {
		captureTimer.cancel();
		captureTimer = null;
		captureTimerTask = null;

		if (captureImageFileMap.size() == continuousCaptureSequence) {
			sendResultToContinousCallback();
		} else {
			finishContinuousCapturing = true;
		}
	}

	private void sendResultToContinousCallback() {
		continuousCaptureCallback.onFinish(new ArrayList<>(captureImageFileMap.values()));
	}

	public void setOnCameraLoadListener(CameraLoadListener cameraLoadListener) {
		this.cameraLoadListener = cameraLoadListener;
	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		initCamera();
	}

	public void initCamera() {
		camera = Camera.open();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		try {
			camera.setPreviewDisplay(holder);
			previewSize = getOptimalPreviewSize(width, height);

			if (cameraLoadListener != null) {
				cameraLoadListener.onCameraOpenComplete();
			}
		} catch (Exception e) {
			releaseCamera();
		}
		matrix = new Matrix();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		previewSize = getOptimalPreviewSize(width, height);
		Camera.Parameters parameters = camera.getParameters();

		setCameraDisplayOrientation(activity, currentCameraId);
		parameters.setRotation(degree);

//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

		parameters.setPreviewSize(previewSize.width, previewSize.height);
		camera.setParameters(parameters);

		camera.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseCamera();
	}

	public void releaseCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			getHolder().removeCallback(this);
			camera.release();
			camera = null;
		}
	}

	public void enableAutoFocus() {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		camera.setParameters(parameters);
	}

	public void disableAutoFocus() {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		camera.setParameters(parameters);
	}

	public void flashOn() {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		camera.setParameters(parameters);
	}

	public void flashOff() {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
	}

	public boolean isFlashOn() {
		Camera.Parameters parameters = camera.getParameters();
		String flashMode = parameters.getFlashMode();

		if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
			return true;
		} else {
			return false;
		}
	}

	public void flashAuto() {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
		camera.setParameters(parameters);
	}

	public boolean isFlashAuto() {
		Camera.Parameters parameters = camera.getParameters();
		String flashMode = parameters.getFlashMode();

		if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
			return true;
		} else {
			return false;
		}
	}

	public void focusOnTouch(MotionEvent event) {
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			if (parameters.getMaxNumMeteringAreas() > 0) {
				Rect rect = calculateTapArea(event.getX(), event.getY(), 1f);

				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				List<Camera.Area> meteringAreas = new ArrayList<>();
				meteringAreas.add(new Camera.Area(rect, 1000));
				parameters.setFocusAreas(meteringAreas);

				camera.setParameters(parameters);
				camera.autoFocus(this);
			} else {
				camera.autoFocus(this);
			}
		}
	}

	private Rect calculateTapArea(float x, float y, float coefficient) {
		int areaSize = Float.valueOf(FOCUS_AREA_SIZE * coefficient).intValue();

		int left = clamp((int) x - areaSize / 2, 0, getWidth() - areaSize);
		int top = clamp((int) y - areaSize / 2, 0, getHeight() - areaSize);

		RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
		matrix.mapRect(rectF);

		return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
	}

	private int clamp(int x, int min, int max) {
		if (x > max) {
			return max;
		}
		if (x < min) {
			return min;
		}
		return x;
	}

	public void switchCamera() {
		releaseCamera();

		if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
			currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
		} else {
			currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		camera = Camera.open(currentCameraId);

		setCameraPreviewSize(previewSize.width, previewSize.height);
		setCameraDisplayOrientation(activity, currentCameraId);

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
		}

		camera.startPreview();
	}

	public boolean isFacingFront() {
		if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onAutoFocus(boolean b, Camera camera) {
	}

	public class CameraCaptureAsyncTask extends AsyncTask<byte[], Void, String> {
		private BaseCaptureCallback captureCallback;
		private int captureSequence;

		public CameraCaptureAsyncTask(BaseCaptureCallback captureCallback) {
			this.captureCallback = captureCallback;
		}

		public CameraCaptureAsyncTask(int sequence, BaseCaptureCallback captureCallback) {
			this.captureSequence = sequence;
			this.captureCallback = captureCallback;
		}

		@Override
		protected String doInBackground(byte[]... datas) {
			File pictureFileDir = new File(DEFAULT_PATH + dir + TEMP_STORAGE);

			if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
				MeiLog.e("Can't create directory to save image. dir : " + pictureFileDir.getAbsolutePath());
				return null;
			}

			String fileName = MeiFileUtils.getTemporaryUniquePath(MeiFileUtils.EXTENSION_JPG);
			File resultImageFile = new File(fileName);

			try {
				//width, height 설정
				int width = camera.getParameters().getPreviewSize().width;
				int height = camera.getParameters().getPreviewSize().height;

				YuvImage yuvImage = new YuvImage(datas[0], camera.getParameters().getPreviewFormat(), width, height, null);

				ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
				yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, byteOutputStream);

				byte[] data = byteOutputStream.toByteArray();
				FileOutputStream fos = new FileOutputStream(resultImageFile);

				BitmapFactory.Options boundsOptions = MeiImageProcessor.getImageBoundsOptions(data);
				Bitmap rotateBitmap = MeiImageProcessor.rotate(MeiImageProcessor.decodeAndResize(data, BITMAP_STANDARD_SIZE_500, BITMAP_STANDARD_SIZE_500 * boundsOptions.outHeight / boundsOptions.outWidth), degree);

				int pxDiff = 0;
				if (cameraRatio != null) {
					pxDiff = Math.round(rotateBitmap.getWidth() * cameraRatio.getValue());
				}

				Bitmap croppedBmp;

				if (cameraRatio == CameraRatio.THREE_BY_FOUR_USER) {
					croppedBmp = Bitmap.createBitmap(rotateBitmap, 0, 0, rotateBitmap.getWidth(), pxDiff);
				} else if (cameraRatio == CameraRatio.FOUR_BY_THREE_USER || cameraRatio == CameraRatio.ONE_BY_ONE) {
					croppedBmp = cropCenterBitmap(rotateBitmap, rotateBitmap.getWidth(), pxDiff);
				} else if (cameraRatio == CameraRatio.SIXTEEN_BY_NINE_CAM || cameraRatio == CameraRatio.FOUR_BY_THREE_CAM) {
					croppedBmp = Bitmap.createBitmap(rotateBitmap, 0, 0, rotateBitmap.getWidth(), rotateBitmap.getHeight());
				} else {
					croppedBmp = Bitmap.createBitmap(rotateBitmap, 0, 0, rotateBitmap.getWidth(), rotateBitmap.getHeight());
				}

				croppedBmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.close();
				return fileName;
			} catch (Exception error) {
				MeiLog.e("file not saved(" + resultImageFile.getAbsolutePath() + ")" + error.getMessage());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String filePath) {
			if (TextUtils.isEmpty(filePath)) {
				captureCallback.onFail(MeiSDKErrorType.FAILED_TO_SAVE_IMAGE);
			} else {
				if (captureCallback instanceof CaptureCallback) {
					((CaptureCallback) captureCallback).onSave(filePath);
				} else {
					captureImageFileMap.put(captureSequence, filePath);
					((ContinuousCaptureCallback) captureCallback).onSaving(captureSequence, filePath);

					if (finishContinuousCapturing && captureImageFileMap.size() == continuousCaptureSequence) {
						sendResultToContinousCallback();
					}
				}
			}
		}
	}

	public Camera.Size getOptimalPreviewSize(int w, int h) {
		Camera.Parameters parameters = camera.getParameters();
		List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) h / w;

		if (sizes == null) return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	private void setCameraDisplayOrientation(Activity activity, int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;
		} else {
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
		degree = result;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	public void setCameraPreviewSize(int width, int height) {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(width, height);
		camera.setParameters(parameters);
	}

	public void setCameraPictureSize(int width, int height) {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPictureSize(width, height);
		camera.setParameters(parameters);
	}

	public List<Camera.Size> getSupportedPictureSizes() {
		if (camera == null) {
			return null;
		}

		List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
		return pictureSizes;
	}

	/**
	 * Bitmap 이미지를 가운데를 기준으로 w, h 크기 만큼 crop한다.
	 *
	 * @param src 원본
	 * @param w   넓이
	 * @param h   높이
	 * @return
	 */
	public static Bitmap cropCenterBitmap(Bitmap src, int w, int h) {
		if (src == null)
			return null;

		int width = src.getWidth();
		int height = src.getHeight();

		if (width < w && height < h)
			return src;

		int x = 0;
		int y = 0;

		if (width > w)
			x = (width - w) / 2;

		if (height > h)
			y = (height - h) / 2;

		int cw = w; // crop width
		int ch = h; // crop height

		if (w > width)
			cw = width;

		if (h > height)
			ch = height;

		return Bitmap.createBitmap(src, x, y, cw, ch);
	}

}

