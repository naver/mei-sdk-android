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

import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.naver.mei.sdk.core.utils.PixelUtils;
import com.naver.mei.sdk.core.utils.TrigonometryUtils;


/**
 * Sticker의 control 버튼을 누를때 발생하는 이벤트(resize, rotation)를 관리하는 리스너
 * resize시 control 버튼뿐만 아니라 delete 버튼도 같이 이동
 */
class StickerControlTouchListener implements View.OnTouchListener {
	private StickerPoint touchPoint;

	private int lastStickerWidth;
	private int lastStickerHeight;
	private int lastStickerLeft;
	private int lastStickerTop;
	private int lastStickerAngle;
	private int lastTextSize;

	private double lastComAngle;

	private StickerButtonSize controlButtonSize;
	private StickerButtonSize deleteButtonSize;
	private StickerPoint lastControlButtonPoint;
	private StickerPoint lastDeleteButtonPoint;
	private StickerPoint centerPosition;
	private StickerPoint lastTouchPoint;

	private View stickerView;
	private View deleteView;
	private TextView textView;
	private View dummyLeftTop;
	private View dummyLeftBottom;
	private int displayWidth;

	private float angle;

	private FrameLayout.LayoutParams controlButtonLayoutParams;
	private FrameLayout.LayoutParams deleteButtonLayoutParams;
	private FrameLayout.LayoutParams stickerLayoutParams;
	private FrameLayout.LayoutParams dummyLeftTopLayoutParams;
	private FrameLayout.LayoutParams dummyLeftBottomLayoutParams;


	public StickerControlTouchListener(View stickerView, View deleteView, View dummyLeftTop, View dummyLeftBottom, int displayWidth) {
		this.stickerView = stickerView;
		this.deleteView = deleteView;
		this.dummyLeftTop = dummyLeftTop;
		this.dummyLeftBottom = dummyLeftBottom;
		this.displayWidth = displayWidth;

		if (stickerView instanceof TextView) {
			textView = (TextView) stickerView;
		}

		initButtonsSize();
		initButtonsPosition();
	}

	private void initButtonsSize() {
		controlButtonSize = new StickerButtonSize(0, 0);
		deleteButtonSize = new StickerButtonSize(deleteView.getWidth(), deleteView.getHeight());
	}

	private void initButtonsPosition() {
		lastTouchPoint = new StickerPoint(0, 0);
		lastControlButtonPoint = new StickerPoint(0, 0);
		lastDeleteButtonPoint = new StickerPoint(deleteView.getLeft(), deleteView.getTop());
	}

	@Override
	public boolean onTouch(View controlView, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				controlButtonLayoutParams = (FrameLayout.LayoutParams) controlView.getLayoutParams();
				deleteButtonLayoutParams = (FrameLayout.LayoutParams) deleteView.getLayoutParams();
				stickerLayoutParams = (FrameLayout.LayoutParams) stickerView.getLayoutParams();
				dummyLeftTopLayoutParams = (FrameLayout.LayoutParams) dummyLeftTop.getLayoutParams();
				dummyLeftBottomLayoutParams = (FrameLayout.LayoutParams) dummyLeftBottom.getLayoutParams();

				touchPoint = getTouchPoint(controlButtonLayoutParams, event);    // 컨트롤 버튼을 누른 지점

				lastStickerWidth = stickerView.getWidth();
				lastStickerHeight = stickerView.getHeight();
				lastStickerLeft = stickerView.getLeft();
				lastStickerTop = stickerView.getTop();

				lastStickerAngle = (int) stickerView.getRotation();

				if (textView != null) {
					lastTextSize = (int) textView.getTextSize();
				}

				lastControlButtonPoint.update(controlButtonLayoutParams.leftMargin, controlButtonLayoutParams.topMargin);
				controlButtonSize.update(controlButtonLayoutParams.width, controlButtonLayoutParams.height);

				lastDeleteButtonPoint.update(deleteButtonLayoutParams.leftMargin, deleteButtonLayoutParams.topMargin);
				deleteButtonSize.update(deleteButtonLayoutParams.width, deleteButtonLayoutParams.height);

				updateLastTouchPoint(event);
				updateCenterPosition();
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				break;
			case MotionEvent.ACTION_UP: {
				break;
			}
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_MOVE:
				if (lastTouchPoint.x != -1) {
					if (Math.abs(event.getRawX() - lastTouchPoint.x) < 5 && Math.abs(event.getRawY() - lastTouchPoint.y) < 5) {
						return false;
					}
				}

				updateLastTouchPoint(event);

				StickerPoint beforePoint = touchPoint; // 컨트롤 버튼의 초기 위치
				StickerPoint afterPoint = getTouchPoint(controlButtonLayoutParams, event); // 이동 컨트롤 버튼의 위치
				float distanceBeforeToCenter = TrigonometryUtils.getDistance(centerPosition, beforePoint);   // 원점과 A사이의 거리
				float distanceAfterToCenter = TrigonometryUtils.getDistance(centerPosition, afterPoint);   // 원점과 B사이의 거리
				float ratio = distanceAfterToCenter / distanceBeforeToCenter;

				resizeStickerView(ratio);
				rotateStickerView(centerPosition, beforePoint, afterPoint, distanceBeforeToCenter, distanceAfterToCenter);
				updatePositionOfViews(controlView);
				break;
		}
		return false;
	}

	private void rotateStickerView(StickerPoint O, StickerPoint A, StickerPoint B, float dOA, float dOB) {
		float numerator = (((A.x - O.x) * (B.x - O.x)) + ((A.y - O.y) * (B.y - O.y)));
		float denominator = dOA * dOB;
		double comAngle = (180 * Math.acos(numerator / denominator) / TrigonometryUtils.PI);
		if (Double.isNaN(comAngle)) {
			comAngle = (lastComAngle < 90 || lastComAngle > 270) ? 0 : 180;
		} else if ((B.y - O.y) * (A.x - O.x) < (A.y - O.y) * (B.x - O.x)) {
			comAngle = 360 - comAngle;
		}
		lastComAngle = comAngle;

		angle = (float) (lastStickerAngle + comAngle);
		angle = angle % 360;
		stickerView.setRotation(angle);
	}

	private void resizeStickerView(float ratio) {
		int newWidth = (int) (lastStickerWidth * ratio);
		int newHeight = (int) (lastStickerHeight * ratio);
		int newTextSize = (int) (lastTextSize * ratio);

		if (newWidth < displayWidth) {	// 하드웨어 가속 이슈로 임시로 크기 제한 조치 취함 추후 gifEncoder가 ndk로 전환될 경우 삭제
			stickerLayoutParams.leftMargin = lastStickerLeft - ((newWidth - lastStickerWidth) / 2);
			stickerLayoutParams.topMargin = lastStickerTop - ((newHeight - lastStickerHeight) / 2);
			stickerLayoutParams.width = newWidth;
			stickerLayoutParams.height = newHeight;
			stickerView.setLayoutParams(stickerLayoutParams);

			if (textView != null) {
				textView.setTextSize(PixelUtils.px2dp(newTextSize));
			}
		}
	}

	private void updatePositionOfViews(View controlView) {
		StickerPoint rightBottomPointOfSticker = new StickerPoint(stickerView.getLeft() + stickerView.getWidth(), stickerView.getTop() + stickerView.getHeight());   // 스티커의 right, bottom 좌표 기준으로 control 버튼의 좌표를 구함
		StickerPoint pointOfControlButton = TrigonometryUtils.getAnglePoint(centerPosition, rightBottomPointOfSticker, angle);
		controlButtonLayoutParams.leftMargin = (int) (pointOfControlButton.x - controlButtonSize.getWidth() / 2);
		controlButtonLayoutParams.topMargin = (int) (pointOfControlButton.y - controlButtonSize.getHeight() / 2);
		controlView.setLayoutParams(controlButtonLayoutParams);

		StickerPoint leftBottomPointOfSticker = new StickerPoint(stickerView.getLeft(), stickerView.getTop() + stickerView.getHeight());   // 스티커의 left, bottom 좌표 기준으로 delete 버튼의 좌표를 구함
		float degree = TrigonometryUtils.getDegree(centerPosition, leftBottomPointOfSticker, rightBottomPointOfSticker);
		StickerPoint pointOfDeleteButton = TrigonometryUtils.getAnglePoint(centerPosition, rightBottomPointOfSticker, angle - (180 - degree));

		deleteButtonLayoutParams.leftMargin = (int) (pointOfDeleteButton.x - deleteButtonSize.getWidth() / 2);
		deleteButtonLayoutParams.topMargin = (int) (pointOfDeleteButton.y - deleteButtonSize.getHeight() / 2);
		deleteView.setLayoutParams(deleteButtonLayoutParams);

		StickerPoint anglePointForLeftTop = TrigonometryUtils.getAnglePoint(centerPosition, rightBottomPointOfSticker, angle - 180);
		dummyLeftTopLayoutParams.leftMargin = (int) (anglePointForLeftTop.x - dummyLeftTop.getWidth() / 2);
		dummyLeftTopLayoutParams.topMargin = (int) (anglePointForLeftTop.y - dummyLeftTop.getHeight() / 2);
		dummyLeftTop.setLayoutParams(dummyLeftTopLayoutParams);

		StickerPoint anglePointForLeftBottom = TrigonometryUtils.getAnglePoint(centerPosition, rightBottomPointOfSticker, angle + degree);
		dummyLeftBottomLayoutParams.leftMargin = (int) (anglePointForLeftBottom.x - dummyLeftBottom.getWidth() / 2);
		dummyLeftBottomLayoutParams.topMargin = (int) (anglePointForLeftBottom.y - dummyLeftTop.getHeight() / 2);
		dummyLeftBottom.setLayoutParams(dummyLeftBottomLayoutParams);
	}


	private void updateLastTouchPoint(MotionEvent event) {
		lastTouchPoint.update(event.getRawX(), event.getRawY());
	}

	private void updateCenterPosition() {
		int x = stickerView.getLeft() + stickerView.getWidth() / 2;
		int y = stickerView.getTop() + stickerView.getHeight() / 2;
		centerPosition = new StickerPoint(x, y);
	}

	private StickerPoint getTouchPoint(FrameLayout.LayoutParams layoutParams, MotionEvent event) {
		return new StickerPoint(layoutParams.leftMargin + (int) event.getX(), layoutParams.topMargin + (int) event.getY());
	}

}
