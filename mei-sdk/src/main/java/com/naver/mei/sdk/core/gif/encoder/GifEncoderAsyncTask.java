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

package com.naver.mei.sdk.core.gif.encoder;

import android.os.AsyncTask;

import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.io.OutputStream;

/**
 * Created by GTPark on 2017-01-03.
 */

public abstract class GifEncoderAsyncTask extends AsyncTask<Void, Double, Boolean> {
	protected int learnQuality;   // 10-30
	protected int mapQuality;     // 8-6
	protected int delay;  // global frame delay. milliseconds.
	protected OutputStream out;
	protected MeiSDKException exception;
	protected EncodingListener encodingListener;


	public GifEncoderAsyncTask(int learnQuality, int mapQuality, int delay, OutputStream out, EncodingListener encodingListener) {
		this.learnQuality = learnQuality;
		this.mapQuality = mapQuality;
		this.delay = delay;
		this.out = out;
		this.exception = null;
		this.encodingListener = encodingListener;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			AnimatedGifEncoder encoder = new AnimatedGifEncoder();
			encoder.setQuality(learnQuality);
			encoder.setMapQuality(mapQuality);
			encoder.setDelay(delay);
			encoder.setRepeat(0);    // infinity repeat
			encoder.start(out);

			if (!encodeFrames(encoder)) {
				throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_GIF_NO_FRAME);
			}

			encoder.finish();
			IOUtils.closeQuietly(out);

		} catch (MeiSDKException mex) {
			this.exception = mex;
			return false;
		} catch (Exception ex) {
			this.exception = new MeiSDKException(MeiSDKErrorType.FAILED_TO_CREATE_GIF);
			MeiLog.e(ex.getMessage(), ex);
			return false;
		}

		return true;
	}

	abstract protected boolean encodeFrames(AnimatedGifEncoder encoder);

	@Override
	protected void onProgressUpdate(Double... values) {
		encodingListener.onProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean isCompleted) {
		if (isCompleted) {
			encodingListener.onSuccess();
		} else {
			encodingListener.onError(exception);
		}
	}
}
