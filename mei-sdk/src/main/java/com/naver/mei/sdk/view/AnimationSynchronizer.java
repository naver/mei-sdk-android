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
package com.naver.mei.sdk.view;

import com.naver.mei.sdk.core.image.meta.PlayDirection;

/**
 * Created by GTPark on 2016-10-23.
 * MeiImageView의 애니메이션 동기화를 지원하는 클래스
 * 서로 다른 MeiImageView 인스턴스 간의 애니메이션 러닝 동기화를 지원한다.
 * getAnimationTime을 통해 특정 시점에 MeiImageView가 노출해야할 프레임을 결정할 수 있다.
 */

public class AnimationSynchronizer {
	private long startTimeMillis = -1;
	private int backgroundDuration = 0;
	private int maxDuration = Integer.MAX_VALUE;
	private AnimationSynchronizeStrategy synchronizeStrategy;
	private double speedRatio = 1.0;

	public AnimationSynchronizer() {
		this.synchronizeStrategy = new AnimationSynchronizeStrategy.iterativeForBase();  // default
	}

	public AnimationSynchronizer setBackgroundDuration(int backgroundDuration) {
		this.backgroundDuration = backgroundDuration;
		return this;
	}

	public void setSpeedRatio(double speedRatio) {
		this.speedRatio = speedRatio;
	}

	public double getSpeedRatio() {
		return speedRatio;
	}

	public AnimationSynchronizer setMaxDurationIfGreatThen(int candidateDuration) {
		if (maxDuration == Integer.MAX_VALUE) maxDuration = 0;
 		maxDuration = (Math.max(maxDuration, candidateDuration));
		return this;
	}

	public int getBaseDuration() {
		return backgroundDuration > 0 ? backgroundDuration : maxDuration;
	}

	public AnimationSynchronizer setAnimationSyncrhonizeStrategy(AnimationSynchronizeStrategy strategy) {
		this.synchronizeStrategy = strategy;
		return this;
	}

	public int getAnimationTime(int duration, PlayDirection playDirection) {
		if (startTimeMillis < 0) startTimeMillis = System.currentTimeMillis();
		int elapsedTime = (int)((System.currentTimeMillis() - startTimeMillis)  * speedRatio);
		int realDuration = (int)(duration * playDirection.durationMultiplier);
		int animationTime = synchronizeStrategy.getAnimationTime(elapsedTime, realDuration, getBaseDuration());

		switch (playDirection) {
			case REVERSE:
				return realDuration - animationTime;
			case BOOMERANG:
				return animationTime < duration ? animationTime : realDuration - animationTime;
			default : // forward or unknown direction
				return animationTime;
		}
	}
}
