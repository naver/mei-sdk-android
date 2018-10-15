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
package com.naver.mei.sdk.core.camera;

/**
 * Created by Naver on 2016-11-28.
 */

public enum CameraRatio {
    // 사용자 정의 비율   가로 = 너비
    THREE_BY_FOUR_USER(1.33f, "3:4"),
    FOUR_BY_THREE_USER(0.75f, "4:3"),

    // 카메라 기본정의 비율   가로 = 높이
    SIXTEEN_BY_NINE_CAM(1.78f, "16:9"),
    FOUR_BY_THREE_CAM(1.33f, "4:3"),

    // 공통사용 가능
    ONE_BY_ONE(1.0f, "1:1");

    private float value;
    private String text;

    CameraRatio(float value, String text) {
        this.value = value;
        this.text = text;
    }

    public float getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
