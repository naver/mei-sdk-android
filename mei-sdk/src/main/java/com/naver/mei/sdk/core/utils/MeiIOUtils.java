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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.naver.mei.sdk.MeiSDK;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URI;

/**
 * Created by GTPark on 2017-01-10.
 * uri to bytes 등 지원
 */

public class MeiIOUtils {
    public static byte[] getBytes(String uriStr) {
        return getBytes(URIUtils.uriStrToUri(uriStr));
    }

    public static byte[] getBytes(URI uri) {
        try {
            return URIUtils.isResourceURI(uri)
                    ? IOUtils.toByteArray(MeiSDK.getContext().getContentResolver().openInputStream(Uri.parse(uri.toString())))
                    : IOUtils.toByteArray(uri);
        } catch (IOException ex) {
            throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_IMAGE);
        }
    }

    public static long getAvailableBytes() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
    }

    public static boolean isStorageSpaceFull() {
        return getAvailableBytes() < MeiSDK.getMinimumStorageSpace();
    }

    public static void handleStorageSpaceCheck(Context context, final DialogInterface.OnClickListener onClickListener) {
        if (!isStorageSpaceFull()) return;

        new AlertDialog.Builder(context)
                .setMessage("저장 공간이 가득찼습니다.\n저장 공간을 비운 후 다시 시도해주세요.")
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onClickListener.onClick(dialog, which);
                    }
                })
                .create()
                .show();
    }
}
