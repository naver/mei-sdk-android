package com.naver.mei.sdk.image.cropper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.naver.mei.sdk.R;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * crop 기능을 제공하는 custom view
 */
public class CropImageView extends FrameLayout {
    static final RectF RECT = new RectF();  // 일반적 내부 사용 용도의 재사용 가능한 rect
    static final float[] POINTS = new float[6]; // 일반적 내부 사용 용도의 재사용 가능한 point
    static final float[] POINTS2 = new float[6];

    private final ImageView mImageView;     // crop할 배경이미지뷰
    private final CropOverlayView mCropOverlayView;     // crop 영역 지정을 위한 view UI
    private final Matrix mImageMatrix = new Matrix();   // 이미지뷰에서 크롭된 이미지를 변환하는 데 사용되는 matrix
    private final Matrix mImageInverseMatrix = new Matrix();    // reverse matrix 사용을 위해 재사용하는 matrix
    private final float[] mImagePoints = new float[8];  // 이미지 matrix 변환 계산에 사용 된 사각형 (rect 인스턴스 재사용)
    private Bitmap mBitmap;
    private int mLayoutWidth;
    private int mLayoutHeight;
    private int mImageResource;
    private Uri mLoadedImageUri;
    private RectF mRestoreCropWindowRect;   // 상태 복원 후 crop 사각형을 복원하는 데 사용
    private int mDegreesRotated;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        CropImageOptions options = new CropImageOptions();

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);
            try {
                options.fixAspectRatio = ta.getBoolean(R.styleable.CropImageView_cropFixAspectRatio, options.fixAspectRatio);
                options.aspectRatioX = ta.getInteger(R.styleable.CropImageView_cropAspectRatioX, options.aspectRatioX);
                options.aspectRatioY = ta.getInteger(R.styleable.CropImageView_cropAspectRatioY, options.aspectRatioY);
                options.guidelines = Guidelines.values()[ta.getInt(R.styleable.CropImageView_cropGuidelines, options.guidelines.ordinal())];
                options.snapRadius = ta.getDimension(R.styleable.CropImageView_cropSnapRadius, options.snapRadius);
                options.initialCropWindowPaddingRatio = ta.getFloat(R.styleable.CropImageView_cropInitialCropWindowPaddingRatio, options.initialCropWindowPaddingRatio);
                options.borderLineThickness = ta.getDimension(R.styleable.CropImageView_cropBorderLineThickness, options.borderLineThickness);
                options.borderLineColor = ta.getInteger(R.styleable.CropImageView_cropBorderLineColor, options.borderLineColor);
                options.borderCornerThickness = ta.getDimension(R.styleable.CropImageView_cropBorderCornerThickness, options.borderCornerThickness);
                options.borderCornerOffset = ta.getDimension(R.styleable.CropImageView_cropBorderCornerOffset, options.borderCornerOffset);
                options.borderCornerLength = ta.getDimension(R.styleable.CropImageView_cropBorderCornerLength, options.borderCornerLength);
                options.borderCornerColor = ta.getInteger(R.styleable.CropImageView_cropBorderCornerColor, options.borderCornerColor);
                options.guidelinesThickness = ta.getDimension(R.styleable.CropImageView_cropGuidelinesThickness, options.guidelinesThickness);
                options.guidelinesColor = ta.getInteger(R.styleable.CropImageView_cropGuidelinesColor, options.guidelinesColor);
                options.backgroundColor = ta.getInteger(R.styleable.CropImageView_cropBackgroundColor, options.backgroundColor);
                options.borderCornerThickness = ta.getDimension(R.styleable.CropImageView_cropBorderCornerThickness, options.borderCornerThickness);
                options.minCropWindowWidth = (int) ta.getDimension(R.styleable.CropImageView_cropMinCropWindowWidth, options.minCropWindowWidth);
                options.minCropWindowHeight = (int) ta.getDimension(R.styleable.CropImageView_cropMinCropWindowHeight, options.minCropWindowHeight);
                options.minCropResultWidth = (int) ta.getFloat(R.styleable.CropImageView_cropMinCropResultWidthPX, options.minCropResultWidth);
                options.minCropResultHeight = (int) ta.getFloat(R.styleable.CropImageView_cropMinCropResultHeightPX, options.minCropResultHeight);
                options.maxCropResultWidth = (int) ta.getFloat(R.styleable.CropImageView_cropMaxCropResultWidthPX, options.maxCropResultWidth);
                options.maxCropResultHeight = (int) ta.getFloat(R.styleable.CropImageView_cropMaxCropResultHeightPX, options.maxCropResultHeight);

                // aspect ratio가 설정되어 있으면 fixed true
                if (ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) && ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) && !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio)) {
                    options.fixAspectRatio = true;
                }
            } finally {
                ta.recycle();
            }
        }

        options.validate();

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.crop_image_view, this, true);

        mImageView = (ImageView) v.findViewById(R.id.image_to_crop);
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);

        mCropOverlayView = (CropOverlayView) v.findViewById(R.id.crop_overlay_view);
        mCropOverlayView.setInitialAttributeValues(options);

    }

    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mCropOverlayView.setMinCropResultSize(minCropResultWidth, minCropResultHeight);

    }
    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mCropOverlayView.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight);
    }

    public boolean isFixAspectRatio() {
        return mCropOverlayView.isFixAspectRatio();
    }

    public void setFixedAspectRatio(boolean fixAspectRatio) {
        mCropOverlayView.setFixedAspectRatio(fixAspectRatio);
    }

    public Guidelines getGuidelines() {
        return mCropOverlayView.getGuidelines();
    }

    public void setGuidelines(Guidelines guidelines) {
        mCropOverlayView.setGuidelines(guidelines);
    }

    public Pair<Integer, Integer> getAspectRatio() {
        return new Pair<>(mCropOverlayView.getAspectRatioX(), mCropOverlayView.getAspectRatioY());
    }

    public void setAspectRatio(int aspectRatioX, int aspectRatioY) {
        mCropOverlayView.setAspectRatioX(aspectRatioX);
        mCropOverlayView.setAspectRatioY(aspectRatioY);
        setFixedAspectRatio(true);
    }

    public void clearAspectRatio() {
        mCropOverlayView.setAspectRatioX(1);
        mCropOverlayView.setAspectRatioY(1);
        setFixedAspectRatio(false);
    }

    /**
     * crop 윈도우가 지정한 가장자리 범위에 닿을때 snap 효과
     */
    public void setSnapRadius(float snapRadius) {
        if (snapRadius >= 0) {
            mCropOverlayView.setSnapRadius(snapRadius);
        }
    }

    public int getImageResource() {
        return mImageResource;
    }

    public Uri getImageUri() {
        return mLoadedImageUri;
    }

    /**
     * 원본 비트맵 기준으로 crop 윈도우의 위치를 반환
     */
    public Rect getCropRect() {
        if (mBitmap != null) {

            float[] points = getCropPoints();

            int orgWidth = mBitmap.getWidth();
            int orgHeight = mBitmap.getHeight();

            // crop point로 이루어진 직사각형
            return BitmapUtils.getRectFromPoints(points, orgWidth, orgHeight, mCropOverlayView.isFixAspectRatio(), mCropOverlayView.getAspectRatioX(), mCropOverlayView.getAspectRatioY());
        } else {
            return null;
        }
    }

    public float[] getCropPoints() {

        // 배경 이미지 기준으로 crop window를 가져옴
        RectF cropWindowRect = mCropOverlayView.getCropWindowRect();

        float[] points = new float[]{
                cropWindowRect.left,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.bottom,
                cropWindowRect.left,
                cropWindowRect.bottom
        };

        mImageMatrix.invert(mImageInverseMatrix);
        mImageInverseMatrix.mapPoints(points);

        return points;
    }


    public Bitmap getCroppedImage() {
        Bitmap croppedBitmap = null;
        if (mBitmap != null) {
            croppedBitmap = BitmapUtils.cropBitmapObjectHandleOOM(mBitmap, getCropPoints(), mDegreesRotated, mCropOverlayView.isFixAspectRatio(), mCropOverlayView.getAspectRatioX(), mCropOverlayView.getAspectRatioY()).bitmap;
        }

        return croppedBitmap;
    }

    public void rotateImage() {
        rotateImage(90);
    }

    public void setImageResource(int resId) {
        if (resId != 0) {
            mCropOverlayView.setInitialCropWindowRect(null);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            setBitmap(bitmap, resId);
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        mCropOverlayView.setInitialCropWindowRect(null);
        setBitmap(bitmap);
    }

    public void setImageUri(Uri uri) {
        int degreesRotated = BitmapUtils.getImageOrientationDegree(uri.toString());
        mCropOverlayView.setInitialCropWindowRect(null);
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
        } catch (FileNotFoundException e) {
            throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_IMAGE);
        } catch (IOException e) {
            throw new MeiSDKException(MeiSDKErrorType.FAILED_TO_LOAD_IMAGE);
        }
        setBitmap(bitmap, uri, degreesRotated);
    }

    private void setBitmap(Bitmap bitmap) {
        setBitmap(bitmap, 0, null, 0);
    }

    private void setBitmap(Bitmap bitmap, int imageResource) {
        setBitmap(bitmap, imageResource, null, 0);
    }

    private void setBitmap(Bitmap bitmap, Uri imageUri, int degreesRotated) {
        setBitmap(bitmap, 0, imageUri, degreesRotated);
    }

    /**
     * 자르기에 사용할 비트맵 설정
     */
    private void setBitmap(Bitmap bitmap, int imageResource, Uri imageUri, int degreesRotated) {
        if (mBitmap == null || !mBitmap.equals(bitmap)) {

            clearImageInt();

            mBitmap = bitmap;
            mImageView.setImageBitmap(mBitmap);

            mLoadedImageUri = imageUri;
            mImageResource = imageResource;
            mDegreesRotated = degreesRotated;

            applyImageMatrix(getWidth(), getHeight());

            if (mCropOverlayView != null) {
                mCropOverlayView.resetCropOverlayView();
                setCropOverlayVisibility();
            }
        }
    }

    private void clearImageInt() {
        if (mBitmap != null && (mImageResource > 0 || mLoadedImageUri != null)) {
            mBitmap.recycle();
        }
        mBitmap = null;

        mImageResource = 0;
        mLoadedImageUri = null;
        mImageMatrix.reset();

        mImageView.setImageBitmap(null);

        setCropOverlayVisibility();
    }

    /**
     * 시계 방향으로 지정된 각도 만큼 회전
     *
     * @param degrees 회전할 각도
     */
    private void rotateImage(int degrees) {
        if (mBitmap != null) {

            boolean flipAxes = !mCropOverlayView.isFixAspectRatio() && (degrees > 45 && degrees < 135) || (degrees > 215 && degrees < 305);
            RECT.set(mCropOverlayView.getCropWindowRect());
            float halfWidth = (flipAxes ? RECT.height() : RECT.width()) / 2f;
            float halfHeight = (flipAxes ? RECT.width() : RECT.height()) / 2f;

            mImageMatrix.invert(mImageInverseMatrix);

            POINTS[0] = RECT.centerX();
            POINTS[1] = RECT.centerY();
            POINTS[2] = 0;
            POINTS[3] = 0;
            POINTS[4] = 1;
            POINTS[5] = 0;
            mImageInverseMatrix.mapPoints(POINTS);

            mDegreesRotated += degrees;
            mDegreesRotated = mDegreesRotated >= 0 ? mDegreesRotated % 360 : mDegreesRotated % 360 + 360;

            applyImageMatrix(getWidth(), getHeight());
            mImageMatrix.mapPoints(POINTS2, POINTS);

            // 이미지 스케일링에 따른 width, height 조정
            double change = Math.sqrt(Math.pow(POINTS2[4] - POINTS2[2], 2) + Math.pow(POINTS2[5] - POINTS2[3], 2));
            halfWidth *= change;
            halfHeight *= change;

            // rotation에 따른 width, height에 변경이 생기면 이 중심에 맞춰 crop wiodow의 위치도 계산
            RECT.set(POINTS2[0] - halfWidth, POINTS2[1] - halfHeight, POINTS2[0] + halfWidth, POINTS2[1] + halfHeight);

            mCropOverlayView.resetCropOverlayView();
            mCropOverlayView.setCropWindowRect(RECT);
            applyImageMatrix(getWidth(), getHeight());

            // crop window가 rotation 이후에도 crop할 이미지 범위 안에 있는지 체크
            mCropOverlayView.fixCurrentCropWindowRect();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mBitmap != null) {

            // 스크롤뷰 안에서 사용시 heightSize가 0이므로 이를 보정
            if (heightSize == 0) {
                heightSize = mBitmap.getHeight();
            }

            int desiredWidth;
            int desiredHeight;

            double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
            double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

            // 너비, 높이가 고정이 되어야 하는지 확인
            if (widthSize < mBitmap.getWidth()) {
                viewToBitmapWidthRatio = (double) widthSize / (double) mBitmap.getWidth();
            }
            if (heightSize < mBitmap.getHeight()) {
                viewToBitmapHeightRatio = (double) heightSize / (double) mBitmap.getHeight();
            }

            // 둘 중 하나가 고정될 필요가 있을때 작은 것 기준으로 계산
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize;
                    desiredHeight = (int) (mBitmap.getHeight() * viewToBitmapWidthRatio);
                } else {
                    desiredHeight = heightSize;
                    desiredWidth = (int) (mBitmap.getWidth() * viewToBitmapHeightRatio);
                }
            } else {
                // 이미지가 레이아웃안에 포함 가능한 크기이면 원하는 너비는 단순 이미지 크기
                desiredWidth = mBitmap.getWidth();
                desiredHeight = mBitmap.getHeight();
            }

            int width = getOnMeasureSpec(widthMode, widthSize, desiredWidth);
            int height = getOnMeasureSpec(heightMode, heightSize, desiredHeight);

            mLayoutWidth = width;
            mLayoutHeight = height;

            setMeasuredDimension(mLayoutWidth, mLayoutHeight);

        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            ViewGroup.LayoutParams origParams = this.getLayoutParams();
            origParams.width = mLayoutWidth;
            origParams.height = mLayoutHeight;
            setLayoutParams(origParams);

            if (mBitmap != null) {
                applyImageMatrix(r - l, b - t);

                if (mRestoreCropWindowRect != null) {
                    mImageMatrix.mapRect(mRestoreCropWindowRect);
                    mCropOverlayView.setCropWindowRect(mRestoreCropWindowRect);
                    mCropOverlayView.fixCurrentCropWindowRect();
                    mRestoreCropWindowRect = null;
                }
            } else {
                updateImageBounds(true);
            }
        } else {
            updateImageBounds(true);
        }
    }


    /**
     * matrix를 적용하여 이미지 내부의 이미지를 처리함
     *
     */
    private void applyImageMatrix(float width, float height) {
        if (mBitmap != null && width > 0 && height > 0) {

            mImageMatrix.invert(mImageInverseMatrix);
            RectF cropRect = mCropOverlayView.getCropWindowRect();
            mImageInverseMatrix.mapRect(cropRect);

            mImageMatrix.reset();

            // 이미지 matrix를 가운데 기준으로 맞춤
            mImageMatrix.postTranslate((width - mBitmap.getWidth()) / 2, (height - mBitmap.getHeight()) / 2);
            mapImagePointsByImageMatrix();

            // 이미지 중심 기준으로 필요한 각도로 회전
            if (mDegreesRotated > 0) {
                mImageMatrix.postRotate(mDegreesRotated, BitmapUtils.getRectCenterX(mImagePoints), BitmapUtils.getRectCenterY(mImagePoints));
                mapImagePointsByImageMatrix();
            }

            // 이미지를 이미지 뷰로 스케일하고, 이미지 rect를 새로운 폭 / 높이를 알기 위해 변형
            float scale = Math.min(width / BitmapUtils.getRectWidth(mImagePoints), height / BitmapUtils.getRectHeight(mImagePoints));
            mImageMatrix.postScale(scale, scale, BitmapUtils.getRectCenterX(mImagePoints), BitmapUtils.getRectCenterY(mImagePoints));
            mapImagePointsByImageMatrix();

            mImageMatrix.mapRect(cropRect);

            mCropOverlayView.setCropWindowRect(cropRect);
            mapImagePointsByImageMatrix();

            mImageView.setImageMatrix(mImageMatrix);

            updateImageBounds(false);
        }
    }

    private void mapImagePointsByImageMatrix() {
        mImagePoints[0] = 0;
        mImagePoints[1] = 0;
        mImagePoints[2] = mBitmap.getWidth();
        mImagePoints[3] = 0;
        mImagePoints[4] = mBitmap.getWidth();
        mImagePoints[5] = mBitmap.getHeight();
        mImagePoints[6] = 0;
        mImagePoints[7] = mBitmap.getHeight();
        mImageMatrix.mapPoints(mImagePoints);
    }

    private static int getOnMeasureSpec(int measureSpecMode, int measureSpecSize, int desiredSize) {
        // Measure Width
        int spec;
        if (measureSpecMode == MeasureSpec.EXACTLY) {
            spec = measureSpecSize;
        } else if (measureSpecMode == MeasureSpec.AT_MOST) {
            // match_parent
            spec = Math.min(desiredSize, measureSpecSize);
        } else {
            // wrap_content
            spec = desiredSize;
        }

        return spec;
    }

    /**
     * crop 윈도우 visibility 설정
     */
    private void setCropOverlayVisibility() {
        if (mCropOverlayView != null) {
            mCropOverlayView.setVisibility(mBitmap != null ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * 실제 이미지와 보여지는 이미지 사이의 비율 update
     */
    private void updateImageBounds(boolean clear) {
        if (mBitmap != null && !clear) {

            // 실제 비트 맵 너비, 높이와 출력된 너비, 높이 간 축척 비율을 가져옴
            float scaleFactorWidth = mBitmap.getWidth() / BitmapUtils.getRectWidth(mImagePoints);
            float scaleFactorHeight = mBitmap.getHeight() / BitmapUtils.getRectHeight(mImagePoints);
            mCropOverlayView.setCropWindowLimits(getWidth(), getHeight(), scaleFactorWidth, scaleFactorHeight);
        }

        // 비율 세팅후 비트맵 rect, crop window 설정
        mCropOverlayView.setBounds(clear ? null : mImagePoints, getWidth(), getHeight());
    }

    public enum Guidelines {
        OFF,
        ON
    }

}
