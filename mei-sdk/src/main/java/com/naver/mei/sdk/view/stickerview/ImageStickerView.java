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
package com.naver.mei.sdk.view.stickerview;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.R;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.view.AnimationSynchronizer;
import com.naver.mei.sdk.view.MeiImageView;

/**
 * Created by Naver on 2016-10-20.
 */

public class ImageStickerView extends StickerView {
	private MeiImageView imageSticker;

	public ImageStickerView(Context context) {
		super(context, R.layout.sticker_image_view);
		imageSticker = (MeiImageView) sticker;
		imageSticker.setClickable(true);
		imageSticker.setFocusable(true);
		imageSticker.setFocusableInTouchMode(true);
		imageSticker.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					showOnlySticker();
				} else {
					showWholeSticker();
				}
			}
		});
	}

	private Uri uri;

	public void setImageUri(Uri uri) {
		this.uri = uri;
		imageSticker.setImageURI(this.uri);
	}

	public void setImageResourceId(int resId) {
		Context context = MeiSDK.getContext();
		Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
				context.getResources().getResourcePackageName(resId) + '/' +
				context.getResources().getResourceTypeName(resId) + '/' +
				context.getResources().getResourceEntryName(resId));

		setImageUri(uri);
	}

	public void setAnimationSynchronizer(AnimationSynchronizer synchronizer) {
		imageSticker.setAnimationSynchronizer(synchronizer);
	}

	public MeiImageView getStickerImageView() {
		return imageSticker;
	}

	public Uri getUri() {
		return uri;
	}

	public int getDuration() {
		return ((MeiImageView) this.sticker).getDuration();
	}

	public void setPlayDirection(PlayDirection playDirection) {
		this.imageSticker.setPlayDirection(playDirection);
	}
	public PlayDirection getPlayDirection() {
		return imageSticker.getPlayDirection();
	}
}
