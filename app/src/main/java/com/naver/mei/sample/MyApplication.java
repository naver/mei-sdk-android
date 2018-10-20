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
package com.naver.mei.sample;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.SparseIntArray;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.memory.PoolParams;
import com.naver.mei.sdk.MeiSDK;

/**
 * Created by Naver on 2016-10-05.
 */
public class MyApplication extends Application {
	public static Context context;

	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();

		int maxRequestPerTime = 64;
		SparseIntArray defaultBuckets = new SparseIntArray();
		defaultBuckets.put(16 * ByteConstants.KB, maxRequestPerTime);
		PoolParams smallByteArrayPoolParams = new PoolParams(
				16 * ByteConstants.KB * maxRequestPerTime,
				2 * ByteConstants.MB,
				defaultBuckets);
		PoolFactory factory = new PoolFactory(
				PoolConfig.newBuilder()
						. setSmallByteArrayPoolParams(smallByteArrayPoolParams)
						.build());

		ImagePipelineConfig config = ImagePipelineConfig.newBuilder(context)
				.setDownsampleEnabled(true)
				.setPoolFactory(factory)
				.build();
		Fresco.initialize(context, config);
//		FLog.setMinimumLoggingLevel(FLog.VERBOSE);

		MeiSDK.init(context);
//		MeiSDK.setStorageDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/mei2/");
	}
}