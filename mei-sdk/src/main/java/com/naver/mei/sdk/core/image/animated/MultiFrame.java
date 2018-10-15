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
package com.naver.mei.sdk.core.image.animated;

import com.naver.mei.sdk.core.image.compositor.type.FrameAlignment;
import com.naver.mei.sdk.core.image.compositor.type.SizeOptions;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.FrameMeta;

import java.util.List;

/**
 * Created by GTPark on 2016-10-27.
 */

public class MultiFrame {
	public final List<FrameMeta> frameMetas;
	public final FrameAlignment frameAlignment;
	public final int width;
	public final int height;

	public MultiFrame(List<FrameMeta> frameMetas, SizeOptions sizeOptions) {
		this.frameMetas = frameMetas;
		this.frameAlignment = FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP;
		Composable.RectSize rectSize = sizeOptions.strategy.determine(frameMetas);
		this.width = rectSize.width;
		this.height= rectSize.height;
	}

	public MultiFrame(List<FrameMeta> frameMetas, int width, int height) {
		this.frameMetas = frameMetas;
		this.frameAlignment = FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP;
		this.width = width;
		this.height = height;
	}
}
