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
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.animated.base.AbstractAnimatedDrawable;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.naver.mei.sample.gallery.GalleryItem;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.view.MeiCanvasView;

import java.io.File;
import java.lang.reflect.Field;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImagePreviewActivity extends AppCompatActivity {
	public static final String INTENT_PATH_KEY = "path";
	private static final int RESIZE_WIDTH = 500;
	private static final int RESIZE_HEIGHT = 500;
//	@BindView(R.id.image_view)
//	SimpleDraweeView sdvImage;

	@BindView(R.id.mei_canvas_for_preview)
	MeiCanvasView meiCanvasView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		ButterKnife.bind(this);

		Intent intent = getIntent();
		String imagePath = intent.getStringExtra(INTENT_PATH_KEY);

		try {
			meiCanvasView.setBackgroundImageURI(Uri.fromFile(new File(imagePath)));
		} catch (OutOfMemoryError error) {
			Toast.makeText(this, "메모리가 부족합니다.", Toast.LENGTH_SHORT).show();
			finish();
		} catch (Exception e) {
			Toast.makeText(this, "정상적으로 처리되지 않았습니다.", Toast.LENGTH_SHORT).show();
			Log.e("ImagePreviewActivity", "preview error. ", e);
			finish();
		}

//		loadImageFitX(sdvImage, new File(imagePath).toURI().toString());
	}

//	private void loadImageFitX(final DraweeView draweeView, final String uri) {
//		ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri))
//				.setAutoRotateEnabled(true)
//				.setLocalThumbnailPreviewsEnabled(true)
//				.setResizeOptions(new ResizeOptions(RESIZE_WIDTH, RESIZE_HEIGHT))
//				.setImageDecodeOptions(ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).setDecodeAllFrames(false).build())
//				.setCacheChoice(ImageRequest.CacheChoice.SMALL)
//				.build();
//
//		draweeView.setController(Fresco.newDraweeControllerBuilder()
//				.setImageRequest(request)
//				.setOldController(draweeView.getController())
//				.setControllerListener(new BaseControllerListener<ImageInfo>() {
//					@Override
//					public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
//						updateViewSize(imageInfo);
//
//						if (animatable != null) {
//							try {
//								Field field = AbstractAnimatedDrawable.class.getDeclaredField("mLoopCount");
//								field.setAccessible(true);
//								field.set(animatable, 0);
//							} catch (Exception e) {
//								Log.e("GTPARK", "image preview error", e);
//							}
//							animatable.start();
//						}
//					}
//
//					@Override
//					public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
//						updateViewSize(imageInfo);
//					}
//
//					private void updateViewSize(@Nullable ImageInfo imageInfo) {
//						if (imageInfo == null) return;
//						int width = imageInfo.getWidth();
//						int height = imageInfo.getHeight();
//
//						draweeView.setAspectRatio(width / (float)height);
//						draweeView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
//						draweeView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
//					}
//
//					@Override
//					public void onFailure(String id, Throwable throwable) {
//						super.onFailure(id, throwable);
//						Log.e("MEI", "failed to load image preview", throwable);
//					}
//				})
//				.setAutoPlayAnimations(false)
//				.setUri(uri)
//				.build());
//
//	}
}
