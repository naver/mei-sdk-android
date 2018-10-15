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
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.core.utils.URIUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;

/**
 * Created by GTPark on 2016-10-12.
 */

public class MeiImageProcessor {
	public static byte[] bitmapToBytes(Bitmap bitmap) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		return baos.toByteArray();
	}

	public static Bitmap rotate(Bitmap bitmap, float degree) {
		if (degree == 0) return bitmap;

		Matrix matrix = new Matrix();
		matrix.postRotate(degree);

		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

	// TODO GTPARk. jpegResize library가 성능이 매우 떨어져 일단 사용 보류
	public static Bitmap resize(Bitmap bitmap, double ratio) {
		return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * ratio), (int) (bitmap.getHeight() * ratio), true);
//		Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * ratio), (int) (bitmap.getHeight() * ratio), true);
//		JpegResize.lanczosResizeBitmap(bitmap, resizedBitmap);
//		return resizedBitmap;
	}

	public static Bitmap resize(Bitmap bitmap, int targetWidth, int targetHeight) {
		return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
//		Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
//		JpegResize.lanczosResizeBitmap(bitmap, resizedBitmap);
//		return resizedBitmap;
	}

	public static Bitmap decodeAndResize(byte[] bytesImage, double resizeRatio) {
		BitmapFactory.Options boundsOptions = getImageBoundsOptions(bytesImage);

		int targetWidth = (int) (boundsOptions.outWidth * resizeRatio);
		int targetHeight = (int) (boundsOptions.outHeight * resizeRatio);

		return decodeAndResize(bytesImage, targetWidth, targetHeight);
	}

	public static Bitmap decodeAndResize(byte[] bytesImage, int targetWidth, int targetHeight) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(bytesImage, 0, bytesImage.length,
				getResizeOptions(getImageBoundsOptions(bytesImage), targetWidth, targetHeight));
		return resize(bitmap, targetWidth, targetHeight);
	}

	public static Bitmap decodeAndResize(URI uri, int targetWidth, int targetHeight) {
		return decodeAndResize(URIUtils.uriToPath(uri), targetWidth, targetHeight);
	}

	public static Bitmap decodeAndResize(String path, int targetWidth, int targetHeight) {
		BitmapFactory.Options options = getResizeOptions(getImageBoundsOptions(path), targetWidth, targetHeight);
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
		return resize(bitmap, targetWidth, targetHeight);
	}

	/**
	 *
	 * @param uri 이미지 URI
	 * @param maxWidth 해당 이미지가 디코딩될 최대 width
	 * @param maxHeight 해당 이미지가 디코딩될 최대 height
	 * @return
	 */
	public static Bitmap decodeAndAutoRotate(URI uri, int maxWidth, int maxHeight) {
		int orientationDegree = MeiImageProcessor.getImageOrientationDegree(uri);
		BitmapFactory.Options originOptions = getImageBoundsOptions(uri);
		int rMaxWidth = orientationDegree == 0 || orientationDegree == 180 ? maxWidth : maxHeight;
		int rMaxHeight = orientationDegree == 0 || orientationDegree == 180 ? maxHeight : maxWidth;
		BitmapFactory.Options targetOptions = getResizeOptions(originOptions.outWidth, originOptions.outHeight, rMaxWidth, rMaxHeight, false);
		return MeiImageProcessor.rotate(BitmapFactory.decodeFile(URIUtils.uriToPath(uri), targetOptions), orientationDegree);
	}

	public static BitmapFactory.Options getImageBoundsOptions(byte[] bytesImage) {
		BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
		boundsOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(bytesImage, 0, bytesImage.length, boundsOptions);
		return boundsOptions;
	}

	public static BitmapFactory.Options getImageBoundsOptions(URI uri) {
		BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
		boundsOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(new File(uri).getAbsolutePath(), boundsOptions);
		return boundsOptions;
	}

	public static BitmapFactory.Options getImageBoundsOptions(String path) {
		BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
		boundsOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(new File(path).getAbsolutePath(), boundsOptions);
		return boundsOptions;
	}

	/**
	 * 이미지 파일을 목표 이미지 크기에 근접한 형태로 리사이즈하여 로드할 수 있도록 비트맵 옵션을 생성한다.
	 * 기본적으로 목표 가로세로 크기와 딱 맞거나 이보다는 크도록 디코딩된다.
	 */
	public static BitmapFactory.Options getResizeOptions(BitmapFactory.Options sourceOptions, int targetWidth, int targetHeight) {
		return getResizeOptions(sourceOptions.outWidth, sourceOptions.outHeight, targetWidth, targetHeight);
	}

	public static BitmapFactory.Options getResizeOptions(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
		return getResizeOptions(sourceWidth, sourceHeight, targetWidth, targetHeight, true);
	}

	public static BitmapFactory.Options getResizeOptions(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, boolean isBiggerThanTarget) {
		int samplingFactor = 2;

		while (sourceWidth / samplingFactor >= targetWidth && sourceHeight / samplingFactor >= targetHeight) {
			samplingFactor *= 2;
		}

		BitmapFactory.Options resizeOptions = new BitmapFactory.Options();
		resizeOptions.inSampleSize = samplingFactor / (isBiggerThanTarget ? 2 : 1);
		return resizeOptions;
	}


	public static int getImageOrientationDegree(URI uri) {
		if (!URIUtils.isLocalFileUri(uri.toString())) return 0;
		String path = URIUtils.uriToPath(uri);
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
			MeiLog.e("failed to check image orientaiton", ex);
			return 0;
		}
	}

	// Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
	public static int getImageOrientationDegree(byte[] jpeg) {
		if (jpeg == null) {
			return 0;
		}

		int offset = 0;
		int length = 0;

		// ISO/IEC 10918-1:1993(E)
		while (offset + 3 < jpeg.length && (jpeg[offset++] & 0xFF) == 0xFF) {
			int marker = jpeg[offset] & 0xFF;

			// Check if the marker is a padding.
			if (marker == 0xFF) {
				continue;
			}
			offset++;

			// Check if the marker is SOI or TEM.
			if (marker == 0xD8 || marker == 0x01) {
				continue;
			}
			// Check if the marker is EOI or SOS.
			if (marker == 0xD9 || marker == 0xDA) {
				break;
			}

			// Get the length and check if it is reasonable.
			length = pack(jpeg, offset, 2, false);
			if (length < 2 || offset + length > jpeg.length) {
				return 0;
			}

			// Break if the marker is EXIF in APP1.
			if (marker == 0xE1 && length >= 8 &&
					pack(jpeg, offset + 2, 4, false) == 0x45786966 &&
					pack(jpeg, offset + 6, 2, false) == 0) {
				offset += 8;
				length -= 8;
				break;
			}

			// Skip other markers.
			offset += length;
			length = 0;
		}

		// JEITA CP-3451 Exif Version 2.2
		if (length > 8) {
			// Identify the byte order.
			int tag = pack(jpeg, offset, 4, false);
			if (tag != 0x49492A00 && tag != 0x4D4D002A) {
				return 0;
			}
			boolean littleEndian = (tag == 0x49492A00);

			// Get the offset and check if it is reasonable.
			int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
			if (count < 10 || count > length) {
				return 0;
			}
			offset += count;
			length -= count;

			// Get the count and go through all the elements.
			count = pack(jpeg, offset - 2, 2, littleEndian);
			while (count-- > 0 && length >= 12) {
				// Get the tag and check if it is orientation.
				tag = pack(jpeg, offset, 2, littleEndian);
				if (tag == 0x0112) {
					// We do not really care about type and count, do we?
					int orientation = pack(jpeg, offset + 8, 2, littleEndian);
					switch (orientation) {
						case 1:
							return 0;
						case 3:
							return 180;
						case 6:
							return 90;
						case 8:
							return 270;
					}
					return 0;
				}
				offset += 12;
				length -= 12;
			}
		}

		return 0;
	}

	private static int pack(byte[] bytes, int offset, int length,
	                        boolean littleEndian) {
		int step = 1;
		if (littleEndian) {
			offset += length - 1;
			step = -1;
		}

		int value = 0;
		while (length-- > 0) {
			value = (value << 8) | (bytes[offset] & 0xFF);
			offset += step;
		}
		return value;
	}
}
