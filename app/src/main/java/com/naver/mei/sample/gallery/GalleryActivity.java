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
package com.naver.mei.sample.gallery;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.naver.mei.sample.R;
import com.naver.mei.sample.util.MeiAlertUtil;
import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.core.utils.PixelUtils;
import com.naver.mei.sdk.core.utils.URIUtils;
import com.naver.mei.sdk.error.MeiLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GalleryActivity extends AppCompatActivity {

	private static final int CHECK_MY_PERMISSION_RESULT_CODE = 100;

	public static final String INTENT_LAUNCH_MODE_KEY = "launchMode";

	public enum LaunchMode {
		PICK_AND_GET,    // 선택된 Gallery Item에 대한 정보를 activityResult로써 반환
		CREATE_GIF       // 선택된 Gallery Item을 바탕으로 MEME 이미지 생성 플로우 진입
	}

	private LaunchMode launchMode;
	private GalleryHelper.MediaSelectionMode mediaSelectionMode;

	private static final int NUM_COLUMNS = 3;
	private static final int DP_COLUMN_SPACING = 3;

	private boolean isGalleryAdapterSetupAlready = false;

	@BindView(R.id.gallery_item_list)
	RecyclerView rvGalleryItems;
	GalleryRecyclerViewAdapter adapter;

	@BindView(R.id.title_layout_close)
	RelativeLayout titleLayout;

	@BindView(R.id.titlebar_icon_next)
	Button btnNext;

	@BindView(R.id.frame_delay)
	EditText etFrameDelay;

	@BindView(R.id.title)
	TextView tvTitle;

	@BindView(R.id.title_create_gif)
	ViewGroup vgGifTitle;


	Dialog dgPermissionCheck;
	boolean isShowingPermissionCheckPopupWindow = false;    // 팝업 윈도우의 show 호출 시점이 액티비티가 그려지기 전이므로, 동기적으로 가시성을 체크할 수 없어 별도의 플래그 삽입

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery);
		ButterKnife.bind(this);
		init();
	}

	private void init() {
		loadLaunchMode();
		loadMediaSelectionMode();
		initRecyclerViewConfig();
		initePermissionCheckDialog();
	}

	private void loadLaunchMode() {
		launchMode = (LaunchMode) getIntent().getSerializableExtra(INTENT_LAUNCH_MODE_KEY);
		if (launchMode == null) {
			throw new RuntimeException("launch mode를 설정하여야 합니다.");
		}

		switch (launchMode) {
			case PICK_AND_GET:
				tvTitle.setVisibility(View.VISIBLE);
				vgGifTitle.setVisibility(View.INVISIBLE);
				btnNext.setVisibility(View.INVISIBLE);
				break;
			case CREATE_GIF:
				tvTitle.setVisibility(View.INVISIBLE);
				vgGifTitle.setVisibility(View.VISIBLE);
				btnNext.setVisibility(View.VISIBLE);
				break;
		}
	}

	private void loadMediaSelectionMode() {
		mediaSelectionMode = (GalleryHelper.MediaSelectionMode) getIntent().getSerializableExtra(GalleryHelper.MediaSelectionMode.INTENT_PARAM_KEY);
		mediaSelectionMode = mediaSelectionMode != null ? mediaSelectionMode : GalleryHelper.MediaSelectionMode.ALL;
	}

	private void setupRecyclerViewAdapter() {
		// check invalid file
		List<GalleryItem> galleryItems = GalleryHelper.getInstance().getGalleryItems(mediaSelectionMode);
		Iterator<GalleryItem> iterator = galleryItems.iterator();

		while (iterator.hasNext()) {
			GalleryItem galleryItem = iterator.next();
			if (!MeiFileUtils.isExists(galleryItem.uri)) {
				iterator.remove();
			}
		}

		adapter = new GalleryRecyclerViewAdapter(
				this,
				galleryItems,
				new GalleryItemClickHandler(),
				new GalleryItemLongClickHandler()
		);
		rvGalleryItems.setAdapter(adapter);
	}

	private void initRecyclerViewConfig() {
		GridLayoutManager glm = new GridLayoutManager(this, NUM_COLUMNS);
		rvGalleryItems.setLayoutManager(glm);
		rvGalleryItems.addItemDecoration(new ItemSpacingDecorator(NUM_COLUMNS, PixelUtils.dp2px(DP_COLUMN_SPACING)));

	}

	class GalleryItemClickHandler implements GalleryRecyclerViewAdapter.OnItemClickListener {
		@Override
		public void onItemClick(View v, int position) {
			GalleryItem galleryItem = (GalleryItem) v.getTag();

			if (!MeiFileUtils.isExists(galleryItem.uri)) {
				removeDeletedFile(position);
				MeiAlertUtil.show(GalleryActivity.this, "not exist file");
				return;
			}

			switch (launchMode) {
				case PICK_AND_GET:
					handlePickAndGet(galleryItem);
					break;
				case CREATE_GIF:
					adapter.clickItem(position);
					break;
				default:
					Log.e("MEI", "unknown gallery activity start mode");
			}
		}
	}

	class GalleryItemLongClickHandler implements GalleryRecyclerViewAdapter.OnItemLongClickListener {
		@Override
		public void onLongClick(View v, int position) {
			GalleryItem galleryItem = (GalleryItem) v.getTag();

			if (!MeiFileUtils.isExists(galleryItem.uri)) {
				removeDeletedFile(position);
				MeiAlertUtil.show(GalleryActivity.this, "not exist file");
				return;
			}

			switch (launchMode) {
				case PICK_AND_GET:
					handlePickAndGet(galleryItem);
					break;
				default:
					Log.e("MEI", "unknown gallery activity start mode");
			}
		}
	}

	private void handlePickAndGet(GalleryItem galleryItem) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra(GalleryItem.INTENT_PARAM_KEY, galleryItem);
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	@OnClick(R.id.titlebar_icon_close)
	public void onCloseButtonClick() {
		finish();
	}


	@OnClick(R.id.titlebar_icon_next)
	public void onNextClick() {
		List<Integer> deletedFilePositions = checkDeletedFile();

		if (deletedFilePositions.isEmpty()) {
			setResultToMultiFrame();
		} else {
			removeDeletedFiles(deletedFilePositions);
			MeiAlertUtil.show(this, "some files deleted. please reselect");
		}
	}

	private void removeDeletedFile(int position) {
		adapter.getGalleryItems().remove(position);
		adapter.notifyItemRemoved(position);
	}

	private void removeDeletedFiles(List<Integer> deletedFilePositions) {
		List<GalleryItem> removeItems = new ArrayList<>();

		for (Integer position : deletedFilePositions) {
			removeItems.add(adapter.getGalleryItem(position));
		}

		adapter.getGalleryItems().removeAll(removeItems);
		adapter.clearSelectedPosition();
	}

	private List checkDeletedFile() {
		List<Integer> deletedFilePositions = new ArrayList<>();

		for (Integer position : adapter.getSelectedPositions()) {
			if (!MeiFileUtils.isExists(adapter.getGalleryItem(position).uri)) {
				deletedFilePositions.add(position);
			}
		}

		return deletedFilePositions;
	}


	private void setResultToMultiFrame() {
		if (adapter.getSelectedPositions().size() == 0) {
			MeiAlertUtil.show(this, "이미지를 선택해주세요.");
			setResult(RESULT_CANCELED);
			return;
		}

		String strFrameDelay = etFrameDelay.getText().toString();
		int frameDelay = (!TextUtils.isDigitsOnly(strFrameDelay) || TextUtils.isEmpty(strFrameDelay)) ? 0 : Integer.parseInt(strFrameDelay);

		if (frameDelay < 60 || frameDelay > 10000) {
			MeiAlertUtil.show(this, "Frame Delay는 60 이상, 10000이하만 지원합니다.");
			setResult(RESULT_CANCELED);
			return;
		}

		ArrayList<String> imagePaths = new ArrayList<>();


		for (Integer position : adapter.getSelectedPositions()) {
			GalleryItem item = adapter.getGalleryItem(position);
			imagePaths.add(URIUtils.uriStrToPath(item.uri));
		}

		Intent resultIntent = new Intent();
		resultIntent.putExtra("imagePaths", imagePaths);
		resultIntent.putExtra("frameDelay", frameDelay);


		setResult(RESULT_OK, resultIntent);
		finish();
	}

	private void initePermissionCheckDialog() {
		this.dgPermissionCheck = new AlertDialog.Builder(this)
				.setMessage("갤러리 실행을 위해 권한이 필요합니다. 추가하시겠습니까?")
				.setPositiveButton("YES", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						isShowingPermissionCheckPopupWindow = false;

						Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
						myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
						myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(myAppSettings);
					}
				})
				.setNegativeButton("NO", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onBackPressed();
					}
				})
				.create();
	}

	@Override
	public void onBackPressed() {
		if (dgPermissionCheck.isShowing()) {
			isShowingPermissionCheckPopupWindow = false;
			finish();
			return;
		}

		super.onBackPressed();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Android Marshmellow Permission Problem 대응
		checkPermission();
	}

	private void checkPermission() {
		if (isShowingPermissionCheckPopupWindow) { // 퍼미션 체크 팝업 윈도우가 노출 중인 경우에는 다시 체크하지 않는다.
			return;
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE}, CHECK_MY_PERMISSION_RESULT_CODE);
		} else {
			initPermissionAfter();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode != CHECK_MY_PERMISSION_RESULT_CODE) return;

		// 권한 요청에 대해 수락하였다면
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
			initPermissionAfter();
		} else {
			dgPermissionCheck.show();
			isShowingPermissionCheckPopupWindow = true;
		}
	}

	private void initPermissionAfter() {
		if (isGalleryAdapterSetupAlready) return;

		setupRecyclerViewAdapter();
		isGalleryAdapterSetupAlready = true;
	}

}
