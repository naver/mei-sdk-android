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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.common.ProgressCallback;
import com.naver.mei.sdk.core.video.VideoToGifParams;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.core.image.compositor.MeiCompositor;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.core.video.CropOptions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.naver.mei.sample.MainActivity.CODE_IMAGES_TO_GIF;

public class ProgressActivity extends AppCompatActivity {
	public static final String INTENT_KEY_META = "meta";
	public static final String INTENT_KEY_VIDEO = "video";
	public static final String INTENT_KEY_VIDEO_CROP = "crop";

	@BindView(R.id.text_progress)
	TextView tvProgress;

	@BindView(R.id.text_remaining_time)
	TextView tvRemainingTime;

	@BindView(R.id.loading_image)
	SimpleDraweeView sdvLoading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_progress);
		ButterKnife.bind(this);
		sdvLoading.setController(Fresco.newDraweeControllerBuilder().setUri("res:///" + R.drawable.animated_loading)
				.setAutoPlayAnimations(true)
				.build());
		Intent intent = getIntent();
		List<Composable> composables = (List<Composable>) intent.getSerializableExtra(INTENT_KEY_META);

		if (composables != null) {
			composite(composables);
		} else {
			VideoToGifParams videoToGifParams = (VideoToGifParams) intent.getSerializableExtra(INTENT_KEY_VIDEO);
			CropOptions cropOptions = (CropOptions) intent.getSerializableExtra(INTENT_KEY_VIDEO_CROP);
			createGifFromVideo(videoToGifParams.setCropOptions(cropOptions));
//			extractFramesFromVideo(videoToGifParams);    // frame만 추출
		}

	}

//	private void extractFramesFromVideo(VideoToGifParams videoToGifParams) {
//		MeiSDK.getFramesFromVideo(videoToGifParams, new MeiFrameListener() {
//			@Override
//			public void onSuccess(List<String> frameFilePaths) {
//			}
//
//			@Override
//			public void onProgress(double progress) {
//				showProgressMessage("Creating GIF from video... ", progress);
//			}
//
//			@Override
//			public void onFail(MeiSDKErrorType errorType) {
//				finish();
//			}
//		});
//	}

	private void createGifFromVideo(VideoToGifParams videoToGifParams) {
		final long startTime = System.currentTimeMillis();

		MeiSDK.videoToGif(videoToGifParams, new MeiEventListener() {
			@Override
			public void onSuccess(String resultFilePath) {
				showProgressMessage("compositing completed.", 1);

				Intent intent = new Intent(ProgressActivity.this, ImagePreviewActivity.class);
				intent.putExtra(ImagePreviewActivity.INTENT_PATH_KEY, resultFilePath);
				startActivity(intent);
				finish();
			}

			@Override
			public void onFail(MeiSDKErrorType meiSDKErrorType) {
				finish();
			}

			@Override
			public void onProgress(double progress) {
				if (progress == 0) return;

				showProgressMessage("Creating GIF from video... ", progress);
				long elapsedTime = System.currentTimeMillis() - startTime;
				long remainingTime = (long)(elapsedTime / progress) - elapsedTime;
				showRemainingTime(remainingTime);
			}
		}, null);
		showProgressMessage("loading...", 0);
	}

	private void composite(final List<Composable> metas) {
		new Thread() {
			@Override
			public void run() {
				try {
					compositionGif(metas);
					finish();
				} catch (Exception ex) {
					Log.e("MEI", "error ", ex);
				}
			}

			private void compositionGif(List<Composable> metas) {
				try {
					final long startTime = System.currentTimeMillis();
					File mediaFile = new File(MeiFileUtils.getUniquePath("mei"));
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mediaFile));
					MeiCompositor.ImageType imageType = MeiCompositor.newBuilder().build(metas.get(0).width, 640).composite(metas, bos, new ProgressCallback() {
						@Override
						public void onProgressComposition(final double progress) {
							showProgressMessage("compositing...", progress);
							if (progress == 0.0) return;

							long elapsedTime = System.currentTimeMillis() - startTime;
							long remainingTime = (long)(elapsedTime / progress) - elapsedTime;
							showRemainingTime(remainingTime);
						}

						@Override
						public void onProgressLoadingResource(double progress) {
							showProgressMessage("image resource loading...", progress);
						}
					});

					MeiFileUtils.changeExtension(mediaFile, imageType.extension);
					Intent intent = new Intent(ProgressActivity.this, ImagePreviewActivity.class);
					intent.putExtra(ImagePreviewActivity.INTENT_PATH_KEY, mediaFile.getAbsolutePath());
					startActivity(intent);
				} catch (Exception ex) {
					Log.e("MEI", "composition error", ex);
					toast("failed to composite.");
					finish();
				}
			}
		}.start();
	}

	private void showProgressMessage(final String message, final double progress) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvProgress.setText(message + "(" + (int) (progress * 100) + "%)");
			}
		});
	}

	private void showRemainingTime(long remainingTime) {
		final int remainingSecond = (int)(remainingTime / 1000);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvRemainingTime.setText(String.format(Locale.KOREAN, "%02d:%02d:%02d", remainingSecond / 3600, remainingSecond / 60, remainingSecond % 60));
			}
		});
	}

	private void toast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ProgressActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;

		switch (requestCode) {
			case CODE_IMAGES_TO_GIF:
				final List<Composable> metas = (List<Composable>) data.getSerializableExtra("metas");
				composite(metas);
				break;
		}
	}

	@Override
	public void onBackPressed() {
		Toast.makeText(this, "prevent finish", Toast.LENGTH_SHORT).show();
	}

}
