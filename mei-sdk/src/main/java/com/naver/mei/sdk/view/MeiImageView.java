
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
package com.naver.mei.sdk.view;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RawRes;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.core.image.animated.AnimatedGif;
import com.naver.mei.sdk.core.image.compositor.MeiImageProcessor;
import com.naver.mei.sdk.core.image.compositor.element.AnimatedMultiFrameElement;
import com.naver.mei.sdk.core.image.compositor.element.Frame;
import com.naver.mei.sdk.core.image.meta.Composable;
import com.naver.mei.sdk.core.image.meta.ComposableImage;
import com.naver.mei.sdk.core.image.meta.ComposableMultiFrame;
import com.naver.mei.sdk.core.image.meta.FrameMeta;
import com.naver.mei.sdk.core.image.meta.MetaRealizer;
import com.naver.mei.sdk.core.image.meta.PlayDirection;
import com.naver.mei.sdk.core.utils.LocalCache;
import com.naver.mei.sdk.core.utils.LocalFileCache;
import com.naver.mei.sdk.core.utils.LocalMemoryCache;
import com.naver.mei.sdk.core.utils.MeiIOUtils;
import com.naver.mei.sdk.core.utils.URIUtils;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * MeiSDK에서 MeiCanvasView와 함께 사용되는 ImageView
 * <p>
 * Animated GIF 지원
 * MultiFrame Image 지원
 * MeiImageView간 애니메이션 동기화 지원
 * <p>
 * Created by GTPark on 2016-10-21.
 */

public class MeiImageView extends ImageView {
	private static final int MIN_FRAME_DELAY = 40;  // 24fps
	private static final int PERFORMANCE_SAMPLING_FACTOR = 2;
	private AnimationSynchronizer animationSynchronizer;
	protected PlayDirection playDirection = PlayDirection.FORWARD;
	private ImageDrawSupporter imageDrawSupporter;
	private LocalFileCache localFileCache;
	private boolean decodeAccelerationWithoutTransparent = false;    // 투명 값이 없다면 JPEG으로 인/디코딩하여 display 성능 향상 가능
	private DisplayMetrics displayMetrics;
	private double originalAspectRatio;

	public MeiImageView(Context context) {
		super(context);
		init();
	}

	public MeiImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MeiImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public MeiImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		localFileCache = LocalFileCache.getInstance();
		displayMetrics = getContext().getResources().getDisplayMetrics();
	}

	/**
	 * 투명색 지원 여부를 지정해 성능 가속 여부를 설정한다.
	 * 투명색을 지원하지 않을 경우 내부적으로 JPEG 포맷을 사용하여 더욱 빠른 인/디코딩이 가능하게 된다.
	 *
	 * @param supportTransparent 투명색 지원 여부
	 */
	public void supportTransparent(boolean supportTransparent) {
		this.decodeAccelerationWithoutTransparent = !supportTransparent;
	}

	public void setPlayDirection(PlayDirection playDirection) {
		this.playDirection = playDirection;
	}

	public PlayDirection getPlayDirection() {
		return playDirection;
	}

	public void setImageResource(@RawRes int resId) {
		Context context = MeiSDK.getContext();
		Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
				context.getResources().getResourcePackageName(resId) + '/' +
				context.getResources().getResourceTypeName(resId) + '/' +
				context.getResources().getResourceEntryName(resId));

		setImageURI(uri);
	}

	public void setAnimationSynchronizer(AnimationSynchronizer synchronizer) {
		this.animationSynchronizer = synchronizer;
	}

	@Override
	public void setImageURI(Uri uri) {
		Movie movie;
		try {
			movie = Movie.decodeStream(getContext().getContentResolver().openInputStream(uri));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		Bitmap bitmap = getBitmap(uri);
		imageDrawSupporter = movie == null ? new SimpleImageDrawSupporter(uri) : new GifDrawSupporter(uri);
		originalAspectRatio = bitmap.getWidth() / (double) bitmap.getHeight();
		setImageBitmap(bitmap);
	}

	private Bitmap getBitmap(Uri uri) {
		if (!URIUtils.isResourceURI(uri.toString())) {
			return MeiImageProcessor.decodeAndAutoRotate(
					URIUtils.uriStrToUri(uri.toString()), displayMetrics.widthPixels, displayMetrics.heightPixels);
		}

		// resource uri
		InputStream inputStream = null;
		try {
			inputStream = getContext().getContentResolver().openInputStream(uri);
			return BitmapFactory.decodeStream(inputStream);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	public void setMultiFrame(ComposableMultiFrame multiFrame) {
		MultiFrameDrawSupporter multiFrameDrawSupporter = new MultiFrameDrawSupporter(multiFrame);
		imageDrawSupporter = multiFrameDrawSupporter;
		Bitmap bitmap = Bitmap.createBitmap(multiFrameDrawSupporter.getMultiFrameWidth(), multiFrameDrawSupporter.getMultiFrameHeight(), Bitmap.Config.ARGB_8888);
		originalAspectRatio = bitmap.getWidth() / (double) bitmap.getHeight();
		this.setImageBitmap(bitmap);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (animationSynchronizer == null) {
			animationSynchronizer = new AnimationSynchronizer();
		}

		imageDrawSupporter.draw(canvas);

		if (imageDrawSupporter instanceof SimpleImageDrawSupporter) return;

		postInvalidateDelayed(MIN_FRAME_DELAY);
	}

	public double getOriginalAspectRatio() {
		return originalAspectRatio;
	}
	public int getDuration() {
		return imageDrawSupporter.getDuration();
	}

	public Composable getComposable() {
		return imageDrawSupporter.getComposable(0, 0, 0, 0);
	}

	public Composable getComposable(int left, int top, int zIndex, float degree) {
		return imageDrawSupporter.getComposable(left, top, zIndex, degree);
	}

	interface ImageDrawSupporter {
		void draw(Canvas canvas);

		Composable getComposable(int left, int top, int zIndex, float degree);

		int getDuration();
	}

	abstract class UriBasedImageDrawSupporter implements ImageDrawSupporter {
		protected Uri imageUri;

		UriBasedImageDrawSupporter(Uri imageUri) {
			this.imageUri = imageUri;
		}

		@Override
		public Composable getComposable(int left, int top, int zIndex, float degree) {
			URI uri = URI.create(imageUri.toString());
			int orientationDegree = MeiImageProcessor.getImageOrientationDegree(uri);
			return new ComposableImage(uri, getWidth(), getHeight(), left, top, zIndex, degree, orientationDegree, playDirection);
		}
	}

	class SimpleImageDrawSupporter extends UriBasedImageDrawSupporter {

		SimpleImageDrawSupporter(Uri uri) {
			super(uri);
		}

		@SuppressLint("WrongCall")
		@Override
		public void draw(Canvas canvas) {
			MeiImageView.super.onDraw(canvas);
		}

		@Override
		public int getDuration() {
			return 0;
		}
	}

	class GifDrawSupporter extends UriBasedImageDrawSupporter {
		private int realDuration;
		private Uri uri;
		private AnimatedGif animatedGif;

		GifDrawSupporter(Uri uri) {
			super(uri);
			this.uri = uri;
			this.animatedGif = AnimatedGif.createInstanceWithMaxSize(
					MeiIOUtils.getBytes(uri.toString()),
					displayMetrics.widthPixels / PERFORMANCE_SAMPLING_FACTOR,
					displayMetrics.heightPixels / PERFORMANCE_SAMPLING_FACTOR);
			this.realDuration = animatedGif.getDuration();
		}


		@Override
		public void draw(Canvas canvas) {
			int animationTime = animationSynchronizer.getAnimationTime(realDuration, playDirection);
			Bitmap bitmap = getBitmap(animatedGif.findFrameByTimestamp(animationTime));
			canvas.scale(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
			canvas.drawColor(Color.TRANSPARENT);
			canvas.drawBitmap(bitmap, 0, 0, null);
		}

		private Bitmap getBitmap(int frameIndex) {
			String cacheKey = getCacheKey(frameIndex);
			Bitmap bitmap = LocalMemoryCache.getCommonInstance().getBitmap(cacheKey);
			if (bitmap == null) {
				bitmap = animatedGif.getFrame(frameIndex).bitmap;
				LocalMemoryCache.getCommonInstance()
						.put(cacheKey, bitmap, LocalCache.CompressFormat.getDefaultCompressFormat(!decodeAccelerationWithoutTransparent));

			}
			return bitmap;
		}

		private String getCacheKey(int frameIndex) {
			return uri.hashCode() + "_" + animatedGif.getWidth() + "_" + animatedGif.getHeight() + "_" + frameIndex;
		}

		public int getDuration() {
			return realDuration;
		}
	}

	class MultiFrameDrawSupporter implements ImageDrawSupporter {
		private ComposableMultiFrame multiFrame;
		private AnimatedMultiFrameElement multiFrameElement;

		MultiFrameDrawSupporter(ComposableMultiFrame composableMultiFrame) {
			this.multiFrame = composableMultiFrame;
			// image view가 resize될 것을 고려하여 초기화 시점에 meta resize를 수행하지 않음
			// 다만, OOM발생을 피하기 위해 화면 사이즈 미만으로 리사이즈 수행
			double resizeRatio = (multiFrame.width < displayMetrics.widthPixels / PERFORMANCE_SAMPLING_FACTOR
					? multiFrame.width : displayMetrics.widthPixels / PERFORMANCE_SAMPLING_FACTOR) / (double) multiFrame.width;

			multiFrameElement = (AnimatedMultiFrameElement) new MetaRealizer(resizeRatio).parse(multiFrame);
		}

		int getMultiFrameWidth() {
			return multiFrameElement.width;
		}

		int getMultiFrameHeight() {
			return multiFrameElement.height;
		}

		@Override
		public void draw(Canvas canvas) {
			int animationTime = animationSynchronizer.getAnimationTime(multiFrameElement.getDuration(), playDirection);
			int frameIndex = multiFrameElement.findFrameByTimestamp(animationTime);
			FrameMeta frameMeta = multiFrameElement.getFrameMeta(frameIndex);
			Bitmap bitmap = getFrameBitmap(frameIndex, frameMeta);
			double resizeRatio = canvas.getWidth() / (double) multiFrameElement.width;

			canvas.drawColor(Color.BLACK);  // default background color
			canvas.translate((int) (frameMeta.left * resizeRatio), (int) (frameMeta.top * resizeRatio));

			canvas.drawBitmap(bitmap,
					new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
					new Rect(0, 0, (int) (bitmap.getWidth() * resizeRatio), (int) (bitmap.getHeight() * resizeRatio)),
					null);
		}

		private Bitmap getFrameBitmap(int frameIndex, FrameMeta frameMeta) {
			String key = createCacheKey(frameMeta);

			Bitmap cachedBitmap = localFileCache.getBitmap(key);

			if (cachedBitmap != null) // cache hit
				return cachedBitmap;

			// cache miss
			Frame frame = multiFrameElement.getFrame(frameIndex);
			localFileCache.put(key, frame.bitmap);
			return frame.bitmap;
		}

		private String createCacheKey(FrameMeta frameMeta) {
			return (URIUtils.uriToPath(frameMeta.uri) + "_" + frameMeta.width + "_" + frameMeta.height).hashCode() + "";
		}

		@Override
		public Composable getComposable(int left, int top, int zIndex, float degree) {
			List<FrameMeta> frameMetas = new ArrayList<>();
			int imageViewWidth = MeiImageView.this.getWidth();
			int imageViewHeight = MeiImageView.this.getHeight();
			for (FrameMeta frameMeta : multiFrame.frameMetas) {
				frameMetas.add(frameMeta.resize(imageViewWidth / (double) multiFrame.width));
			}

			return new ComposableMultiFrame(frameMetas, imageViewWidth, imageViewHeight, left, top, zIndex, degree, playDirection);
		}

		@Override
		public int getDuration() {
			return multiFrameElement.getDuration();
		}
	}
}
