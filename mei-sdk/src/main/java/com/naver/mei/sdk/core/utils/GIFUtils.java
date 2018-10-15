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

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * Created by Naver on 2016-10-10.
 */
public class GIFUtils {
    public static boolean isGif(byte[] bytes) {
        return isGifHeader(Arrays.copyOfRange(bytes, 0, 6));
    }

    public static boolean isGif(String path) {
        try {
            FileInputStream fis = new FileInputStream(new File(path));
            byte[] buffer = new byte[6];
            fis.read(buffer);
            fis.close();
            return isGifHeader(buffer);

        } catch (Exception ex) {
            Log.e("MEI", "GifUtils.isGif runtime error. path : " + path, ex);
            return false;   // gif 인지 판단을 실패한 경우 해당 리소스는 gif가 아니다.
        }
    }

    public static boolean isGifHeader(byte[] headerBuffer) {
        return new String(headerBuffer).matches("(?i)(GIF89a|GIF87a)");
    }
}
