package com.naver.mei.sdk.core.image.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.net.URI;

/**
 * Created by GTPark on 2016-10-12.
 */

public class IOHelper {
	public static byte[] getImageBytes(URI uri) {
		try {
			return isResourceURI(uri)
					? IOUtils.toByteArray(MeiSDK.getContext().getContentResolver().openInputStream(Uri.parse(uri.toString())))
					: IOUtils.toByteArray(uri);
		} catch (Exception ex) {
			throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_IMAGE);
		}
	}

	private static boolean isResourceURI(URI uri) {
		return uri.getScheme().contains("resource");
	}

	public static Bitmap getBitmap(URI uri) {
		byte[] bytesImage = getImageBytes(uri);
		return BitmapFactory.decodeByteArray(bytesImage, 0, bytesImage.length);
	}

	public static Bitmap getBitmap(String uriStr) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (Exception ex) {
			throw new MeiSDKException(MeiSDKErrorType.INVALID_URI);
		}
		return getBitmap(uri);
	}
}
