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
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;

import com.naver.mei.sample.util.MeiAlertUtil;
import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiLog;

import java.util.Arrays;

/**
 * Created by GTPark on 2016-05-23.
 */
public class VideoErrorHelper {
	private static final String[] VIDEO_MIMETYPE = {"video/mp4", "video/x-matroska", "video/x-ms-wmv"};
	private static final String[] AVAILABLE_EXTENSIONS = {"mp4", "3gp", "mkv", "wmv"};

	public static class VideoPlayerErrorListener implements MediaPlayer.OnErrorListener {
		private Activity activity;

		public VideoPlayerErrorListener(Activity activity) {
			this.activity = activity;
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			switch (what) {
				case MediaPlayer.MEDIA_ERROR_UNKNOWN:
				case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
					MeiAlertUtil.show(activity, "invalid format", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							activity.finish();
						}
					});
					break;
				case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
					MeiAlertUtil.show(activity, "This video failed to load");
					break;
			}
			return true;
		}
	}

	public static boolean isAvailableVideo(String videoUri) {
		try {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			retriever.setDataSource(MeiSDK.getContext(), Uri.parse(videoUri));
			retriever.getFrameAtTime(10000);
			String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
			return Arrays.asList(VIDEO_MIMETYPE).contains(mimeType) && Arrays.asList(AVAILABLE_EXTENSIONS).contains(MeiFileUtils.getFileExtension(videoUri));
		} catch (Exception ex) {
			return false;
		}
	}
}