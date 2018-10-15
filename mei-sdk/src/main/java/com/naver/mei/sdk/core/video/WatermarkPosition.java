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
package com.naver.mei.sdk.core.video;

import static com.naver.mei.sdk.core.video.WatermarkPosition.XPosition.LEFT;
import static com.naver.mei.sdk.core.video.WatermarkPosition.XPosition.RIGHT;
import static com.naver.mei.sdk.core.video.WatermarkPosition.YPosition.BOTTOM;
import static com.naver.mei.sdk.core.video.WatermarkPosition.YPosition.TOP;

/**
 * Created by tigerbaby on 2017-04-14.
 */

public enum WatermarkPosition {
	LEFT_TOP(LEFT, TOP),
	LEFT_BOTTOM(LEFT, BOTTOM),
	RIGHT_TOP(RIGHT, TOP),
	RIGHT_BOTTOM(RIGHT, BOTTOM);

	public final XPosition xPosition;
	public final YPosition yPosition;

	WatermarkPosition(XPosition xPosition, YPosition yPosition) {
		this.xPosition = xPosition;
		this.yPosition = yPosition;
	}

	public int getLeft(int backgroundWidth, int watermarkWidth, int margin) {
		return xPosition.positionCalculator.getPosition(backgroundWidth, watermarkWidth, margin);
	}

	public int getTop(int backgroundHeight, int watermarkHeight, int margin) {
		return yPosition.positionCalculator.getPosition(backgroundHeight, watermarkHeight, margin);
	}

	public enum XPosition {
		LEFT(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundWidth, int watermarkWidth, int margin) {
				return margin;
			}
		}),
		RIGHT(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundWidth, int watermarkWidth, int margin) {
				return backgroundWidth - watermarkWidth - margin;
			}
		}),
		CENTER(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundWidth, int watermarkWidth, int margin) {
				return backgroundWidth / 2 - watermarkWidth / 2;
			}
		});

		XPosition(PositionCalculator positionCalculator) {
			this.positionCalculator = positionCalculator;
		}
		final PositionCalculator positionCalculator;
	}

	public enum YPosition {
		TOP(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundHeight, int watermarkHeight, int margin) {
				return margin;
			}
		}),
		BOTTOM(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundHeight, int watermarkHeight, int margin) {
				return backgroundHeight - watermarkHeight - margin;
			}
		}),
		MIDDLE(new PositionCalculator() {
			@Override
			public int getPosition(int backgroundHeight, int watermarkHeight, int margin) {
				return backgroundHeight / 2 - watermarkHeight / 2;
			}
		});

		YPosition(PositionCalculator positionCalculator) {
			this.positionCalculator = positionCalculator;
		}
		final PositionCalculator positionCalculator;
	}

	interface PositionCalculator {
		int getPosition(int background, int watermark, int margin);
	}
}