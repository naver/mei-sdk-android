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
package com.naver.mei.sample.video;

import android.os.Handler;

import java.util.TimerTask;

/**
 * Created by tigerbaby on 2016-05-20.
 */
public class VideoTimerTask extends TimerTask {
	private static final int EMPTY_MESSAGE = 0;
	private Handler timerHandler;

	public VideoTimerTask(Handler handler) {
		this.timerHandler = handler;
	}
	@Override
	public void run() {
		this.timerHandler.sendEmptyMessage(EMPTY_MESSAGE);
	}
}
