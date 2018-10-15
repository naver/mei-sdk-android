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

import java.util.concurrent.TimeUnit;

/**
 * Created by tigerbaby on 2016-04-27.
 */
public class TimeCalculatorUtil {
	public static int getHoursFromMilliseconds(long milliseconds) {
		return (int) TimeUnit.MILLISECONDS.toHours(milliseconds);
	}

	public static int getMinutesFromMilliseconds(long milliseconds) {
		return (int) TimeUnit.MILLISECONDS.toMinutes(milliseconds) - (int) TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
	}

	public static long getSecondsFromMilliseconds(long milliseconds) {
		return (int) TimeUnit.MILLISECONDS.toSeconds(milliseconds) - (int) TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
	}
}
