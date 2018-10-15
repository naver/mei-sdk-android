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
package com.naver.mei.sdk.error;

import android.util.Log;

/**
 * Created by tigerbaby on 2016-10-26.
 */

public class MeiLog {
	private static final String TAG = "MEI-SDK";

	public static void v(String message) {
		Log.v(TAG, message);
	}

	public static void v(String message, Throwable throwable) {
		Log.v(TAG, message, throwable);
	}

	public static void d(String message) {
		Log.d(TAG, message);
	}

	public static void d(String message, Throwable throwable) {
		Log.d(TAG, message, throwable);
	}

	public static void i(String message) {
		Log.i(TAG, message);
	}

	public static void i(String message, Throwable throwable) {
		Log.i(TAG, message, throwable);
	}

	public static void e(String message) {
		Log.e(TAG, message);
	}

	public static void e(String message, Throwable throwable) {
		Log.e(TAG, message, throwable);
	}
}
