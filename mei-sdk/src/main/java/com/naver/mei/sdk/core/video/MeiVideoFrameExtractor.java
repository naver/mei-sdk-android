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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.naver.mei.sdk.core.gif.encoder.AnimatedGifEncoder;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.util.IOHelper;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.listener.MeiFrameListener;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tigerbaby on 2016-10-19.
 */

public class MeiVideoFrameExtractor extends AsyncTask<Void, Double, Void> {
	private static final int BITMAP_MAX_SIZE = 1000;
	private VideoToGifParams videoToGifParams;
	private MeiEventListener eventListener;
	private MeiFrameListener frameListener;
	private CropOptions cropOptions;
	private String resultFilePath;
	private List<String> frameFilePathList;
	private ExtractorMode extractorMode;
	private byte[] bytesCompositedImage;
	private MediaMetadataRetriever retriever;
	private WatermarkOptions watermarkOptions;

	public MeiVideoFrameExtractor(VideoToGifParams videoToGifParams, MeiEventListener listener) {
		setVideoToGifParams(videoToGifParams);
		setMediaRetreiver();
		this.eventListener = listener;
		this.extractorMode = ExtractorMode.MAKE_GIF;
	}

	public MeiVideoFrameExtractor(VideoToGifParams videoToGifParams, MeiFrameListener listener) {
		setVideoToGifParams(videoToGifParams);
		setMediaRetreiver();
		this.frameListener = listener;
		this.frameFilePathList = new ArrayList<>();
		this.extractorMode = ExtractorMode.EXPORT_FRAMES;
	}

	private void setVideoToGifParams(VideoToGifParams videoToGifParams) {
		this.videoToGifParams = videoToGifParams;
		this.watermarkOptions = videoToGifParams.watermarkOptions;
		this.cropOptions = videoToGifParams.cropOptions;
	}

	private void setMediaRetreiver() {
		this.retriever = new MediaMetadataRetriever();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected Void doInBackground(Void... voids) {
		if (eventListener == null && frameListener == null) {
			throw new MeiSDKException(MeiSDKErrorType.NEED_EVENT_LISTENER);
		}

		long startUs = videoToGifParams.startMs * 1000;
		long endUs = videoToGifParams.endMs * 1000;
		long durationUs = endUs - startUs;
		int delayMs = 1000 / videoToGifParams.fps;
		long intervalUs = delayMs * 1000;
		long totalTime = 0;
		int frameCount = 0;

		MeiLog.d("extract frames for : " + videoToGifParams.startMs + "ms to " + videoToGifParams.endMs + "ms");

		bytesCompositedImage = null;
		AnimatedGifEncoder encoder = null;
		ByteArrayOutputStream byteOutputStream = null;

		MeiVideoDecoder decoder = new MeiVideoDecoder();
		Matrix matrix = new Matrix();

		try {
			retriever.setDataSource(Uri.parse(videoToGifParams.videoUri).toString());
			int rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

//			MeiLog.d("BITMAP ROTATION : " + rotation);

			if (rotation != 0f) {
				matrix.postRotate(rotation);
			}

			if (isMakeGif()) {
				byteOutputStream = new ByteArrayOutputStream();
				encoder = new AnimatedGifEncoder();
				encoder.setQuality(10);
				encoder.setMapQuality(8);
				encoder.setDelay(delayMs);
				encoder.start(byteOutputStream);
			}

			// setting video decoder
			decoder.setDataSource(videoToGifParams.videoUri);
			decoder.setRotation(rotation);
			decoder.start();

			for (long i = startUs; i <= endUs; i += intervalUs) {
				long startTime = System.currentTimeMillis();
				Bitmap bitmap = decoder.getFrame(i);
//				MeiFileUtils.createFileFromBitmap(bitmap);

				if (bitmap == null) {
					MeiLog.d("bitmap is null!!!");
				} else {
					MeiLog.d("extract time : " + (System.currentTimeMillis() - startTime));
					MeiLog.d("bitmap w/h : " + bitmap.getWidth() + "," + bitmap.getHeight());

					if (isMakeGif()) {
						if (cropOptions != null) {
							bitmap = Bitmap.createBitmap(bitmap, cropOptions.getLeft(), cropOptions.getTop(), cropOptions.getWidth(), cropOptions.getHeight());
						}

						bitmap = scalingSize(bitmap);   //avoid out of memory exception.
						MeiLog.d("scaling bitmap w/h : " + bitmap.getWidth() + "," + bitmap.getHeight());

						if (rotation != 0) {
							bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						} else {
							bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
						}

						bitmap = drawWatermark(bitmap);

						long addStartTime = System.currentTimeMillis();
						encoder.addFrame(bitmap);    //microsecond
						long durationTime = System.currentTimeMillis() - addStartTime;
						MeiLog.d("add time : " + durationTime);
						totalTime += durationTime;
						frameCount++;
					} else {
						frameFilePathList.add(MeiFileUtils.createFileFromBitmap(bitmap));
					}
					double progress = (i - startUs) / (double) durationUs;
					publishProgress(progress);
				}
			}

			MeiLog.e("Frame Count : " + frameCount);
			MeiLog.d("time for gif generation : " + totalTime);
		} catch (Exception e) {
			MeiLog.e("get video frame failed. ", e);
		} finally {
			try {
				retriever.release();
				decoder.finish();

				if (isMakeGif()) {
					encoder.finish();
					bytesCompositedImage = byteOutputStream.toByteArray();
					IOUtils.closeQuietly(byteOutputStream);
				}
			} catch (Exception e) {
				// do nothing
			}
		}

		return null;
	}

	private Bitmap drawWatermark(Bitmap backgroundBitmap) {
		if (watermarkOptions == null || watermarkOptions.uri == null) {
			return backgroundBitmap;
		}

		Bitmap watermarkBitmap = IOHelper.getBitmap(watermarkOptions.uri);
		int targetWatermarkWidth = watermarkOptions.width > 0 ? watermarkOptions.width : 0;
		int targetWatermarkHeight = watermarkOptions.height > 0 ? watermarkOptions.height : 0;

		if (targetWatermarkWidth <= 0 && targetWatermarkHeight <= 0) {
			targetWatermarkWidth = watermarkBitmap.getWidth();
			targetWatermarkHeight = watermarkBitmap.getHeight();
		} else if (targetWatermarkWidth <= 0) {
			targetWatermarkWidth = (int) (watermarkBitmap.getWidth() * (watermarkOptions.height / (double) watermarkBitmap.getHeight()));
		} else {
			targetWatermarkHeight = (int) (watermarkBitmap.getHeight() * (watermarkOptions.width / (double) watermarkBitmap.getWidth()));
		}

		watermarkBitmap = MeiImageProcessor.resize(watermarkBitmap, targetWatermarkWidth, targetWatermarkHeight);
		int left = 0;
		int top = 0;

		switch (watermarkOptions.position.xPosition) {
			case LEFT:
				left = watermarkOptions.margin;
				break;
			case RIGHT:
				left = backgroundBitmap.getWidth() - watermarkBitmap.getWidth() - watermarkOptions.margin;
				break;
			case CENTER:
				left = backgroundBitmap.getWidth() / 2 - watermarkBitmap.getWidth() / 2;
				break;
			default:
				left = 0;
		}

		switch (watermarkOptions.position.yPosition) {
			case TOP:
				top = watermarkOptions.margin;
				break;
			case BOTTOM:
				top = backgroundBitmap.getHeight() - watermarkBitmap.getHeight() - watermarkOptions.margin;
				break;
			case MIDDLE:
				top = backgroundBitmap.getHeight() / 2 - watermarkBitmap.getHeight() / 2;
				break;
			default:
				top = 0;
		}

		Canvas canvas = new Canvas(backgroundBitmap);
		canvas.drawBitmap(watermarkBitmap, left, top, null);

		return backgroundBitmap;
	}

	@Override
	protected void onProgressUpdate(Double... values) {
		if (isMakeGif()) {
			eventListener.onProgress(values[0]);
		} else {
			frameListener.onProgress(values[0]);
		}
	}

	@Override
	protected void onPostExecute(Void value) {
		if (isMakeGif()) {
			if (createGifFromBytes(bytesCompositedImage)) {
				eventListener.onSuccess(resultFilePath);
			} else {
				eventListener.onFail(MeiSDKErrorType.FAILED_TO_CREATE_GIF);
			}
		} else {
			if (frameFilePathList.size() > 0) {
				frameListener.onSuccess(frameFilePathList);
			} else {
				frameListener.onFail(MeiSDKErrorType.FAILED_TO_CREATE_GIF);
			}
		}
	}

	private boolean createGifFromBytes(byte[] gifBytesArrray) {
		if (gifBytesArrray == null) {
			return false;
		}

		if (TextUtils.isEmpty(resultFilePath)) {
			resultFilePath = MeiFileUtils.getUniquePath(MeiFileUtils.EXTENSION_GIF);
		}

		return MeiFileUtils.createFileFromBytes(resultFilePath, gifBytesArrray);
	}

	private Bitmap scalingSize(Bitmap bitmap) {
		if (videoToGifParams.targetWidth > 0 && videoToGifParams.targetHeight > 0) {
			return MeiImageProcessor.resize(bitmap, videoToGifParams.targetWidth, videoToGifParams.targetWidth);
		}

		int scaleWidth = bitmap.getWidth();
		int scaleHeight = bitmap.getHeight();

		while (scaleWidth > BITMAP_MAX_SIZE || scaleHeight > BITMAP_MAX_SIZE) {
			scaleWidth /= 2;
			scaleHeight /= 2;
		}

		if (bitmap.getWidth() != scaleWidth || bitmap.getHeight() != scaleHeight) {
			return MeiImageProcessor.resize(bitmap, scaleWidth, scaleHeight);
		}

		return bitmap;
	}

	private boolean isMakeGif() {
		return this.extractorMode == ExtractorMode.MAKE_GIF;
	}

	public MeiVideoFrameExtractor setFrameExtractorMode(boolean mode) {
		this.extractorMode = mode ? ExtractorMode.EXPORT_FRAMES : ExtractorMode.MAKE_GIF;
		return this;
	}

	public MeiVideoFrameExtractor setResultFilePath(String path) {
		this.resultFilePath = path;
		return this;
	}

	private enum ExtractorMode {
		MAKE_GIF,
		EXPORT_FRAMES
	}
}