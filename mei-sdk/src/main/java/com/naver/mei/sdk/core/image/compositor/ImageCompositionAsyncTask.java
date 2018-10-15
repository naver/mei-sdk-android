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
package com.naver.mei.sdk.core.image.compositor;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.naver.mei.sdk.core.common.ProgressCallback;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.core.utils.MeiFileUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by tigerbaby on 2016-10-28.
 */

public class ImageCompositionAsyncTask extends AsyncTask<Void, Double, Boolean> {
	private List<Composable> composables;
	private MeiEventListener eventListener;
	private String savedFilePath;
	private int outputWidth;
	private double speedRatio;

	public ImageCompositionAsyncTask(List<Composable> composables, MeiEventListener eventListener, String savedFilePath, int outputWidth, double speedRatio) {
		this.composables = composables;
		this.eventListener = eventListener;
		this.savedFilePath = savedFilePath;
		this.outputWidth = outputWidth;
		this.speedRatio = speedRatio;

		if (TextUtils.isEmpty(savedFilePath)) {
			this.savedFilePath = MeiFileUtils.getUniquePath(MeiFileUtils.EXTENSION_GIF);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		FileOutputStream fileOutputStream = null;
		File mediaFile = new File(savedFilePath);

		try {
			fileOutputStream = new FileOutputStream(mediaFile);

			int screenWidth = composables.get(0).width;
			MeiCompositor.ImageType imageType = MeiCompositor.newBuilder()
					.build(screenWidth, outputWidth)
					.speedRatio(speedRatio)
					.composite(composables, fileOutputStream, new ProgressCallback() {
						@Override
						public void onProgressComposition(double progress) {
							publishProgress(progress);
						}

						@Override
						public void onProgressLoadingResource(double progress) {
						}
					});
			this.savedFilePath = MeiFileUtils.changeExtension(mediaFile, imageType.extension);
			MeiFileUtils.broadcastNewMediaAdded(Uri.fromFile(new File(savedFilePath)));

			return true;

		} catch (Exception ex) {
			MeiLog.e("image composition error : " + ex.getMessage(), ex);
			mediaFile.delete();
			return false;
		} finally {
			IOUtils.closeQuietly(fileOutputStream);
		}
	}

	@Override
	protected void onProgressUpdate(Double... values) {
		eventListener.onProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			eventListener.onSuccess(savedFilePath);
		} else {
			eventListener.onFail(MeiSDKErrorType.FAILED_TO_COMPOSITE_IMAGE);
		}
	}
}
