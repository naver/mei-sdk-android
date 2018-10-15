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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.naver.mei.sample.R;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by GTPark on 2016-03-17.
 * 샘플 커스텀 갤러리 액티비티
 * <p>
 * 일반 정적 이미지(JPEG, PNG 등)는 Fresco Cache를 그대로 활용한다.
 * 단, GIF는 스케일링이 정상적으로 지원되지 않으므로, 속도 저하 이슈가 있어 별도의 GIF_BITMAP 캐시를 유지하고 관리한다.
 */
public class GalleryRecyclerViewAdapter extends RecyclerView.Adapter<GalleryRecyclerViewAdapter.GalleryRecyclerViewHolder> {
	private static final int GIF_BITMAP_CACHE_SIZE = 50 * 1024 * 1024;
	private static final int RESIZE_WIDTH = 300;
	private static final int RESIZE_HEIGHT = 300;

	private List<GalleryItem> galleryItems;
	private OnItemClickListener onItemClickListener;


	private OnItemLongClickListener onItemLongClickListener;
	private LruCache<String, Bitmap> thumbnailCache;
	private ThreadPoolExecutor bitmapLoaderThreadPoolExecutor = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private Handler handler = new Handler();
	private BitmapFactory.Options decodeBoundOption;

	private List<Integer> selectedPositions;

	public interface OnItemClickListener {
		void onItemClick(View v, int position);
	}

	public interface OnItemLongClickListener {
		void onLongClick(View v, int position);
	}

	public GalleryRecyclerViewAdapter(Context context, List<GalleryItem> galleryItems, OnItemClickListener onItemClickListener, OnItemLongClickListener onItemLongClickListener) {
		this.galleryItems = galleryItems;
		this.onItemClickListener = onItemClickListener;
		this.onItemLongClickListener = onItemLongClickListener;
		this.thumbnailCache = new LruCache<String, Bitmap>(GIF_BITMAP_CACHE_SIZE) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
		this.selectedPositions = new ArrayList<>();

		decodeBoundOption = new BitmapFactory.Options();
		decodeBoundOption.inJustDecodeBounds = true;
	}

	@Override
	public GalleryRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View galleryItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery, parent, false);
		return new GalleryRecyclerViewHolder(galleryItemView);
	}

	@Override
	public void onBindViewHolder(final GalleryRecyclerViewHolder holder, final int position) {
		final GalleryItem galleryItem = galleryItems.get(position);
		Uri uri = Uri.parse(galleryItem.uri);
		holder.root.setTag(galleryItem);
		clearMediaTypeMark(holder);

		holder.root.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onItemClickListener.onItemClick(v, position);
			}
		});

		holder.root.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				onItemLongClickListener.onLongClick(v, position);
				return true;
			}
		});

		int order = selectedPositions.indexOf(position) + 1;

		if (order == 0) {
			holder.vgSelectedItemLayout.setVisibility(View.INVISIBLE);
		} else {
			holder.tvSelectedItemOrder.setText(String.valueOf(order));
			holder.vgSelectedItemLayout.setVisibility(View.VISIBLE);
		}

		ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
				.setAutoRotateEnabled(true)
				.setLocalThumbnailPreviewsEnabled(true)
				.setResizeOptions(new ResizeOptions(RESIZE_WIDTH, RESIZE_HEIGHT))
				.setImageDecodeOptions(ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).setDecodeAllFrames(false).build())
				.setCacheChoice(ImageRequest.CacheChoice.SMALL)
				.build();

		if (galleryItem.getMediaDetailType() == GalleryItem.MediaDetailType.IMAGE_GIF) {
			holder.ivGalleryItem.setVisibility(View.INVISIBLE);
			holder.ivGifView.setVisibility(View.VISIBLE);
			holder.ivGifView.setImageBitmap(null);
			loadThumbnail(holder, galleryItem, position);
			setMediaTypeMark(holder, galleryItem);
		} else {
			holder.ivGalleryItem.setVisibility(View.VISIBLE);
			holder.ivGifView.setVisibility(View.INVISIBLE);
			DraweeController controller = Fresco.newDraweeControllerBuilder()
					.setImageRequest(request)
					.setAutoPlayAnimations(false)
					.setControllerListener(new BaseControllerListener<ImageInfo>() {
						@Override
						public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
							super.onFinalImageSet(id, imageInfo, animatable);
							setMediaTypeMark(holder, galleryItem);
						}

						@Override
						public void onFailure(String id, Throwable throwable) {
							galleryItems.remove(position);
							notifyDataSetChanged();
						}
					})
					.build();
			holder.ivGalleryItem.setBackground(null);
			holder.ivGalleryItem.setController(controller);
		}
	}

	private void loadThumbnail(final GalleryRecyclerViewHolder holder, final GalleryItem galleryItem, final int position) {
		final String uri = galleryItem.uri;

		holder.ivGifView.setImageBitmap(null);
		Bitmap thumbnailBitmap = thumbnailCache.get(uri);
		if (thumbnailBitmap != null) {
			holder.ivGifView.setImageBitmap(thumbnailBitmap);
			return;
		}

		bitmapLoaderThreadPoolExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if (position != holder.getLayoutPosition()) return;

				String path = URIUtils.uriStrToPath(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 8;
				Bitmap thumbnail = galleryItem.getMediaDetailType() == GalleryItem.MediaDetailType.VIDEO ?
						ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND) :
						BitmapFactory.decodeFile(URIUtils.uriStrToPath(uri), options);

				if (thumbnail != null) {
					thumbnailCache.put(uri, thumbnail);
				}

				handler.post(new Runnable() {
					@Override
					public void run() {
						if (position != holder.getLayoutPosition()) return;
						holder.ivGifView.setImageBitmap(thumbnailCache.get(uri));
					}
				});
			}
		});
	}

	private void clearMediaTypeMark(GalleryRecyclerViewHolder holder) {
		holder.ivVideoMark.setVisibility(View.INVISIBLE);
		holder.ivGifMark.setVisibility(View.INVISIBLE);
	}

	private void setMediaTypeMark(final GalleryRecyclerViewHolder holder, GalleryItem galleryItem) {
		GalleryItem.MediaDetailType mediaDetailType = galleryItem.getMediaDetailType();
		holder.ivVideoMark.setVisibility(galleryItem.mediaType == GalleryItem.MediaType.VIDEO ? View.VISIBLE : View.INVISIBLE);
		holder.ivGifMark.setVisibility(mediaDetailType == GalleryItem.MediaDetailType.IMAGE_GIF ? View.VISIBLE : View.INVISIBLE);
		holder.ivGifView.setColorFilter(galleryItem.getMediaDetailType() == GalleryItem.MediaDetailType.IMAGE_GIF ? Color.parseColor("#991C1F23") : Color.TRANSPARENT);
	}

	@Override
	public int getItemCount() {
		return galleryItems.size();
	}

	void clickItem(int position) {
		int index = selectedPositions.indexOf(position);
		if (index >= 0) {
			selectedPositions.remove(index);
		} else {
			selectedPositions.add(position);
		}

		notifyDataSetChanged();
	}

	List<Integer> getSelectedPositions() {
		return selectedPositions;
	}

	void clearSelectedPosition() {
		selectedPositions.clear();
		notifyDataSetChanged();
	}

	GalleryItem getGalleryItem(int index) {
		return galleryItems.get(index);
	}

	List<GalleryItem> getGalleryItems() {
		return galleryItems;
	}

	class GalleryRecyclerViewHolder extends RecyclerView.ViewHolder {
		View root;
		@BindView(R.id.gallery_image_view)
		SimpleDraweeView ivGalleryItem;
		@BindView(R.id.gallery_gif_view)
		ImageView ivGifView;
		@BindView(R.id.gif_mark)
		ImageView ivGifMark;
		@BindView(R.id.video_mark)
		ImageView ivVideoMark;
		@BindView(R.id.gallery_selected_layout)
		ViewGroup vgSelectedItemLayout;
		@BindView(R.id.gallery_selected_order)
		TextView tvSelectedItemOrder;

		GalleryRecyclerViewHolder(View galleryItemView) {
			super(galleryItemView);
			root = galleryItemView;
			ButterKnife.bind(this, galleryItemView);
		}
	}
}
