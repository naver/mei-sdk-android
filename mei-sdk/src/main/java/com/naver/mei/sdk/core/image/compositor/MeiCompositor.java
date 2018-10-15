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
package com.naver.mei.sdk.core.image.compositor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.naver.mei.sdk.core.common.ProgressCallback;
import com.naver.mei.sdk.core.gif.encoder.AnimatedGifEncoder;
import com.naver.mei.sdk.core.image.compositor.element.AnimatedElement;
import com.naver.mei.sdk.core.image.compositor.element.CompositionElement;
import com.naver.mei.sdk.core.image.compositor.strategy.BackgroundFirstDurationStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.DurationStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.FramePickStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.FrameRateStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.IterativeFramePickStrategy;
import com.naver.mei.sdk.core.image.compositor.strategy.SmoothFrameRateStrategy;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.MetaRealizer;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-10-12.
 */

public class MeiCompositor {
	private MetaRealizer realizer;

	private DurationStrategy durationStrategy;
	private FrameRateStrategy frameRateStrategy;
	private FramePickStrategy framePickStrategy;

	private double speedRatio = 1.0;

	long startTime = System.currentTimeMillis();

	// must construct by newBuilder method
	private MeiCompositor(double resizeRatio, DurationStrategy durationStrategy, FrameRateStrategy frameRateStrategy, FramePickStrategy framePickStrategy) {
		this.realizer = new MetaRealizer(resizeRatio);
		this.durationStrategy = durationStrategy;
		this.frameRateStrategy = frameRateStrategy;
		this.framePickStrategy = framePickStrategy;
	}


	public static Builder newBuilder() {
		return new Builder();
	}

	public MeiCompositor speedRatio(double speedRatio) {
		this.speedRatio = speedRatio;
		return this;
	}

//	public ImageType composite(List<Composable> metas) throws Exception {
//		return composite(metas, new FileOutputStream(MeiFileUtils.getTemporaryUniquePath()), null);
//	}

	/**
	 * @param metas        Composable metas
	 * @param outputStream outputStream
	 * @param callback     ProgressCallback
	 * @return isGif
	 */
	public ImageType composite(List<Composable> metas, OutputStream outputStream, ProgressCallback callback) {
		if (MeiIOUtils.isStorageSpaceFull()) {
			throw new MeiSDKException(MeiSDKErrorType.STORAGE_SPACE_FULL);
		}

		startTime = System.currentTimeMillis();
		List<CompositionElement> compositionElements = realize(metas, callback);
		List<AnimatedElement> animatedElements = filterAnimatableElements(compositionElements);

		if (animatedElements.size() == 0) {
			compositeImage(compositionElements, outputStream, callback);
			return ImageType.JPEG;
		} else {
			compositeGif(compositionElements, animatedElements, outputStream, callback);
			return ImageType.GIF;
		}
	}

	private void compositeImage(List<CompositionElement> compositionElements, OutputStream outputStream, ProgressCallback callback) {
		Bitmap compositedResult = Bitmap.createBitmap(compositionElements.get(0).width, compositionElements.get(0).height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(compositedResult);
		drawToCanvas(canvas, compositionElements);
		callback.onProgressComposition(1.0);
		compositedResult.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
	}

	private void compositeGif(List<CompositionElement> compositionElements, List<AnimatedElement> animatedElements, OutputStream outputStream, ProgressCallback callback) {
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(outputStream);
		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		encoder.setQuality(10);
		encoder.setMapQuality(7);
		encoder.setRepeat(0);    // infinity repeat
		encoder.start(bos);        // assign output stream

		long startTime = System.currentTimeMillis();
		int duration = durationStrategy.calculate(compositionElements.get(0), animatedElements);
		List<Integer> frameTimestamps = frameRateStrategy.calculate(animatedElements, duration, speedRatio);
		int count = 0;
		int runTime = 0;
		int backgroundWidth = compositionElements.get(0).width;
		int backgroundHeight = compositionElements.get(0).height;

		for (int timestamp : frameTimestamps) {
			Bitmap compositedResult = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(compositedResult);
			canvas.drawColor(Color.TRANSPARENT);

			if (callback != null)
				callback.onProgressComposition(count / (double) frameTimestamps.size());

			int delay = timestamp - runTime;

			drawToCanvas(canvas, compositionElements, duration, timestamp);

			encoder.setDelay((int) (delay / speedRatio));
			encoder.addFrame(compositedResult);
			runTime = timestamp;
			++count;
		}
		encoder.finish();

		MeiLog.d("total composite time : " + (System.currentTimeMillis() - startTime));
		IOUtils.closeQuietly(bos);
	}

	private List<CompositionElement> realize(List<Composable> metas, ProgressCallback callback) {
		List<CompositionElement> elements = new ArrayList<>();
		int metaCount = metas.size();
		for (int i = 0; i < metaCount; ++i) {
			Composable meta = metas.get(i);
			elements.add(realizer.parse(meta));
			if (callback != null) callback.onProgressLoadingResource(i / (double) metaCount);
		}
		return elements;
	}

	private List<AnimatedElement> filterAnimatableElements(List<CompositionElement> compositionElements) {
		ArrayList<AnimatedElement> animatedElements = new ArrayList<>();
		for (CompositionElement compositionElement : compositionElements) {
			if (!(compositionElement instanceof AnimatedElement)) continue;

			animatedElements.add((AnimatedElement) compositionElement);
		}
		return animatedElements;
	}

	private void drawToCanvas(Canvas canvas, List<CompositionElement> compositionElements) {
		drawToCanvas(canvas, compositionElements, 0, 0);
	}

	private void drawToCanvas(Canvas canvas, List<CompositionElement> compositionElements, int duration, int timestamp) {
		for (CompositionElement compositionElement : compositionElements) {
			Bitmap bitmap = framePickStrategy.pick(compositionElement, timestamp, duration);
			Bitmap rotateBitmap = MeiImageProcessor.rotate(bitmap, compositionElement.degree);
			int dx = -(rotateBitmap.getWidth() - bitmap.getWidth()) / 2;
			int dy = -(rotateBitmap.getHeight() - bitmap.getHeight()) / 2;
			dx += (compositionElement.width - bitmap.getWidth()) / 2;
			dy += (compositionElement.height - bitmap.getHeight()) / 2;

			canvas.drawBitmap(rotateBitmap, compositionElement.left + dx, compositionElement.top + dy, null);
		}
	}

	public static class Builder {
		private DurationStrategy durationStrategy;
		private FrameRateStrategy frameRateStrategy;
		private FramePickStrategy framePickStrategy;
		private double resizeRatio = 1.0;

		public Builder durationStrategy(DurationStrategy durationStrategy) {
			this.durationStrategy = durationStrategy;
			return this;
		}

		public Builder frameRateStrategy(FrameRateStrategy frameRateStrategy) {
			this.frameRateStrategy = frameRateStrategy;
			return this;
		}

		public Builder framePickStrategy(FramePickStrategy framePickStrategy) {
			this.framePickStrategy = framePickStrategy;
			return this;
		}

		public MeiCompositor build(int editingAreaWidth, int targetWidth) {
			if (this.durationStrategy == null)
				this.durationStrategy = new BackgroundFirstDurationStrategy();
			if (this.frameRateStrategy == null)
				this.frameRateStrategy = new SmoothFrameRateStrategy();
			if (this.framePickStrategy == null)
				this.framePickStrategy = new IterativeFramePickStrategy();

			return new MeiCompositor(targetWidth / (double) editingAreaWidth, this.durationStrategy, this.frameRateStrategy, this.framePickStrategy);
		}
	}

	public enum ImageType {
		GIF("gif"),
		JPEG("jpg");

		public final String extension;

		ImageType(String extension) {
			this.extension = extension;
		}
	}
}
