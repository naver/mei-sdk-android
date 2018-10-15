package com.naver.mei.sdk.core.image.compositor.type;

import com.naver.mei.sdk.core.image.compositor.strategy.FrameAlignmentStrategy;

/**
 * Created by GTPark on 2016-12-20.
 */

public enum FrameAlignment {
	FIT_SHORT_AXIS_CENTER_CROP(new FrameAlignmentStrategy.FitShortAxisCenterCrop()),
	KEEP_ORIGINAL_RATIO(new FrameAlignmentStrategy.KeepOriginalRatio());

	public final FrameAlignmentStrategy strategy;

	FrameAlignment(FrameAlignmentStrategy strategy) {
		this.strategy = strategy;
	}
}