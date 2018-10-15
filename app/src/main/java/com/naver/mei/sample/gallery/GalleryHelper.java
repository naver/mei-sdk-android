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
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.naver.mei.sample.MyApplication;
import com.naver.mei.sdk.core.utils.GIFUtils;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by GTPark on 2016-03-17.
 * 갤러리 관련 유틸성 클래스
 */
public class GalleryHelper {
	private static final Uri EXTERNAL_STORAGE_URI = MediaStore.Files.getContentUri("external");
	private static final String[] PATH_PROJECTION = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE};
	private static final String MEDIA_SELECTION =
			MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
					+ " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "= " + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

	private static final String ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";
	private static final int MAX_CACHE_SIZE = 100;
	private static GalleryHelper instance;
	private Map<String, String> thumbnailCache;

	public GalleryHelper() {
		thumbnailCache = new LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
			public boolean removeEldestEntry(Map.Entry eldest) {
				return size() > MAX_CACHE_SIZE;
			}
		};
	}

	public static GalleryHelper getInstance() {
		if (instance == null) {
			synchronized (GalleryHelper.class) {
				if (instance == null) {
					instance = new GalleryHelper();
				}
			}
		}
		return instance;
	}

	private static String getVideoThumbnail(Context context, GalleryItem galleryItem) {
		String thumbnailPath = galleryItem.uri;
		Uri thumbnail = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
		String[] projection = new String[]{MediaStore.Video.Thumbnails.DATA};
		String where = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";
		String[] whereArgs = new String[]{String.valueOf(galleryItem.id)};
		Cursor cursor = context.getContentResolver().query(thumbnail, projection, where, whereArgs, null);
		int colPath = cursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA);

		if (cursor != null) {
			if (cursor.moveToNext()) {
				thumbnailPath = cursor.getString(colPath);
			}
			cursor.close();
		}

		return thumbnailPath;
	}

	/**
	 * uri에 해당하는 이미지의 orientation 을 구한다.
	 * 반환 값은 degree 이다.
	 *
	 * @param uri 이미지의 uri
	 * @return orientation degree
	 */
	public static int getImageOrientationDegree(URI uri) {
		return getImageOrientationDegree(uri.toString());
	}

	public static int getImageOrientationDegree(String uri) {
		String path = URIUtils.uriStrToPath(uri);
		try {
			if (path == null) return 0;
			File imageFile = new File(path);
			ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					return 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_270:
					return 270;
			}
			return 0;
		} catch (Exception ex) {
			Log.e("MEI", "failed to check image orientaiton", ex);
			return 0;
		}
	}

	public List<GalleryItem> getGalleryItems() {
		return getGalleryItems(MediaSelectionMode.ALL);
	}

	public List<GalleryItem> getGalleryItems(MediaSelectionMode selectionMode) {
		List<GalleryItem> galleryItems = new ArrayList<>();
		Cursor cursor = MyApplication.context.getContentResolver().query(EXTERNAL_STORAGE_URI, PATH_PROJECTION, MEDIA_SELECTION, null, ORDER);

		if (cursor == null) return galleryItems;

		while (cursor.moveToNext()) {
			int id = cursor.getInt(0);
			String path = cursor.getString(1);
			int fileType = cursor.getInt(2);

			GalleryItem.MediaType mediaType = GalleryItem.MediaType.from(fileType);
			if (isValidPath(path) && selectionMode.isSuitableMediaType(mediaType, path)) {
				// selection mode가 image only인 경우 eager 하게 처리가능하도록 변경
				GalleryItem item = new GalleryItem(id, URIUtils.pathToUri(path).toString(), mediaType, selectionMode == MediaSelectionMode.IMAGE_ONLY
						? (GIFUtils.isGif(path) ? GalleryItem.MediaDetailType.IMAGE_GIF : GalleryItem.MediaDetailType.IMAGE_NORMAL)
						: null);
				galleryItems.add(item);
			}
		}

		cursor.close();

		return galleryItems;
	}

	private boolean isValidPath(String path) {
		return new File(path).canRead();
	}

	// Thumbnail 획득 참조용 코드로써 남겨둠
	// Thumbnail 획득 시 Overhead로 인해 ListView 버벅임 발생
	@Deprecated
	public String getThumbnail(Context context, GalleryItem galleryItem) {
		String thumbnailPath = thumbnailCache.get(galleryItem.uri);
		if (thumbnailPath != null) {
			return thumbnailPath;
		}
		long startTimestamp = System.currentTimeMillis();
		switch (galleryItem.mediaType) {
			case IMAGE:
				thumbnailPath = getImageThumbnail(context, galleryItem);
				break;
			case VIDEO:
				thumbnailPath = getVideoThumbnail(context, galleryItem);
				break;
			default:
				thumbnailPath = null;
				break;
		}

		thumbnailCache.put(galleryItem.uri, thumbnailPath);
		return thumbnailPath;
	}

	private String getImageThumbnail(Context context, GalleryItem galleryItem) {
		String thumbnailPath = galleryItem.uri;
		Uri thumbnail = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
		String[] projection = new String[]{MediaStore.Images.Thumbnails.DATA};
		String where = MediaStore.Images.Thumbnails.IMAGE_ID + "= ?";
		String[] whereArgs = new String[]{String.valueOf(galleryItem.id)};
		Cursor cursor = context.getContentResolver().query(thumbnail, projection, where, whereArgs, null);

		if (cursor != null) {
			if (cursor.moveToNext()) {
				thumbnailPath = cursor.getString(0);
			}
			cursor.close();
		}

		return thumbnailPath;
	}

	public enum MediaSelectionMode {
		IMAGE_WITHOUT_GIF,
		IMAGE_ONLY,
		VIDEO_ONLY,
		ALL;

		public static String INTENT_PARAM_KEY = "mediaSelectionMode";

		public boolean isSuitableMediaType(GalleryItem.MediaType mediaType, String path) {
			switch (this) {
				case ALL:
					return true;
				case VIDEO_ONLY:
					return mediaType == GalleryItem.MediaType.VIDEO;
				case IMAGE_ONLY:
					return mediaType == GalleryItem.MediaType.IMAGE;
				case IMAGE_WITHOUT_GIF:
					return mediaType == GalleryItem.MediaType.IMAGE && !GIFUtils.isGif(path);
				default:
					return false;
			}
		}
	}
}
