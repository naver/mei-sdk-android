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
import android.view.View;

import com.naver.mei.sample.gallery.GalleryActivity;
import com.naver.mei.sample.gallery.GalleryHelper;
import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.utils.MeiIOUtils;

import org.apache.commons.io.IOUtils;

import java.util.List;

public class MultiFrameCompositeActivity extends CompositeActivity {
	public static final String INTENT_KEY_IMAGE_PATHS = "imagePaths";
	public static final String INTENT_KEY_FRAME_DELAY = "frameDelay";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		if (MeiIOUtils.isStorageSpaceFull()) return;

		if (!setBackgroundFromIntent(intent)) {
			selectBackgroundImage();
		}

		fabMenuFrameAlignment.setVisibility(View.VISIBLE);
	}

	private boolean setBackgroundFromIntent(Intent intent) {
		List<String> imagePaths = (List<String>)intent.getSerializableExtra(INTENT_KEY_IMAGE_PATHS);
		int frameDelay = intent.getIntExtra(INTENT_KEY_FRAME_DELAY, 200);
		if (imagePaths == null) return false;

		showStickerCompositionView();
		meiCanvasView.setBackgroundMultiFrame(imagePaths, frameDelay, FrameAlignment.KEEP_ORIGINAL_RATIO, PlayDirection.FORWARD);

		return true;
	}

	public void selectBackgroundImage() {
		Intent intent = new Intent(this, GalleryActivity.class);
		intent.putExtra(GalleryHelper.MediaSelectionMode.INTENT_PARAM_KEY, GalleryHelper.MediaSelectionMode.IMAGE_WITHOUT_GIF);
		intent.putExtra(GalleryActivity.INTENT_LAUNCH_MODE_KEY, GalleryActivity.LaunchMode.CREATE_GIF);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == 0 && resultCode == RESULT_OK) {
			setBackgroundFromIntent(intent);
		}
	}
}
