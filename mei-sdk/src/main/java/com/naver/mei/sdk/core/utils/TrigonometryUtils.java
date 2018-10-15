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
package com.naver.mei.sdk.core.utils;


import com.naver.mei.sdk.view.stickerview.StickerPoint;

/**
 * Created by Naver on 2016-09-21.
 */
public class TrigonometryUtils {
    public static final double PI = 3.14159265359;

    public static float getDistance(StickerPoint a, StickerPoint b) {
        float v = ((a.x - b.x) * (a.x - b.x)) + ((a.y - b.y) * (a.y - b.y));
        return ((int) (Math.sqrt(v) * 100)) / 100f;
    }

    public static StickerPoint getAnglePoint(StickerPoint O, StickerPoint A, float angle) {
        int x, y;
        float dOA = getDistance(O, A);
        double p1 = angle * PI / 180f;
        double p2 = Math.acos((A.x - O.x) / dOA);
        x = (int) (O.x + dOA * Math.cos(p1 + p2));

        double p3 = Math.acos((A.x - O.x) / dOA);
        y = (int) (O.y + dOA * Math.sin(p1 + p3));
        return new StickerPoint(x, y);
    }

    public static float getDegree(StickerPoint O, StickerPoint A, StickerPoint B) {
        double dOA = getDistance(O, A);
        double dOB = getDistance(O, B);
        double dAB = getDistance(A, B);

        double cosC = (Math.pow(dOA, 2) + Math.pow(dOB, 2) - Math.pow(dAB, 2)) / (2 * dOA * dOB);
        float degree = (float) Math.toDegrees(Math.acos(cosC));

        return degree;
    }

}
