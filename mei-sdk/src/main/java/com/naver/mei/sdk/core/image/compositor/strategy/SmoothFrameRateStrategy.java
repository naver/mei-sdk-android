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
package com.naver.mei.sdk.core.image.compositor.strategy;

import com.naver.mei.sdk.core.image.compositor.element.AnimatedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GTPark on 2016-10-20.
 */

public class SmoothFrameRateStrategy implements FrameRateStrategy {
	public List<Integer> calculate(List<AnimatedElement> animatedElements, int duration, double speedRatio) {
		List<Integer> frameTimestamps = new ArrayList<>();
		int[] nextTimestampsByElements = new int[animatedElements.size()];
		int accumulateTimestamp = 0;
		int preAccumulateTimestamp = 0;

		while (accumulateTimestamp < duration) {
			updateNextTimestampByFrame(animatedElements, nextTimestampsByElements, accumulateTimestamp);
			accumulateTimestamp = Math.max(min(nextTimestampsByElements), preAccumulateTimestamp + (int)(MIN_DELAY * speedRatio));    // min term 0.06
			if (preAccumulateTimestamp == accumulateTimestamp) continue;

			frameTimestamps.add(accumulateTimestamp < duration ? accumulateTimestamp : duration);
			preAccumulateTimestamp = accumulateTimestamp;
		}

		if (frameTimestamps.size() == 0) {
			frameTimestamps.add(0);       // for bitmap only image
		}

		return frameTimestamps;
	}

	private int min(int[] accumulateTimestampByFrame) {
		int min = accumulateTimestampByFrame[0];
		for (int i = 1; i < accumulateTimestampByFrame.length; ++i) {
			if (min > accumulateTimestampByFrame[i]) min = accumulateTimestampByFrame[i];
		}
		return min;
	}

	private void updateNextTimestampByFrame(List<AnimatedElement> animatedElements, int[] nextTimestampsByFrame, int currentTimestamp) {
		for (int i = 0; i < animatedElements.size(); ++i) {
			if (nextTimestampsByFrame[i] > currentTimestamp) continue;
			nextTimestampsByFrame[i] += animatedElements.get(i).delay();
		}
	}
}