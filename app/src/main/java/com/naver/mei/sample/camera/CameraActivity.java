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
package com.naver.mei.sample.camera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.mei.sample.MultiFrameCompositeActivity;
import com.naver.mei.sample.R;
import com.naver.mei.sdk.core.camera.CameraLoadListener;
import com.naver.mei.sdk.core.camera.CameraRatio;
import com.naver.mei.sdk.core.camera.CaptureCallback;
import com.naver.mei.sdk.core.camera.ContinuousCaptureCallback;
import com.naver.mei.sdk.core.camera.MeiCameraView;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.core.utils.MeiPermissionUtils;
import com.naver.mei.sdk.core.utils.PixelUtils;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Naver on 2016-11-10.
 */
public class CameraActivity extends AppCompatActivity implements PictureSizeSelectListener {
	private static final int REQUEST_PERMISSIONS = 1000;

	@BindView(R.id.spinner_pic_count)
	Spinner picCountSpinner;
	@BindView(R.id.bar)
	LinearLayout barLayout;
	@BindView(R.id.camera_frame)
	EditText etFrameDelay;
	@BindView(R.id.camera_ratio)
	ImageButton ratioButton;
	@BindArray(R.array.picture_count)
	String[] countList;

	MeiCameraView meiCameraView;

	int selectedCount;
	int captureCount;

	List<String> imageFilePaths = new ArrayList<>();
	boolean isFlashOff;
	int ratioClickCount;

	boolean isCapturing;

	PopupWindow pictureSizePopup;
	PictureSizeRecyclerViewAdapter pictureSizeRecyclerViewAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		ButterKnife.bind(this);

		if (MeiIOUtils.isStorageSpaceFull()) return;

		checkPermission();
	}

	private void initUI() {
		meiCameraView = (MeiCameraView) findViewById(R.id.mei_camera_view);
		meiCameraView.init();
	}

	private void initEvent() {
		picCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
				barLayout.removeAllViews();
				captureCount = 0;
				imageFilePaths.clear();
				countList = getResources().getStringArray(R.array.picture_count);
				selectedCount = Integer.valueOf(countList[position]);
				barLayout.setWeightSum(selectedCount);

				addBar(selectedCount);

			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		});

		meiCameraView.setOnCameraLoadListener(new CameraLoadListener() {
			@Override
			public void onCameraOpenComplete() {
				meiCameraView.setAutoFocusEnabled(true);
			}
		});
	}

	private void addBar(int selectedCount) {
		for (int i = 0; i < selectedCount; i++) {
			if (i == (selectedCount - 1)) {
				barLayout.addView(createCaptureBar(true));
			} else {
				barLayout.addView(createCaptureBar(false));
			}
		}
	}

	private void setResultToMultiFrame() {
		ArrayList<String> imagePaths = new ArrayList<>();

		for (String filePath : imageFilePaths) {
			imagePaths.add(filePath);
		}

		Intent resultIntent = new Intent(this, MultiFrameCompositeActivity.class);
		resultIntent.putExtra(MultiFrameCompositeActivity.INTENT_KEY_IMAGE_PATHS, imagePaths);

		startActivity(resultIntent);
		finish();
	}

	private View createCaptureBar(boolean isLast) {
		View captureBarView = new View(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, PixelUtils.dp2px(3), 1f);

		captureBarView.setBackgroundColor(0xff0BC904);
		captureBarView.setVisibility(View.INVISIBLE);
		if (!isLast) {
			params.setMargins(0, 0, PixelUtils.dp2px(2), 0);
		}
		captureBarView.setLayoutParams(params);

		return captureBarView;
	}

	@OnClick(R.id.capture_button)
	public void capture() {
		captureOnce();          //1회 촬영
//		captureContinuous();    //연속 촬영
	}

	private void captureOnce() {
		if (captureCount >= selectedCount) return;

		captureCount++;
		meiCameraView.capture(new CaptureCallback() {
			@Override
			public void onSave(String resultFilePath) {
				MeiLog.d("cpature callback result : " + resultFilePath);
				imageFilePaths.add(resultFilePath);
				barLayout.getChildAt(imageFilePaths.size() - 1).setVisibility(View.VISIBLE);

				if (selectedCount == imageFilePaths.size()) {
					setResultToMultiFrame();
				}
			}

			@Override
			public void onFail(MeiSDKErrorType errorType) {
				captureCount--;
				Toast.makeText(CameraActivity.this, errorType.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void captureContinuous() {
		int intervalMs = 100;

		if (isCapturing) {
			meiCameraView.finishCapturing();
			isCapturing = false;
		} else {
			meiCameraView.startCapturing(new ContinuousCaptureCallback() {
				@Override
				public void onFinish(List<String> resultFilePaths) {
					MeiLog.d("capture callback result count : " + resultFilePaths.size());
				}

				@Override
				public void onSaving(int sequence, String resultFilePath) {
					MeiLog.d("capture callback saving... : " + sequence + " / " + resultFilePath);
				}

				@Override
				public void onFail(MeiSDKErrorType errorType) {
					Toast.makeText(CameraActivity.this, errorType.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}, intervalMs);
			isCapturing = true;
		}
	}

	@OnClick(R.id.camera_flash)
	public void flash() {
		isFlashOff = !isFlashOff;
		if (isFlashOff) {
			meiCameraView.flashOn();
		} else {
			meiCameraView.flashOff();
		}
	}

	@OnClick(R.id.camera_switch)
	public void switchCamera() {
		meiCameraView.switchCamera();
	}

	// 사용자 정의 카메라 비율 촬영
	@OnClick(R.id.camera_ratio)
	public void changeCustomRatio() {
		ratioClickCount++;
		switch (ratioClickCount) {
			case 0:
				ratioButton.setBackgroundResource(R.drawable.ico_r34);
				meiCameraView.setPictureAspectRatio(CameraRatio.THREE_BY_FOUR_USER);
				break;
			case 1:
				ratioButton.setBackgroundResource(R.drawable.ico_r11);
				meiCameraView.setPictureAspectRatio(CameraRatio.ONE_BY_ONE);
				break;
			case 2:
				ratioButton.setBackgroundResource(R.drawable.ico_r43);
				meiCameraView.setPictureAspectRatio(CameraRatio.FOUR_BY_THREE_USER);
				break;
			default:
				ratioButton.setBackgroundResource(R.drawable.ico_r34);
				meiCameraView.setPictureAspectRatio(CameraRatio.THREE_BY_FOUR_USER);
		}


		if (ratioClickCount == 3) {
			ratioClickCount = 0;
		}
	}

	@OnClick(R.id.camera_close)
	public void close() {
//        System.exit(0);
		onBackPressed();
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

		barLayout.removeAllViews();
		captureCount = 0;
		imageFilePaths.clear();
		selectedCount = Integer.parseInt(picCountSpinner.getSelectedItem().toString());
		addBar(selectedCount);


	}

	// 카메라 기본 제공 사진 사이즈 선택 팝업
	@OnClick(R.id.camera_pic_size)
	public void setPictureSize() {
		if (pictureSizePopup == null) {
			View popupView = getLayoutInflater().inflate(R.layout.popup_select_pic_size, null);
			RecyclerView recyclerView = (RecyclerView) popupView.findViewById(R.id.camera_pic_size_recycler_view);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));

			pictureSizeRecyclerViewAdapter = new PictureSizeRecyclerViewAdapter(meiCameraView.getSupportedPictureSizes(), this);
			recyclerView.setAdapter(pictureSizeRecyclerViewAdapter);
			pictureSizePopup = new PopupWindow(popupView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		}
		pictureSizePopup.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.NO_GRAVITY, 0, 0);
	}

	@Override
	public void selectPictureSize(int width, int height) {
		meiCameraView.setPictureAspectRatio(width, height);    // 카메라 기본 제공 비율을 사용할 경우 w, h를 넘김

		pictureSizePopup.dismiss();
	}

	private void checkPermission() {
		if (!MeiPermissionUtils.checkPermission(Manifest.permission.CAMERA)) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
				Snackbar.make(findViewById(android.R.id.content), "need CAMERA permission", Snackbar.LENGTH_INDEFINITE).setAction("Use Permission",
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
							}
						}).show();
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
			}
		} else {
			initUI();
			initEvent();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				initUI();
				initEvent();
			} else {
				finish();
			}
		}
	}

	class PictureSizeRecyclerViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ratio_radio_button)
		RadioButton rbRatio;
		@BindView(R.id.picture_size_text_view)
		TextView tvPictureSize;

		public PictureSizeRecyclerViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}
	}

	class PictureSizeRecyclerViewAdapter extends RecyclerView.Adapter<PictureSizeRecyclerViewHolder> {
		private List<Camera.Size> pictureSizeList;
		private RadioButton lastChecked = null;
		private PictureSizeSelectListener pictureSizeSelectListener;

		public PictureSizeRecyclerViewAdapter(List<Camera.Size> pictureSizeList, PictureSizeSelectListener pictureSizeSelectListener) {
			this.pictureSizeList = pictureSizeList;
			this.pictureSizeSelectListener = pictureSizeSelectListener;
		}

		@Override
		public PictureSizeRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_picture_size, parent, false);
			return new PictureSizeRecyclerViewHolder(view);
		}

		@Override
		public void onBindViewHolder(PictureSizeRecyclerViewHolder holder, final int position) {
			holder.rbRatio.setText(getRatio(Math.round((float) pictureSizeList.get(position).width / pictureSizeList.get(position).height * 100f) / 100f));
			holder.rbRatio.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					RadioButton rbChecked = (RadioButton) view;
					if (lastChecked != null) {
						lastChecked.setChecked(false);
					}
					lastChecked = rbChecked;
					pictureSizeSelectListener.selectPictureSize(pictureSizeList.get(position).width, pictureSizeList.get(position).height);
				}
			});
			holder.tvPictureSize.setText(pictureSizeList.get(position).width + "*" + pictureSizeList.get(position).height);
		}

		@Override
		public int getItemCount() {
			return pictureSizeList.size();
		}

		private String getRatio(float value) {
			String ratio;
			if (value == CameraRatio.FOUR_BY_THREE_CAM.getValue()) {
				ratio = CameraRatio.FOUR_BY_THREE_CAM.getText();
			} else if (value == CameraRatio.ONE_BY_ONE.getValue()) {
				ratio = CameraRatio.ONE_BY_ONE.getText();
			} else {
				ratio = CameraRatio.SIXTEEN_BY_NINE_CAM.getText();
			}

			return ratio;
		}
	}
}
