package com.naver.mei.sdk.image.cropper;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

/**
 * crop window의 custom 옵션
 * default value도 이곳에서 초기화
 */
public class CropImageOptions implements Parcelable {

    public static final Creator<CropImageOptions> CREATOR = new Creator<CropImageOptions>() {
        @Override
        public CropImageOptions createFromParcel(Parcel in) {
            return new CropImageOptions(in);
        }

        @Override
        public CropImageOptions[] newArray(int size) {
            return new CropImageOptions[size];
        }
    };

    private static int MIN_CROP_RESULT_SIZE = 40;  // 픽셀단위
    private static int MAX_CROP_RESULT_SIZE = 99999;    // 픽셀단위
    private static int DEFAULT_COMPRESS_QUALITY = 90;

    public float snapRadius;    // snap 반경으로 경계에 닿았을때 붙는 효과를 줄 수 있음
    public float touchRadius;   // 핸들 주변 터치 가능 반지름(권장 48dp)
    public CropImageView.Guidelines guidelines;
    public float initialCropWindowPaddingRatio;     // crop window의 초기 padding(crop 할 이미지 사이즈의 백분율)
    public boolean fixAspectRatio;  // 가로 세로 비율을 유지할지 또는 자유롭게 변경할 지 여부
    public int aspectRatioX;
    public int aspectRatioY;
    public float borderLineThickness;
    public int borderLineColor;
    public float borderCornerThickness;
    public float borderCornerOffset;    // crop window 경계와 코너 라인의 간격 offset
    public float borderCornerLength;
    public int borderCornerColor;
    public float guidelinesThickness;
    public int guidelinesColor;
    public int backgroundColor; // crop window 바깥으로 원본 이미지를 덮는 배경색상
    public int minCropWindowWidth;
    public int minCropWindowHeight;
    public int minCropResultWidth;
    public int minCropResultHeight;
    public int maxCropResultWidth;
    public int maxCropResultHeight;
    public Uri outputUri;   // crop 된 이미지가 저장된 uri
    public Bitmap.CompressFormat outputCompressFormat;
    public int outputCompressQuality;  // (0 - 100)

    /**
     * 디폴트 옵션 세팅 : xml 선언이나 코드를 통해서 옵션 변경이 가능
     */
    public CropImageOptions() {

        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();

        snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm);
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm);
        guidelines = CropImageView.Guidelines.ON;
        initialCropWindowPaddingRatio = 0.1f;

        fixAspectRatio = false;
        aspectRatioX = 1;
        aspectRatioY = 1;

        borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm);
        borderLineColor = Color.WHITE;
        borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
        borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, dm);
        borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, dm);
        borderCornerColor = Color.WHITE;

        guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
        guidelinesColor = Color.WHITE;
        backgroundColor = Color.argb(119, 0, 0, 0);

        minCropWindowWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, dm);  // crop window의 최소 너비
        minCropWindowHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, dm); // crop window의 최소 높이
        minCropResultWidth = MIN_CROP_RESULT_SIZE;    // crop 된 결과 이미지의 최소 너비이며 crop window 크기 한도에 영향을 줌(픽셀 단위)
        minCropResultHeight = MIN_CROP_RESULT_SIZE;   // crop 된 결과 이미지의 최소 높이이며 crop window 크기 한도에 영향을 줌(픽셀 단위)
        maxCropResultWidth = MAX_CROP_RESULT_SIZE;     // crop 된 결과 이미지의 최대 너비이며 crop window 크기 한도에 영향을 줌(픽셀 단위)
        maxCropResultHeight = MAX_CROP_RESULT_SIZE;    // crop 된 결과 이미지의 최소 높이이며 crop window 크기 한도에 영향을 줌(픽셀 단위)

        outputUri = Uri.EMPTY;
        outputCompressFormat = Bitmap.CompressFormat.JPEG;
        outputCompressQuality = DEFAULT_COMPRESS_QUALITY;

    }

    /**
     * Create object from parcel.
     */
    protected CropImageOptions(Parcel in) {
        snapRadius = in.readFloat();
        guidelines = CropImageView.Guidelines.values()[in.readInt()];
        initialCropWindowPaddingRatio = in.readFloat();
        fixAspectRatio = in.readByte() != 0;
        aspectRatioX = in.readInt();
        aspectRatioY = in.readInt();
        borderLineThickness = in.readFloat();
        borderLineColor = in.readInt();
        borderCornerThickness = in.readFloat();
        borderCornerOffset = in.readFloat();
        borderCornerLength = in.readFloat();
        borderCornerColor = in.readInt();
        guidelinesThickness = in.readFloat();
        guidelinesColor = in.readInt();
        backgroundColor = in.readInt();
        minCropWindowWidth = in.readInt();
        minCropWindowHeight = in.readInt();
        minCropResultWidth = in.readInt();
        minCropResultHeight = in.readInt();
        maxCropResultWidth = in.readInt();
        maxCropResultHeight = in.readInt();
        outputUri = in.readParcelable(Uri.class.getClassLoader());
        outputCompressFormat = Bitmap.CompressFormat.valueOf(in.readString());
        outputCompressQuality = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(snapRadius);
        dest.writeInt(guidelines.ordinal());
        dest.writeFloat(initialCropWindowPaddingRatio);
        dest.writeByte((byte) (fixAspectRatio ? 1 : 0));
        dest.writeInt(aspectRatioX);
        dest.writeInt(aspectRatioY);
        dest.writeFloat(borderLineThickness);
        dest.writeInt(borderLineColor);
        dest.writeFloat(borderCornerThickness);
        dest.writeFloat(borderCornerOffset);
        dest.writeFloat(borderCornerLength);
        dest.writeInt(borderCornerColor);
        dest.writeFloat(guidelinesThickness);
        dest.writeInt(guidelinesColor);
        dest.writeInt(backgroundColor);
        dest.writeInt(minCropWindowWidth);
        dest.writeInt(minCropWindowHeight);
        dest.writeInt(minCropResultWidth);
        dest.writeInt(minCropResultHeight);
        dest.writeInt(maxCropResultWidth);
        dest.writeInt(maxCropResultHeight);
        dest.writeParcelable(outputUri, flags);
        dest.writeString(outputCompressFormat.name());
        dest.writeInt(outputCompressQuality);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 옵션 유효성 검사
     *
     * @throws IllegalArgumentException
     */
    public void validate() {
        if (initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_CROP_WINDOW_PADDING_RATIO);
        }
        if (aspectRatioX <= 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_ASPECT_RATIO);
        }
        if (aspectRatioY <= 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_ASPECT_RATIO);
        }
        if (borderLineThickness < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_LINE_THICKNESS);
        }
        if (borderCornerThickness < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_CORNER_THICKNESS);
        }
        if (guidelinesThickness < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_GUIDE_LINE_THICKNESS);
        }
        if (minCropWindowHeight < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_MIN_CROP_WINDOW_HEIGHT);
        }
        if (minCropResultWidth < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_MIN_CROP_RESULT_WIDTH);
        }
        if (minCropResultHeight < 0) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_MIN_CROP_RESULT_HEIGHT);
        }
        if (maxCropResultWidth < minCropResultWidth) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_MAX_CROP_RESULT_WIDTH);
        }
        if (maxCropResultHeight < minCropResultHeight) {
            throw new MeiSDKException(MeiSDKErrorType.INVALID_MAX_CROP_RESULT_HEIGHT);
        }
    }
}