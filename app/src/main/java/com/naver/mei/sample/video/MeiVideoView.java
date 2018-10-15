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
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.naver.mei.sample.util.OSHelper;

import java.util.Timer;

/**
 * video의 가로/세로 중 더 큰 높이를 찾아서 한쪽을 맞추어 주는 View.
 * 사용할때 xml의 width/height를 MATCH_PARENT로 선언해야 함.
 * Created by tigerbaby on 2016-05-03.
 * <p/>
 * additional by GTPark on 2016-06-08.
 * 구간 반복을 지원하는 MeiVideoView
 * OS 버전별로 비디오 플레이어와 관련된 다양한 버그가 존재하며 이에 대한 대응 코드 일원화
 * 비디오 플레이와 관련된 콜백 명시적 지원
 */
public class MeiVideoView extends VideoView {
	private static final int VIDEO_POSITION_MARGIN = 1000;
	private Uri videoURI;

	// 아래 field 들은 function call overhead 를 줄이기 위해 public 으로 변경할 가치가 있다.
	private int totalTimeMillis;            // 총 동영상 시간
	private int startTimeMillis = 0;        // 사용자가 설정한 동영상 재생 시작 시간
	private int durationTimeMillis = 0;     // 사용자가 설정한 동영상 재생 끝 시간.

	private Timer videoPlayTimer;

	private int mVideoWidth;
	private int mVideoHeight;

	private OnVideoPlayInfoListener onVideoPlayInfoListener;
	private OnPreparedVideoPlayerListener onPreparedVideoPlayerListener;

	public interface OnVideoPlayInfoListener {
		void onVideoPlayInfo(int currentPlayTimeMillis);

		void onBufferingStart();

		void onBufferingEnd();
	}

	public interface OnPreparedVideoPlayerListener {
		void onPreparedVideoPlayer(int totalTimeMillis);
	}

	private Handler videoRepeatHandler = new VideoRepeatHandler();

	public MeiVideoView(Context context) {
		super(context);
		init();
	}

	public MeiVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MeiVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private class VideoRepeatHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			int currentPlayTime = getCurrentPosition();
			// position boundary over
			if (currentPlayTime >= startTimeMillis + durationTimeMillis
					// 비정상적으로 Position 이 0으로 이동하는 경우. 넉넉한 마진을 바탕으로 강제로 원래 포지션으로 이동시킨다.
					// 5.1.1, 4.1.2, G4_6.0.1
					|| (currentPlayTime < startTimeMillis - VIDEO_POSITION_MARGIN * 2 && currentPlayTime < VIDEO_POSITION_MARGIN)) {
				pause();
				seekTo(startTimeMillis);
			}

			if (onVideoPlayInfoListener != null) {
				onVideoPlayInfoListener.onVideoPlayInfo(currentPlayTime);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if (mVideoWidth > 0 && mVideoHeight > 0) {
			if (mVideoWidth * height > width * mVideoHeight) {
				height = width * mVideoHeight / mVideoWidth;    //fit width to screen size
			} else if (mVideoWidth * height < width * mVideoHeight) {
				width = height * mVideoWidth / mVideoHeight;    //fit height to screen size
			} else {
				// do nothing.
			}
		}
		setMeasuredDimension(width, height);
	}

	public void setVideoSize(int width, int height) {
		mVideoWidth = width;
		mVideoHeight = height;
	}

	private void init() {
		setOnErrorListener(new VideoErrorHelper.VideoPlayerErrorListener((Activity) getContext()));
		setOnPreparedListener(createPrepareListener());
		setOnCompletionListener(createOnCompletionListener());

		this.setKeepScreenOn(true);
	}

	private void initVideoSize() {
		ViewGroup.LayoutParams params = this.getLayoutParams();
		params.width = 1;
		params.height = 1;
		this.setLayoutParams(params);
	}

	private MediaPlayer.OnPreparedListener createPrepareListener() {
		return new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				MeiVideoView.this.setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.CENTER;
				MeiVideoView.this.setLayoutParams(params);

				mp.setVolume(0f, 0f);
				totalTimeMillis = getDuration();
				durationTimeMillis = durationTimeMillis <= 0 ? totalTimeMillis : durationTimeMillis;
				if (onPreparedVideoPlayerListener != null)
					onPreparedVideoPlayerListener.onPreparedVideoPlayer(totalTimeMillis);

				mp.setOnSeekCompleteListener(createOnSeekCompleteListener());
				playVideo();
				mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
					@Override
					public boolean onInfo(MediaPlayer mp, int what, int extra) {

						String whatText = "";
						switch (what) {
//							case 1:
//								whatText = "MEDIA_INFO_UNKNOWN";
//								break;
//							case 2:
//								whatText = "MEDIA_INFO_STARTED_AS_NEXT";
//								break;
//							case 3:
//								whatText = "MEDIA_INFO_VIDEO_RENDERING_START";
//								break;
//							case 700:
//								whatText = "MEDIA_INFO_VIDEO_TRACK_LAGGING";
//								break;
							case MediaPlayer.MEDIA_INFO_BUFFERING_START:
								whatText = "MEDIA_INFO_BUFFERING_START";
								if (onVideoPlayInfoListener != null)
									onVideoPlayInfoListener.onBufferingStart();
								break;
							case MediaPlayer.MEDIA_INFO_BUFFERING_END:
								whatText = "MEDIA_INFO_BUFFERING_END";
								if (onVideoPlayInfoListener != null)
									onVideoPlayInfoListener.onBufferingEnd();
								break;
//							case 703:
//								whatText = "MEDIA_INFO_NETWORK_BANDWIDTH";
//								break;
//							case 800:
//								whatText = "MEDIA_INFO_BAD_INTERLEAVING";
//								break;
//							case 801:
//								whatText = "MEDIA_INFO_NOT_SEEKABLE";
//								break;
//							case 802:
//								whatText = "MEDIA_INFO_METADATA_UPDATE";
//								break;
//							case 803:
//								whatText = "MEDIA_INFO_EXTERNAL_METADATA_UPDATE";
//								break;
//							case 900:
//								whatText = "MEDIA_INFO_TIMED_TEXT_ERROR";
//								break;
//							case 901:
//								whatText = "MEDIA_INFO_UNSUPPORTED_SUBTITLE";
//								break;
//							case 902:
//								whatText = "MEDIA_INFO_SUBTITLE_TIMED_OUT";
//								break;
						}

						return false;
					}
				});
			}
		};
	}

	private MediaPlayer.OnSeekCompleteListener createOnSeekCompleteListener() {
		return new MediaPlayer.OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				int currentPosition = MeiVideoView.this.getCurrentPosition();
				if (currentPosition < startTimeMillis - VIDEO_POSITION_MARGIN * 2) {    // seekTo가 의도치 않게 지나치게 앞으로 이동한 경우를 위한 보정
					pause();
					seekTo(startTimeMillis);
					return;
				}

				start();
			}
		};
	}

	private MediaPlayer.OnCompletionListener createOnCompletionListener() {
		return new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// Samsung Gallexy S3 4.1.2 버전에서 MP가 Play completion 상태로 전이되면 seekTo 가 정상 동작하지 않는 버그가 존재
				// seekTo가 정상적으로 적용되지 않는다. 따라서, onSeekComplete는 반복 호출되지만, 정작 this 의 position 은 그대로인 상태다.
				// http://developer.samsung.com/forum/board/thread/view.do?boardName=GeneralBKor&messageId=224702
				if (OSHelper.isJellyBean()) {
					stopPlayback();
					setVideoURI(videoURI);
					if (onVideoPlayInfoListener != null)
						onVideoPlayInfoListener.onBufferingStart();
				}
				pause();
				seekTo(startTimeMillis);

				// 논리적으로 start 는 명시적으로 호출하지 않아도 되지만, S6 5.1.1 에서의 버그를 해결하기 위해 삽입한다. 4.1.2와 유사한 버그가 발생된다.
				playVideo();
			}
		};
	}

	public boolean toggleVideo() {
		boolean isPlaying = isPlaying();
		if (isPlaying) {
			pauseVideo();
		} else {
			playVideo();
		}
		return !isPlaying;
	}

	public void playVideo() {
		start();
		runVideoRepeatHandler();
	}

	public void playVideo(int startTimeMillis) {
		requestFocus();
		seekTo(startTimeMillis);
		playVideo();
	}

	public void pauseVideo() {
		pause();
		stopVideoRepeatHandler();
	}

	private void runVideoRepeatHandler() {
		if (videoPlayTimer != null) return;

		videoPlayTimer = new Timer();
		videoPlayTimer.schedule(new VideoTimerTask(videoRepeatHandler), 100, 100);
	}

	private void stopVideoRepeatHandler() {
		if (videoPlayTimer == null) return;

		videoPlayTimer.cancel();
		videoPlayTimer.purge();
		videoPlayTimer = null;
	}

	public Uri getVideoUri() {
		return videoURI;
	}

	@Override
	public void setVideoURI(Uri videoURI) {
		this.videoURI = videoURI;
		super.setVideoURI(videoURI);
	}

	public int getTotalTimeMillis() {
		return totalTimeMillis;
	}

	public int getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(int startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public int getDurationTimeMillis() {
		return durationTimeMillis;
	}

	public void setDurationTimeMillis(int durationTimeMillis) {
		this.durationTimeMillis = durationTimeMillis;
	}

	public int getRemainTimeMillis() {
		return this.totalTimeMillis - this.startTimeMillis;
	}

	public void setOnVideoPlayInfoListener(OnVideoPlayInfoListener onVideoPlayInfoListener) {
		this.onVideoPlayInfoListener = onVideoPlayInfoListener;
	}

	public void setOnPreparedVideoPlayerListener(OnPreparedVideoPlayerListener onPreparedVideoPlayerListener) {
		this.onPreparedVideoPlayerListener = onPreparedVideoPlayerListener;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		initVideoSize();
	}

	@Override
	protected void onDetachedFromWindow() {
		stopVideoRepeatHandler();
		super.onDetachedFromWindow();
	}
}
