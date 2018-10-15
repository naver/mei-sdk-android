package com.naver.mei.sdk.core.image.compositor.type;

import com.naver.mei.sdk.core.image.compositor.strategy.MaxWidthHeighSizeStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.MultiFrameSizeStrategy;

/**
 * Created by GTPark on 2016-12-20.
 */

public enum SizeOptions {
	MAX_WIDTH_HEIGHT(new MaxWidthHeighSizeStrategy());

	public final MultiFrameSizeStrategy strategy;

	SizeOptions(MultiFrameSizeStrategy strategy) {
		this.strategy = strategy;
	}
}