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
import android.net.Uri;
import android.os.Bundle;

import java.io.File;

public class CropImageCompositeActivity extends CompositeActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		File croppedImageFile = new File(intent.getStringExtra("uri"));
		if (croppedImageFile != null) {
			showStickerCompositionView();
			meiCanvasView.setBackgroundImageURI(Uri.fromFile(croppedImageFile));
		}
	}

}
