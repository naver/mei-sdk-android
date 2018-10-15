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
package com.naver.mei.sdk.core.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.error.MeiLog;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;

/**
 * Created by Naver on 2016-10-10.
 */
public class URIUtils {
	public static URI uriStrToUri(String uriStr) {
		return URI.create(uriStr.replaceAll(" ", "%20"));
	}

	public static URI pathToUri(String path) {
		return new File(path).toURI();
	}

	public static String uriToPath(URI uri) {
		return uriStrToPath(uri.toString());
	}

	public static String uriStrToPath(String uri) {
		return isLocalFileUri(uri) ? decode(uriStrToUri(uri).getPath()) : "";
	}

	public static boolean isLocalFileUri(String uri) {
		if (!TextUtils.isEmpty(uri)) {
			return uri.startsWith("file:");
		}

		return false;
	}

	public  static boolean isResourceURI(String uri) {
		return isResourceURI(uriStrToUri(uri));
	}

	public static boolean isResourceURI(URI uri) {
		return uri.getScheme().contains("resource");
	}

	public static String resourceIdToURIStr(int resourceId) {
		Context context = MeiSDK.getContext();
		return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
				context.getResources().getResourcePackageName(resourceId) + '/' +
				context.getResources().getResourceTypeName(resourceId) + '/' +
				context.getResources().getResourceEntryName(resourceId);
	}

	private static String decode(String str) {
		try {
			return URLDecoder.decode(str, "utf-8");
		} catch (Exception ex) {
			MeiLog.e("failed to decode uri");
			return str;
		}
	}
}
