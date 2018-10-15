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
package com.naver.mei.sdk.listener;

import com.naver.mei.sdk.error.MeiSDKErrorType;

/**
 * Created by tigerbaby on 2016-10-20.
 */

public interface MeiQueuingEventListener {
	void onSuccess(String resultFilePath);
	void onFail(MeiSDKErrorType errorType);
	void onStop(int total);
	void onFrameProgress(int current, int total);
}
