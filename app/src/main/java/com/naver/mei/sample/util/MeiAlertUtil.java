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
package com.naver.mei.sample.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.naver.mei.sdk.MeiSDK;

/**
 * Created by tigerbaby on 2016-03-31.
 */
public class MeiAlertUtil {
	private static Toast lastToast = null;

	public static void show(Context context, String message, DialogInterface.OnClickListener onClickListener) {
		final AlertDialog alertDialog = createAlertDialog(context, message);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", onClickListener);
		alertDialog.show();
	}

	public static void show(Context context, int stringResourceId) {
		show(context, MeiSDK.getContext().getString(stringResourceId));
	}

	public static void show(Context context, String message) {
		show(context, message, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
	}

	public static void showThenFinish(final Activity activity, int stringResourceId) {
		showThenFinish(activity, MeiSDK.getContext().getString(stringResourceId));
	}

	public static void showThenFinish(final Activity activity, String message) {
		show(activity, message, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				activity.finish();
			}
		});
	}

	private static AlertDialog createAlertDialog(Context context, String message) {
		return new AlertDialog.Builder(context).setMessage(message).setCancelable(false).create();
	}

	public static void toast(int stringResourceId) {
		if (lastToast != null) lastToast.cancel();
		lastToast = Toast.makeText(MeiSDK.getContext(), stringResourceId, Toast.LENGTH_SHORT);
		lastToast.show();
	}

	public static void toast(String message) {
		if (lastToast != null) lastToast.cancel();
		lastToast = Toast.makeText(MeiSDK.getContext(), message, Toast.LENGTH_SHORT);
		lastToast.show();
	}

}
