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

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.naver.mei.sample.gallery.GalleryActivity;
import com.naver.mei.sample.gallery.GalleryHelper;
import com.naver.mei.sample.gallery.GalleryItem;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.core.utils.URIUtils;
import com.naver.mei.sdk.image.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ImageCropActivity extends AppCompatActivity {
	@BindView(R.id.select_image)
	Button selectButton;
	@BindView(R.id.crop_relative_layout)
	RelativeLayout relativeLayout;
	@BindView(R.id.crop_image_view)
	CropImageView cropImageView;
	private Pair<Integer, Integer> aspectRatio = new Pair<>(1, 1);
	private boolean fixAspectRatio;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_crop);
		ButterKnife.bind(this);
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

	@OnClick(R.id.select_image)
	public void selectImageToCrop() {
		Intent intent = new Intent(this, GalleryActivity.class);
		intent.putExtra(GalleryHelper.MediaSelectionMode.INTENT_PARAM_KEY, GalleryHelper.MediaSelectionMode.IMAGE_ONLY);
		intent.putExtra(GalleryActivity.INTENT_LAUNCH_MODE_KEY, GalleryActivity.LaunchMode.PICK_AND_GET);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 0 && resultCode == RESULT_OK) {
			GalleryItem galleryItem = (GalleryItem) data.getSerializableExtra(GalleryItem.INTENT_PARAM_KEY);
			showCropImageView();

			if (galleryItem == null) return;

			cropImageView.setImageUri(Uri.fromFile(new File(URIUtils.uriStrToPath(galleryItem.uri))));

		}
	}

	protected void showCropImageView() {
		selectButton.setVisibility(View.GONE);
		relativeLayout.setVisibility(View.VISIBLE);
	}

	@OnClick(R.id.aspect_ratio_button)
	public void setAspectRatio(Button button) {
		if (!fixAspectRatio) {
			fixAspectRatio = true;
			aspectRatio = new Pair<>(1, 1);
		} else {
			if (aspectRatio.first == 1 && aspectRatio.second == 1) {
				aspectRatio = new Pair<>(4, 3);
			} else if (aspectRatio.first == 4 && aspectRatio.second == 3) {
				aspectRatio = new Pair<>(16, 9);
			} else if (aspectRatio.first == 16 && aspectRatio.second == 9) {
				aspectRatio = new Pair<>(9, 16);
			} else {
				fixAspectRatio = false;
			}
		}

		cropImageView.setAspectRatio(aspectRatio.first, aspectRatio.second);
		cropImageView.setFixedAspectRatio(fixAspectRatio);

		String aspectRatioText = "FREE";
		if (fixAspectRatio) {
			aspectRatioText = aspectRatio.first + ":" + aspectRatio.second;
		}
		button.setText(aspectRatioText);

	}

	@OnClick(R.id.crop_button)
	public void crop() {
		String fileName = MeiFileUtils.getUniquePath("jpg");
		File pictureFile = new File(fileName);
		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			Bitmap bitmap = cropImageView.getCroppedImage();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();

			Intent intent = new Intent(ImageCropActivity.this, CropImageCompositeActivity.class);
			intent.putExtra("uri", fileName);
			startActivity(intent);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@OnClick(R.id.rotate_button)
	public void rotate() {
		cropImageView.rotateImage();    // 90도 단위 회전
	}
}
