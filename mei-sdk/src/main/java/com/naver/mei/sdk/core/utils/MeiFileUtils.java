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

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by tigerbaby on 2016-04-26.
 */
public class MeiFileUtils {
	public static final String DEFAULT_MEI_STORAGE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/mei/";
	private static final String TEMP_POSTFIX = "/temp/";
	public static final String EXTENSION_JPG = "jpg";
	public static final String EXTENSION_GIF = "gif";
	public static final String EXTENSION_MP4 = "mp4";
	private static String meiStorage;

	public static void init() {
		meiStorage = DEFAULT_MEI_STORAGE;
		makeStorageDir();
	}

	public static void setStorageDir(String storageDir) {
		File newStorageDir = new File(storageDir);

		if (!newStorageDir.exists()) {
			newStorageDir.mkdir();
		}

		if (newStorageDir.canRead() && newStorageDir.canWrite()) {
				if (!TextUtils.isEmpty(meiStorage) && !TextUtils.equals(storageDir, meiStorage)) {
					removeDir(meiStorage);
					removeDir(meiStorage + TEMP_POSTFIX);
				}

				meiStorage = storageDir;
		} else {
			newStorageDir.delete();
			MeiLog.e(MeiSDKErrorType.STORAGE_PATH_NOT_VALID.getMessage());
		}
	}

	/**
	 * @param extension file extension
	 * @return new file path
	 */
	public static String getUniquePath(String extension) {
		return getMeiDir() + getDateString() + "." + extension;
	}

	/**
	 * @param extension file extension
	 * @return new file path
	 */
	public static String getTemporaryUniquePath(String extension) {
		return getMeiTempDir() + getDateString() + "." + extension;
	}

	/**
	 * @param path dir path to clean
	 */
	public static void cleanDir(String path) {
		cleanDir(new File(path));
	}

	public static void broadcastNewMediaAdded(Uri uri) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(uri);
		MeiSDK.getContext().sendBroadcast(mediaScanIntent);
	}

	/**
	 * @param uri file uri
	 * @return file is valid or not
	 */
	public static boolean isExists(String uri) {
		File file = new File(URIUtils.uriStrToPath(uri));
		return file != null && file.length() > 0;
	}

	/**
	 * @param path file to delete
	 */
	public static void delete(String path) {
		File file = new File(path);

		if (file != null && file.exists()) {
			file.delete();
		}
	}

	/**
	 * @param filepath file to inspect
	 * @return file extension
	 */
	public static String getFileExtension(String filepath) {
		return filepath.substring(filepath.lastIndexOf(".") + 1);
	}

	/**
	 * @param bitmap target bitmap to create file
	 * @return file path
	 */
	public static String createFileFromBitmap(Bitmap bitmap) {
		return createFileFromBitmap(MeiFileUtils.getTemporaryUniquePath(MeiFileUtils.EXTENSION_JPG), bitmap);
	}

	private static String createFileFromBitmap(String filePath, Bitmap bitmap) {
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}

		OutputStream out = null;
		try {
			file.createNewFile();
			out = new FileOutputStream(file);

			bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
			broadcastNewMediaAdded(Uri.fromFile(file));
			return filePath;
		} catch (Exception e) {
			MeiLog.e("extract frame from video error", e);
			return null;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
	}

	public static boolean createFileFromBytes(String filePath, byte[] bytes) {
		FileOutputStream outputStream = null;

		try {
			File file = new File(filePath);
			if (file.exists())
				file.delete();

			outputStream = new FileOutputStream(filePath);
			outputStream.write(bytes);
			broadcastNewMediaAdded(Uri.fromFile(file));
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	/**
	 * @param file         oldFile
	 * @param newExtension file extension
	 * @return new file Path
	 */
	public static String changeExtension(File file, String newExtension) {
		String oldFileName = file.getName();
		int lastDotIndex = oldFileName.lastIndexOf(".");
		String newFileName = (lastDotIndex < 0 ? oldFileName : oldFileName.substring(0, lastDotIndex)) + "." + newExtension;
		File renamedFile = new File(file.getParent(), newFileName);
		file.renameTo(new File(file.getParent(), newFileName));
		return renamedFile.getAbsolutePath();
	}

	/**
	 * MEI Storage 용 Directory 를 생성한다.
	 */
	private static void makeStorageDir() {
		makeDirIfNotExists(getMeiDir());
		makeDirIfNotExists(getMeiTempDir());
	}

	private static void makeDirIfNotExists(String path) {
		File file = new File(path);

		if (!file.exists()) {
			file.mkdir();
		}
	}

	private static void cleanDir(File dir) {
		if (dir.isDirectory()) {
			File[] tempFiles = dir.listFiles();

			for (File file : tempFiles) {
				file.delete();
			}
		}
	}

	private static void removeDir(String path) {
		File dir = new File(path);
		cleanDir(dir);
		dir.delete();
	}

	private static String getDateString() {
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH); // Note: zero based!
		int day = now.get(Calendar.DAY_OF_MONTH);
		int hour = now.get(Calendar.HOUR_OF_DAY);
		int minute = now.get(Calendar.MINUTE);
		int second = now.get(Calendar.SECOND);
		int milliSecond = now.get(Calendar.MILLISECOND);

		return String.format(Locale.getDefault(), "%d%02d%02d_%02d%02d%02d_%03d", year, month + 1, day, hour, minute, second, milliSecond);
	}

	private static String getMeiDir() {
		makeDirIfNotExists(meiStorage);
		return meiStorage;
	}

	private static String getMeiTempDir() {
		String tempDir = getMeiDir() + TEMP_POSTFIX;
		makeDirIfNotExists(tempDir);
		return tempDir;
	}
}
