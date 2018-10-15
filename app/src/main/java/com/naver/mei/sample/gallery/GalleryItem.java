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

import android.provider.MediaStore;

import com.naver.mei.sdk.core.utils.GIFUtils;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.io.Serializable;

/**
 * Created by GTPark on 2016-03-22.
 */
public class GalleryItem implements Serializable {
	public static final String INTENT_PARAM_KEY = "GalleryItem";
	public int id;
	public String uri;
	public MediaType mediaType;
	private MediaDetailType mediaDetailType;

	public GalleryItem(int id, String uri, MediaType mediaType) {
		this.id = id;
		this.uri = uri;
		this.mediaType = mediaType;
		this.mediaDetailType = null;
	}

	public GalleryItem(int id, String uri, MediaType mediaType, MediaDetailType mediaDetailType) {
		this.id = id;
		this.uri = uri;
		this.mediaType = mediaType;
		this.mediaDetailType = mediaDetailType;
	}

	public MediaDetailType getMediaDetailType() {
		// detail type은 필요에 의해 호출되며 (예: 일반 이미지와 GIF의 구분) 성능이점을 위해 lazy하게 값을 결정할 수 있다.
		if (mediaType == MediaType.VIDEO) {
			return MediaDetailType.VIDEO;
		}

		if (mediaDetailType == null) {
			mediaDetailType = GIFUtils.isGif(URIUtils.uriStrToPath(uri)) ? MediaDetailType.IMAGE_GIF : MediaDetailType.IMAGE_NORMAL;
		}

		return mediaDetailType;
	}

	public enum MediaType {
		IMAGE,	// GalleryHelper를 통해 media 정보를 불러올 때, 초기 로드 성능을 향상시키기 위해 GIF와 일반 이미지를 구분하지 않는다.
		VIDEO;

		public static MediaType from(int value) {
			switch (value) {
				case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
					return MediaType.IMAGE;
				case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
					return MediaType.VIDEO;
				default:
					return null;
			}
		}
	}

	public enum MediaDetailType {
		IMAGE_NORMAL,
		IMAGE_GIF,
		VIDEO
	}
}
