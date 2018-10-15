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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.naver.mei.sample.gallery.GalleryActivity;
import com.naver.mei.sample.gallery.GalleryHelper;
import com.naver.mei.sample.gallery.GalleryItem;
import com.naver.mei.sample.listener.RecyclerItemClickListener;
import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.core.utils.SoftKeyboardHelper;
import com.naver.mei.sdk.core.utils.URIUtils;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.listener.MeiEventListener;
import com.naver.mei.sdk.view.MeiCanvasView;
import com.naver.mei.sdk.view.stickerview.ImageStickerView;
import com.naver.mei.sdk.view.stickerview.StickerView;
import com.naver.mei.sdk.view.stickerview.TextStickerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CompositeActivity extends Activity {
	@BindView(R.id.select_background)
	Button selectButton;
	@BindView(R.id.outer_layout)
	LinearLayout relativeLayout;
	@BindView(R.id.fab_menu)
	FloatingActionMenu fabMenu;
	@BindView(R.id.fab_menu_reverse_background)
	FloatingActionButton fabMenuReverseBackground;
	@BindView(R.id.fab_menu_add_text)
	FloatingActionButton fabMenuAddText;
	@BindView(R.id.fab_menu_add_image)
	FloatingActionButton fabMenuAddImage;
	@BindView(R.id.fab_menu_save)
	FloatingActionButton fabMenuSave;
	@BindView(R.id.fab_menu_background_speed_normal)
	FloatingActionButton fabMenuSpeedNormal;
	@BindView(R.id.fab_menu_background_speed_half)
	FloatingActionButton fabMenuSpeedHalf;
	@BindView(R.id.fab_menu_background_speed_double)
	FloatingActionButton fabMenuSpeedDouble;
	@BindView(R.id.fab_menu_frame_alignment)
	FloatingActionButton fabMenuFrameAlignment;
	@BindView(R.id.fab_menu_aspect_ratio)
	FloatingActionButton fabMenuAspectRatio;
	@BindView(R.id.fab_menu_aspect_ratio_init)
	FloatingActionButton fabMenuAspectRatioInit;
	@BindView(R.id.mei_canvas)
	MeiCanvasView meiCanvasView;

	SoftKeyboardHelper softKeyboardHelper;

	private PopupWindow colorPalettePopup;
	private PopupWindow stickerPopup;
	protected ProgressPopupWindow progressPopupWindow;

	private int[] resourceIds = {
			R.drawable.ico1,
			R.drawable.ico2,
			R.drawable.ico3,
			R.drawable.sticker_pockemon,
			R.drawable.sticker_baby_2,
			R.drawable.sticker_dog_2
	};

	ColorDrawable selectedColor;
	CompositeActivity.IconRecyclerViewAdapter iconRecyclerViewAdapter;
	File file;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_composite);
		ButterKnife.bind(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		progressPopupWindow = new ProgressPopupWindow(this);
		initEvent();
		softKeyboardHelper = new SoftKeyboardHelper(getWindow()) {
			@Override
			public void onKeyboardChangeEvent(boolean isVisible) {
				if (!isVisible) {
					meiCanvasView.clearAllFocus();
				}
			}
		};
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

	private void initEvent() {
		fabMenuAddText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				showColorPalettePopup();

			}
		});

		fabMenuAddImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				selectStickerFromResource();
			}
		});

		fabMenuSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.getBackgroundImageView().requestFocus();
				save();
			}
		});

		fabMenuReverseBackground.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.setBackgroundPlayDirection(PlayDirection.REVERSE);
			}
		});

		fabMenuSpeedNormal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.setSpeedRatio(1.0);
			}
		});

		fabMenuSpeedHalf.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.setSpeedRatio(0.5);
			}
		});

		fabMenuSpeedDouble.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.setSpeedRatio(2.0);
			}
		});

		fabMenuFrameAlignment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				FloatingActionButton fab = (FloatingActionButton) v;
				if (fab.getLabelText().equals("화면맞춤")) {
					fab.setLabelText("원본비율유지");
					meiCanvasView.setBackgroundMultiFrameAlignment(FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP);
				} else {
					fab.setLabelText("화면맞춤");
					meiCanvasView.setBackgroundMultiFrameAlignment(FrameAlignment.KEEP_ORIGINAL_RATIO);
				}
			}
		});
		fabMenuFrameAlignment.setVisibility(View.GONE);

		fabMenuAspectRatio.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				FloatingActionButton fab = (FloatingActionButton) v;
				if (fab.getLabelText().equals("1:1")) {
					fab.setLabelText("3:4");
					meiCanvasView.setAspectRatio(1);
				} else {
					fab.setLabelText("1:1");
					meiCanvasView.setAspectRatio(3 / 4.0);
				}
			}
		});

		fabMenuAspectRatioInit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fabMenu.toggle(true);
				meiCanvasView.setAspectRatio(meiCanvasView.getOriginalAspectRatio());
			}
		});
	}

	@OnClick(R.id.select_background)
	public void selectBackgroundImage() {
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
			showStickerCompositionView();

			if (galleryItem == null) return;

			file = new File(URIUtils.uriStrToPath(galleryItem.uri));

			try {
				meiCanvasView.setBackgroundImageURI(Uri.fromFile(file));
			} catch (OutOfMemoryError error) {
				Toast.makeText(this, "메모리가 부족합니다.", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	protected void showStickerCompositionView() {
		selectButton.setVisibility(View.GONE);
		relativeLayout.setVisibility(View.VISIBLE);
		fabMenu.setVisibility(View.VISIBLE);
	}

	private List<StickerView> getAllStickers() {
		int zIndex = 0;
		List<StickerView> allStickers = new ArrayList<>();
		int count = relativeLayout.getChildCount();
		for (int i = 0; i < count; i++) {
			View view = relativeLayout.getChildAt(i);
			if (view instanceof StickerView) {
				StickerView stickerView = (StickerView) view;
				stickerView.setZIndex(zIndex++);
				allStickers.add(stickerView);
			}
		}

		return allStickers;

	}

	private void showColorPalettePopup() {
		if (colorPalettePopup == null) {
			View popupView = getLayoutInflater().inflate(R.layout.popup_text_color, null);
			colorPalettePopup = new PopupWindow(popupView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		}
		colorPalettePopup.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.BOTTOM, 0, 0);
	}

	public void selectTextColor(View color) {
		selectedColor = (ColorDrawable) color.getBackground();
		colorPalettePopup.dismiss();
		addTextStickerView();
	}

	private void addTextStickerView() {
		final TextStickerView textStickerView = new TextStickerView(this);
		textStickerView.setLocation(0, 0);
		textStickerView.setMaxEditTextWidthRatio(0.8f);

		EditText editText = textStickerView.getEditText();

		if (selectedColor != null) {
			editText.setTextColor(selectedColor.getColor());
		}

		meiCanvasView.addStickerView(textStickerView);
	}

	public void selectStickerFromResource() {
		if (stickerPopup == null) {
			View popupView = getLayoutInflater().inflate(R.layout.popup_select_resource, null);
			RecyclerView recyclerView = (RecyclerView) popupView.findViewById(R.id.resource_sticker_recycler_view);
			recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
			iconRecyclerViewAdapter = new CompositeActivity.IconRecyclerViewAdapter(resourceIds);
			recyclerView.setAdapter(iconRecyclerViewAdapter);
			recyclerView.addOnItemTouchListener(
					new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
						@Override
						public void onItemClick(View view, int position) {
							final ImageStickerView imageStickerView = new ImageStickerView(CompositeActivity.this); // sdk
							imageStickerView.setImageResourceId(resourceIds[position]);
							imageStickerView.setLocation(0, 0);
							meiCanvasView.addStickerView(imageStickerView);
							stickerPopup.dismiss();
						}

						@Override
						public void onLongItemClick(View view, int position) {
						}
					})
			);
			stickerPopup = new PopupWindow(popupView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		}

		stickerPopup.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.BOTTOM, 0, 0);
	}

	private void save() {
		if (MeiIOUtils.isStorageSpaceFull()) {
			Toast.makeText(this, "저장공간이 가득찬 상태에서 이미지를 생성할 수 없습니다.", Toast.LENGTH_SHORT).show();
			return;
		}

		fabMenu.toggle(false);

		compositeBySDK();
	}

	private void compositeBySDK() {
		progressPopupWindow.show();

		final long startTime = System.currentTimeMillis();
		MeiSDK.createImageCompositor().setMeiCanvasView(meiCanvasView)
				.setEventListener(new MeiEventListener() {
					@Override
					public void onSuccess(final String resultFilePath) {
						Intent intent = new Intent(CompositeActivity.this, ImagePreviewActivity.class);
						intent.putExtra(ImagePreviewActivity.INTENT_PATH_KEY, resultFilePath);
						startActivity(intent);
						finish();
					}

					@Override
					public void onFail(MeiSDKErrorType meiSDKErrorType) {
						Toast.makeText(CompositeActivity.this, "이미지 합성을 실패하였습니다.", Toast.LENGTH_SHORT).show();
						finish();
					}

					@Override
					public void onProgress(final double progress) {
						if (progressPopupWindow.isShowing()) {
							if (progress == 0.0) return;
							progressPopupWindow.setProgressOfComposition(progress, startTime);
						}
					}
				})
				.setOutputWidth(640)
				.composite();
	}

	class IconRecyclerViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.icon_image_view)
		ImageView iconImageView;

		public IconRecyclerViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}
	}

	class IconRecyclerViewAdapter extends RecyclerView.Adapter<CompositeActivity.IconRecyclerViewHolder> {
		final int[] iconResourceIds;

		public IconRecyclerViewAdapter(int[] iconResourceIds) {
			this.iconResourceIds = iconResourceIds;
		}

		@Override
		public CompositeActivity.IconRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false);
			return new CompositeActivity.IconRecyclerViewHolder(view);
		}

		@Override
		public void onBindViewHolder(CompositeActivity.IconRecyclerViewHolder holder, int position) {
			holder.iconImageView.setImageResource(iconResourceIds[position]);

		}

		@Override
		public int getItemCount() {
			return iconResourceIds.length;
		}
	}

	@Override
	public void onBackPressed() {
		if (stickerPopup != null && stickerPopup.isShowing()) {
			stickerPopup.dismiss();
			return;
		}

		if (colorPalettePopup != null && colorPalettePopup.isShowing()) {
			colorPalettePopup.dismiss();
			return;
		}

		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		progressPopupWindow.dismiss();
	}
}
