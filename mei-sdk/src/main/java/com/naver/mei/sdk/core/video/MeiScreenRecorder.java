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
package com.naver.mei.sdk.core.video;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.naver.mei.sdk.MeiGifEncoder;
import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.gif.encoder.EncodingListener;
import com.naver.mei.sdk.core.gif.encoder.GifEncodingOptions;
import com.naver.mei.sdk.core.gif.encoder.GifQueuingEncodable;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.listener.MeiQueuingEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by tigerbaby on 2016-11-25.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MeiScreenRecorder {
	private static final String TAG = "MeiScreenRecorder";
	public static final int RECORDING_REQUEST_CODE = 333;
	private static final int VIDEO_BUFFER_MS = 100;
	private static final long NANO_SECOND = 1000000000L;

	private WindowManager windowManager;
	private MediaProjectionManager mediaProjectionManager;
	private MediaProjection mediaProjection;
	private VirtualDisplay virtualDisplay;
	private CamcorderProfile camcorderProfile;
	private Activity callerActivity;
	private ImageReader imageReader;
	private View targetView;

	private int screenWidth;
	private int screenHeight;
	private int screenDensity;
	private WatermarkOptions watermarkOptions;

	private double widthRatio;
	private double heightRatio;

	private MeiQueuingEventListener eventListener;
	private CropOptions cropOptions;
	private long captureTimeStampInNanoSec;
	private ArrayList<String> imageFilePathList;
	private HandlerThread handlerThread;
	private Handler captureHandler;
	private GifQueuingEncodable encodable;
	private GifEncodingOptions gifEncodingOptions;
	private int fps;

	public MeiScreenRecorder(Activity activity, View targetView) {
		if (!validateOSVersion()) {
			throw new MeiSDKException(MeiSDKErrorType.NOT_AVAILABLE_OS_VERSION_FOR_RECORDING);
		}

		this.callerActivity = activity;
		this.targetView = targetView;

		this.imageFilePathList = new ArrayList<>();
		this.captureTimeStampInNanoSec = 0;
		this.gifEncodingOptions = GifEncodingOptions.asDefault();

		setScreenSize();
		initMediaProjection();
		checkPermission();

//		this.camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//		MeiLog.d("Screen width: " + screenWidth + " / Screen Height : " + screenHeight);
//		initMediaRecorder();
	}

	private void setScreenSize() {
		this.windowManager = (WindowManager) MeiSDK.getContext().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		this.windowManager.getDefaultDisplay().getMetrics(metrics);
		this.screenDensity = metrics.densityDpi;

		Display display = windowManager.getDefaultDisplay();
		Point screenSize = new Point();
		display.getRealSize(screenSize);
		this.screenWidth = screenSize.x;
		this.screenHeight = screenSize.y;
	}

	public void notifyRecordingActionToUser() {
		callerActivity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), RECORDING_REQUEST_CODE);
	}

	public void start(Intent resultData, int fps, MeiQueuingEventListener meiQueuingEventListener) {
		if (fps > 10) {
			throw new MeiSDKException(MeiSDKErrorType.VIDEO_TO_GIF_FPS_CANNOT_EXCEED_10);
		}

		this.fps = fps;
		this.eventListener = meiQueuingEventListener;

		mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, resultData);
		cropOptions = new CropOptions(targetView, widthRatio, heightRatio);

		createCaptureHandler();
		createVirtualDisplay(resultData);
	}

	public void createCaptureHandler() {
		handlerThread = new HandlerThread(TAG);
		handlerThread.start();
		captureHandler = new Handler(handlerThread.getLooper());
	}

	public void stop() {
		encodable.stop();
	}

	private void stopRecording() {
		mediaProjection.stop();
		virtualDisplay.release();
		handlerThread.quitSafely();
	};

	public GifEncodingOptions getGifEncodingOptions() {
		return gifEncodingOptions;
	}

	public void setWatermark(String watermarkUri, WatermarkPosition position, int margin) {
		this.watermarkOptions = new WatermarkOptions(watermarkUri, position, margin);
	}

	public void setWatermark(String watermarkUri, int width, int height, WatermarkPosition position, int margin) {
		this.watermarkOptions = new WatermarkOptions(watermarkUri, width, height, position, margin);
	}

	private void initMediaProjection() {
		mediaProjectionManager = (MediaProjectionManager) MeiSDK.getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
	}

	private void createVirtualDisplay(Intent resultData) {
		FileOutputStream fileOutputStream = null;

		final String outputFilePath = MeiFileUtils.getUniquePath(MeiFileUtils.EXTENSION_GIF);

		try {
			fileOutputStream = new FileOutputStream(outputFilePath);
		} catch (Exception ex) {
		}

		encodable = MeiGifEncoder.newInstance()
				.setQuality(gifEncodingOptions.getQuality())
				.setColorLevel(gifEncodingOptions.getColorLevel())
				.setDelay(1000 / fps)
				.encodeWithQueuing(fileOutputStream, new EncodingListener() {
					@Override
					public void onSuccess() {
						eventListener.onSuccess(outputFilePath);
					}

					@Override
					public void onFrameProgress(int current, int total) {
						eventListener.onFrameProgress(current, total);
					}

					@Override
					public void onError(MeiSDKException mex) {
						eventListener.onFail(mex.getErrorType());
					}

					@Override
					public void onStop(int totalFrameCount) {
						stopRecording();
						eventListener.onStop(totalFrameCount);
					}
				});

		imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);

		virtualDisplay = mediaProjection.createVirtualDisplay("MeiSDK Screen Recorder", screenWidth, screenHeight, screenDensity,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
				imageReader.getSurface(), null, captureHandler);

		imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
			@Override
			public void onImageAvailable(ImageReader reader) {
				captureImage();
			}
		}, captureHandler);
	}

	private void checkPermission() {
		if (ContextCompat.checkSelfPermission(MeiSDK.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			throw new MeiSDKException(MeiSDKErrorType.NEED_PERMISSION_WRITE_EXTERNAL_STORAGE);
		}
	}

	private boolean validateOSVersion() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	private void captureImage() {
		Image image = null;
		FileOutputStream fileOutputStream = null;
		Bitmap bitmap = null;

		try {
			image = imageReader.acquireLatestImage();
			long nanoInterval = NANO_SECOND / fps;
			long pastedTime = image.getTimestamp() - captureTimeStampInNanoSec;

			if (image != null && pastedTime >= nanoInterval) {
				captureTimeStampInNanoSec = image.getTimestamp();

				//crop to fit target view size
				bitmap = Bitmap.createBitmap(getBitmapFromImageReader(image), cropOptions.getLeft(), cropOptions.getTop(), cropOptions.getWidth(), cropOptions.getHeight());

				if (bitmap != null) {
					if (watermarkOptions != null) {
						bitmap = WatermarkHelper.drawWatermark(bitmap, watermarkOptions);
					}

					String path = MeiFileUtils.getTemporaryUniquePath(MeiFileUtils.EXTENSION_JPG);
					fileOutputStream = new FileOutputStream(path);
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
					encodable.addFrame(new File(path));
				}
			}
		} catch (Exception e) {
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException ioe) {
				}
			}

			if (bitmap != null) {
				bitmap.recycle();
			}

			if (image != null) {
				image.close();
			}
		}
	}

	private Bitmap getBitmapFromImageReader(Image image) {
		Image.Plane[] planes = image.getPlanes();

		if (planes[0].getBuffer() == null) {
			return null;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int pixelStride = planes[0].getPixelStride();
		int rowStride = planes[0].getRowStride();

		ByteBuffer croppedBuffer = ByteBuffer.allocate(width * height * pixelStride);
		ByteBuffer buffer = planes[0].getBuffer();
		byte imageArray[] = new byte[buffer.remaining()];
		buffer.get(imageArray);

		int offset = 0;

		for (int i = 0; i < height; ++i) {
			croppedBuffer.put(imageArray, offset, width * pixelStride);
			offset += rowStride;
		}

		croppedBuffer.rewind();
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.copyPixelsFromBuffer(croppedBuffer);

		return bitmap;
	}
}