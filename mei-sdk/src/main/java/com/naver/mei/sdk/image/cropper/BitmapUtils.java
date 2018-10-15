package com.naver.mei.sdk.image.cropper;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import com.naver.mei.sdk.core.utils.URIUtils;

import java.io.File;

final class BitmapUtils {
    /**
     * 주어진 비율대로 비트맵을 crop
     * OOM으로 인해 crop이 실패 할 경우 충분히 작을 때까지 crop 이미지를 0.5 씩 확장
     */
    static BitmapSampled cropBitmapObjectHandleOOM(Bitmap bitmap, float[] points, int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        int scale = 1;
        while (true) {
            try {
                Bitmap cropBitmap = cropBitmapObjectWithScale(bitmap, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY, 1 / (float) scale);
                return new BitmapSampled(cropBitmap, scale);
            } catch (OutOfMemoryError e) {
                scale *= 2;
                if (scale > 8) {
                    throw e;
                }
            }
        }
    }

    /**
     * 주어진 비율과 scale로 crop
     *
     * @param scale crop 이미지 부분의 크기를 조정하려면 0.5를 사용하여 이미지를 절반으로 낮춤(OOM 처리)
     */
    private static Bitmap cropBitmapObjectWithScale(Bitmap bitmap, float[] points, int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY, float scale) {

        // crop 영역이 포함된 원본 이미지의 rect point를 가져옴
        Rect rect = getRectFromPoints(points, bitmap.getWidth(), bitmap.getHeight(), fixAspectRatio, aspectRatioX, aspectRatioY);

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postRotate(degreesRotated, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap result = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

        if (result == bitmap) {
            // 모든 비트맵이 crop 으로 선택될 경우
            result = bitmap.copy(bitmap.getConfig(), false);
        }

        return result;
    }

    /**
     * 주어진 배경 바운더리의 left 값 (x좌표)
     */
    static float getRectLeft(float[] points) {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * 주어진 배경 바운더리의 top 값 (y좌표)
     */
    static float getRectTop(float[] points) {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * 주어진 배경 바운더리의 right 값 (x좌표)
     */
    static float getRectRight(float[] points) {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * 주어진 배경 바운더리의 bottom 값 (y좌표)
     */
    static float getRectBottom(float[] points) {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
    }

    static float getRectWidth(float[] points) {
        return getRectRight(points) - getRectLeft(points);
    }

    static float getRectHeight(float[] points) {
        return getRectBottom(points) - getRectTop(points);
    }

    static float getRectCenterX(float[] points) {
        return (getRectRight(points) + getRectLeft(points)) / 2f;
    }

    static float getRectCenterY(float[] points) {
        return (getRectBottom(points) + getRectTop(points)) / 2f;
    }

    /**
     * 주어진 4점을 포함하는 rect를 설정 (x0, y0, x1, y1, x2, y2, x3, y3)
     */
    static Rect getRectFromPoints(float[] points, int imageWidth, int imageHeight, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        int left = Math.round(Math.max(0, getRectLeft(points)));
        int top = Math.round(Math.max(0, getRectTop(points)));
        int right = Math.round(Math.min(imageWidth, getRectRight(points)));
        int bottom = Math.round(Math.min(imageHeight, getRectBottom(points)));

        Rect rect = new Rect(left, top, right, bottom);
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
        }

        return rect;
    }

    /**
     * 비율 규칙에 맞게 rect를 수정
     */
    private static void fixRectForAspectRatio(Rect rect, int aspectRatioX, int aspectRatioY) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width();
            } else {
                rect.right -= rect.width() - rect.height();
            }
        }
    }

    /**
     * 비트맵과 load&crop 된 샘플 크기를 포함
     */
    static final class BitmapSampled {
        public final Bitmap bitmap;
        final int sampleSize;   // 비트맵의 사이즈를 낮추는데 사용되는 sample size

        BitmapSampled(Bitmap bitmap, int sampleSize) {
            this.bitmap = bitmap;
            this.sampleSize = sampleSize;
        }
    }

    public static int getImageOrientationDegree(String uri) {
        String path = URIUtils.uriStrToPath(uri);
        try {
            if (path == null) return 0;
            File imageFile = new File(path);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
            return 0;
        } catch (Exception ex) {
            Log.e("MEI", "failed to check image orientaiton", ex);
            return 0;
        }
    }
}