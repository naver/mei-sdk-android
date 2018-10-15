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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.naver.mei.sdk.MeiGifEncoder;
import com.naver.mei.sdk.core.gif.encoder.EncodingListener;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiSDKException;

import java.util.List;

public class ImagePathsToGifActivity extends MultiFrameCompositeActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			final List<String> imagePaths = (List<String>)intent.getSerializableExtra(INTENT_KEY_IMAGE_PATHS);
			meiCanvasView.post(new Runnable() {
				@Override
				public void run() {
					imagePathsToGif(imagePaths);
				}
			});
		}
	}

	private void imagePathsToGif(List<String> imagePaths) {
		progressPopupWindow.show();
		final String outputPath = MeiFileUtils.getUniquePath("gif");
		final long startTime = System.currentTimeMillis();

		MeiGifEncoder.newInstance().encodeByImagePaths(imagePaths, outputPath, new EncodingListener() {
			@Override
			public void onSuccess() {
				progressPopupWindow.dismiss();
				Intent intent = new Intent(ImagePathsToGifActivity.this, ImagePreviewActivity.class);
				intent.putExtra(ImagePreviewActivity.INTENT_PATH_KEY, outputPath);
				startActivity(intent);
				finish();
			}

			@Override
			public void onError(MeiSDKException mex) {
				progressPopupWindow.dismiss();
				Toast.makeText(ImagePathsToGifActivity.this, mex.getMessage(), Toast.LENGTH_SHORT).show();
				finish();
			}

			@Override
			public void onProgress(final double progress) {
				if (progressPopupWindow.isShowing()) {
					if (progress == 0.0) return;
					progressPopupWindow.setProgressOfComposition(progress, startTime);
				}
			}
		});
	}
}
