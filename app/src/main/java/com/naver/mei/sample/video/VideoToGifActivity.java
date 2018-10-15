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
package com.naver.mei.sample.video;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.naver.mei.sample.ProgressActivity;
import com.naver.mei.sample.ProgressPopupWindow;
import com.naver.mei.sample.R;
import com.naver.mei.sample.gallery.GalleryActivity;
import com.naver.mei.sample.gallery.GalleryHelper;
import com.naver.mei.sample.gallery.GalleryItem;
import com.naver.mei.sample.util.MeiAlertUtil;
import com.naver.mei.sample.util.OSHelper;
import com.naver.mei.sample.util.TimeCalculatorUtil;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.core.utils.URIUtils;
import com.naver.mei.sdk.core.video.VideoToGifParams;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoToGifActivity extends Activity {
	private static final String TAG = "VideoToGifActivity";
	private static final int VIDEO_POSITION_MARGIN = 1000;

	@BindView(R.id.video_editor_video_view)
	MeiVideoView videoView;

	@BindView(R.id.video_editor_play_layout)
	FrameLayout videoFrameLayout;

	@BindView(R.id.video_editor_play_button)
	ImageView videoPlayButton;

	@BindView(R.id.video_time_playing)
	TextView videoPlayTimeText;

	@BindView(R.id.video_time_total)
	TextView videoTotalTimeText;

	@BindView(R.id.video_time_duration)
	TextView videoDurationTimeText;

	@BindView(R.id.video_seekbar_start)
	SeekBar videoStartSeekbar;

	@BindView(R.id.video_seekbar_duration)
	SeekBar videoDurationSeekbar;

	@BindView(R.id.video_editor_buffering_progress_bar)
	ProgressBar videoBufferingProgressBar;

	@BindView(R.id.spinner_video_fps)
	Spinner fpsSpinner;

	String originalVideoUri;
	boolean isPrepared = false;
	ProgressPopupWindow progressPopupWindow;
	int videoFps;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);
		ButterKnife.bind(this);

		progressPopupWindow = new ProgressPopupWindow(this);
		fpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String[] fpsInfo = getResources().getStringArray(R.array.fps);
				videoFps = Integer.valueOf(fpsInfo[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		if (MeiIOUtils.isStorageSpaceFull()) return;

		Intent intent = new Intent(this, GalleryActivity.class);
		intent.putExtra(GalleryHelper.MediaSelectionMode.INTENT_PARAM_KEY, GalleryHelper.MediaSelectionMode.VIDEO_ONLY);
		intent.putExtra(GalleryActivity.INTENT_LAUNCH_MODE_KEY, GalleryActivity.LaunchMode.PICK_AND_GET);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MeiIOUtils.handleStorageSpaceCheck(this, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 0 && resultCode == RESULT_OK) {
			GalleryItem galleryItem = (GalleryItem) data.getSerializableExtra(GalleryItem.INTENT_PARAM_KEY);
			originalVideoUri = galleryItem.uri;

			if (!VideoErrorHelper.isAvailableVideo(originalVideoUri)) {
				MeiAlertUtil.show(this, "invalid video", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
			} else {
				initView();
				initEvent();
			}
		} else {
			finish();
		}
	}

	private void initView() {
		videoView.setVideoURI(Uri.parse(originalVideoUri));
		videoView.setOnErrorListener(new VideoErrorHelper.VideoPlayerErrorListener(VideoToGifActivity.this));
	}

	private void initEvent() {
		videoView.setOnPreparedVideoPlayerListener(new MeiVideoView.OnPreparedVideoPlayerListener() {
			@Override
			public void onPreparedVideoPlayer(int totalTimeMillis) {
				if (isPrepared) return;
				int maxDurationTimeInMillis = totalTimeMillis < 10000 ? totalTimeMillis : 10000;

				videoView.setDurationTimeMillis(maxDurationTimeInMillis);
				videoTotalTimeText.setText(String.format("%02d:%02d:%02d", TimeCalculatorUtil.getHoursFromMilliseconds(totalTimeMillis),
						TimeCalculatorUtil.getMinutesFromMilliseconds(totalTimeMillis), TimeCalculatorUtil.getSecondsFromMilliseconds(totalTimeMillis)));
				videoDurationTimeText.setText(String.format("%2d.0 sec", TimeCalculatorUtil.getSecondsFromMilliseconds(maxDurationTimeInMillis)));

				videoStartSeekbar.setMax(totalTimeMillis);
				videoDurationSeekbar.setMax(maxDurationTimeInMillis);
				videoDurationSeekbar.setProgress(maxDurationTimeInMillis);

				isPrepared = true;
			}
		});

		videoView.setOnVideoPlayInfoListener(new MeiVideoView.OnVideoPlayInfoListener() {
			@Override
			public void onVideoPlayInfo(int currentPlayTimeMillis) {
				int secondaryProgress = currentPlayTimeMillis - videoView.getStartTimeMillis();
				secondaryProgress = Math.min(secondaryProgress, videoView.getRemainTimeMillis());
				secondaryProgress = Math.max(secondaryProgress, 0);
				videoDurationSeekbar.setSecondaryProgress(secondaryProgress);
			}

			@Override
			public void onBufferingStart() {
				videoBufferingProgressBar.setVisibility(View.VISIBLE);
			}

			@Override
			public void onBufferingEnd() {
				videoBufferingProgressBar.setVisibility(View.GONE);
			}
		});
		initStartSeekBar();
	}

	private void initStartSeekBar() {
		videoStartSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				handleSeekBarChanged(seekBar);

				// 4.1.2 버전에서 Remote Media 를 플레이할 경우 버퍼링 과정에서 심한 성능저하 발생.
				// 4.1.2 에서는 Progress Changed 발생 시 seekTo를 수행하지 않음
				if (OSHelper.isJellyBean()) return;

				applyStartTimeToVideoView(seekBar);

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// 4.1.2 버전 대응 코드. 성능저하 예방을 위해 seeking 이 시작되면 동영상 정지
				if (OSHelper.isJellyBean()) {
					videoView.pauseVideo();
				}
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// 4.1.2 버전에서 TrackingStop 이 발생할 때 seekTo 수행
				if (OSHelper.isJellyBean()) {
					handleSeekBarChanged(seekBar);
					applyStartTimeToVideoView(seekBar);
					videoView.playVideo();
				}
			}

			private void handleSeekBarChanged(SeekBar startSeekBar) {
				int startTimeMillis = startSeekBar.getProgress();
				if (videoView.getTotalTimeMillis() - startTimeMillis < VIDEO_POSITION_MARGIN) {
					startTimeMillis = videoView.getTotalTimeMillis() - VIDEO_POSITION_MARGIN - 100; // 시작지점 정할때 마지막 1.1초는 남겨놓도록 함
					startSeekBar.setProgress(startTimeMillis);
					return;
				}

				videoPlayTimeText.setText(String.format("%02d:%02d:%02d", TimeCalculatorUtil.getHoursFromMilliseconds(startTimeMillis),
						TimeCalculatorUtil.getMinutesFromMilliseconds(startTimeMillis), TimeCalculatorUtil.getSecondsFromMilliseconds(startTimeMillis)));

				// 4.1.2 대응을 위해 Seekbar의 상태를 즉시 videoView 에 반영하지 않으므로 SeekBar를 통해 remainTime을 계산한다.
				int remainTimeMillis = videoView.getTotalTimeMillis() - startSeekBar.getProgress();
				if (remainTimeMillis < videoDurationSeekbar.getProgress()) {
					videoDurationSeekbar.setProgress(remainTimeMillis);
				}
			}

			private void applyStartTimeToVideoView(SeekBar seekBar) {
				int startTimeMillis = seekBar.getProgress();
				videoView.setStartTimeMillis(startTimeMillis);
				videoView.pause();
				videoView.seekTo(startTimeMillis);
				videoPlayButton.setVisibility(View.GONE);
			}
		});

		videoDurationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int durationTimeMillis, boolean fromUser) {
				int adjustedDurationTimeMillis = durationTimeMillis;

				// minimum duration
				if (adjustedDurationTimeMillis < VIDEO_POSITION_MARGIN) {
					adjustedDurationTimeMillis = VIDEO_POSITION_MARGIN + 100; // Duration 정할때 처음 1.1초는 남겨놓도록 함
				}
				if (adjustedDurationTimeMillis > videoView.getRemainTimeMillis()) {
					adjustedDurationTimeMillis = videoView.getRemainTimeMillis();
				}
				seekBar.setProgress(adjustedDurationTimeMillis);

				videoView.setDurationTimeMillis(adjustedDurationTimeMillis);
				videoDurationTimeText.setText(String.format("%2d.0 sec", TimeCalculatorUtil.getSecondsFromMilliseconds(adjustedDurationTimeMillis)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
	}


	@OnClick({R.id.video_editor_play_layout, R.id.video_editor_play_button})
	void playAndPauseVideo() {
		boolean isPlaying = videoView.toggleVideo();
		videoPlayButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
	}

	@OnClick(R.id.btn_video_to_gif)
	void createGif() {
		VideoToGifParams metas = new VideoToGifParams(URIUtils.uriStrToPath(originalVideoUri), videoView.getStartTimeMillis(), videoView.getStartTimeMillis() + videoView.getDurationTimeMillis(), videoFps);
		Intent intent = new Intent(VideoToGifActivity.this, ProgressActivity.class);
		intent.putExtra(ProgressActivity.INTENT_KEY_VIDEO, metas);
		startActivity(intent);
		finish();
	}
}
