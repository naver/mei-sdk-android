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

import java.util.List;

/**
 * Created by GTPark on 2016-10-20.
 */

public interface FrameRateStrategy {
	int MIN_DELAY = 60;
	List<Integer> calculate(List<AnimatedElement> compositionElements, int duration, double speedRatio);
}
