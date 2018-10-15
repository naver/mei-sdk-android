package com.naver.mei.sdk.image.cropper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;

/**
 * crop window와 crop window 외부의 음영 처리 배경을 포함하는 overlay custom view
 */
public class CropOverlayView extends View {
    static final Rect EMPTY_RECT = new Rect();
    static final RectF EMPTY_RECT_F = new RectF();

    private final CropWindowHandler mCropWindowHandler = new CropWindowHandler();   // crop window의 위치와 moving 등을 관리하는 핸들러
    private CropWindowChangeListener mCropWindowChangeListener;
    private Paint mBorderPaint; // crop 영역 주위의 흰색 사각형과 관련된 Paint
    private Paint mBorderCornerPaint;   // crop 영역 모서리용 Paint
    private Paint mGuidelinePaint;  // crop 영역 가이드 라인 Paint
    private Paint mBackgroundPaint;
    private final float[] mBoundsPoints = new float[8];   // crop 중인 비트맵의 bound point
    private final RectF mCalcBounds = new RectF();  // // crop 중인 비트맵의 bound rect
    private int mViewWidth; // crop 영역 경계를 확인할수 있는 바운딩 이미지뷰 너비
    private int mViewHeight;    // crop 영역 경계를 확인할수 있는 바운딩 이미지뷰 높이
    private float mBorderCornerOffset;  // border corner와 border 간의 offset
    private float mBorderCornerLength;
    private float mInitialCropWindowPaddingRatio;   // 이미지와 crop window 간 padding
    private float mTouchRadius; // 주어진 핸들 주위의 터치 영역 반경(pixel)
    private float mSnapRadius;  // crop window와 이미지 경계간 snap 효과 반경
    private CropWindowMoveHandler mMoveHandler;     // 현재 눌려진 핸들
    private boolean mFixAspectRatio;
    private int mAspectRatioX;
    private int mAspectRatioY;
    private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
    private CropImageView.Guidelines mGuidelines;
    private final Rect mInitialCropWindowRect = new Rect();
    private boolean initializedCropWindow;  // crop window 초기화 여부

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCropWindowChangeListener(CropWindowChangeListener listener) {
        mCropWindowChangeListener = listener;
    }

    /**
     * crop window의 left/top/right/bottom 좌표 값
     */
    public RectF getCropWindowRect() {
        return mCropWindowHandler.getRect();
    }

    public void setCropWindowRect(RectF rect) {
        mCropWindowHandler.setRect(rect);
    }

    /**
     * 현재 crop window가 crop 중인 이미지나 뷰의 경계 밖에 있는 경우 고정시킴
     */
    public void fixCurrentCropWindowRect() {
        RectF rect = getCropWindowRect();
        fixCropWindowRectByRules(rect);
        mCropWindowHandler.setRect(rect);
    }

    /**
     * 이미지뷰를 기준으로 이미지의 위치를 CropOverlayView에 알림
     *
     * @param boundsPoints 이미지의 경계점
     * @param viewWidth 경계 이미지 뷰 너비
     * @param viewHeight 경계 이미지 뷰 높이
     */
    public void setBounds(float[] boundsPoints, int viewWidth, int viewHeight) {
        if (boundsPoints == null || !Arrays.equals(mBoundsPoints, boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(mBoundsPoints, 0);
            } else {
                System.arraycopy(boundsPoints, 0, mBoundsPoints, 0, boundsPoints.length);
            }
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            RectF cropRect = mCropWindowHandler.getRect();
            if (cropRect.width() == 0 || cropRect.height() == 0) {
                initCropWindow();
            }
        }
    }

    public void resetCropOverlayView() {
        if (initializedCropWindow) {
            setCropWindowRect(EMPTY_RECT_F);
            initCropWindow();
            invalidate();
        }
    }


    public CropImageView.Guidelines getGuidelines() {
        return mGuidelines;
    }

    public void setGuidelines(CropImageView.Guidelines guidelines) {
        if (mGuidelines != guidelines) {
            mGuidelines = guidelines;
            if (initializedCropWindow) {
                invalidate();
            }
        }
    }

    public boolean isFixAspectRatio() {
        return mFixAspectRatio;
    }

    public void setFixedAspectRatio(boolean fixAspectRatio) {
        if (mFixAspectRatio != fixAspectRatio) {
            mFixAspectRatio = fixAspectRatio;
            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public int getAspectRatioX() {
        return mAspectRatioX;
    }

    public void setAspectRatioX(int aspectRatioX) {
        if (aspectRatioX <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else if (mAspectRatioX != aspectRatioX) {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public int getAspectRatioY() {
        return mAspectRatioY;
    }

    public void setAspectRatioY(int aspectRatioY) {
        if (aspectRatioY <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else if (mAspectRatioY != aspectRatioY) {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    public void setSnapRadius(float snapRadius) {
        mSnapRadius = snapRadius;
    }

    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight);
    }

    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight);
    }

    public void setCropWindowLimits(float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
        mCropWindowHandler.setCropWindowLimits(maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight);
    }

    public Rect getInitialCropWindowRect() {
        return mInitialCropWindowRect;
    }

    public void setInitialCropWindowRect(Rect rect) {
        mInitialCropWindowRect.set(rect != null ? rect : EMPTY_RECT);
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
            callOnCropWindowChanged(false);
        }
    }

    public void resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
            callOnCropWindowChanged(false);
        }
    }

    public void setInitialAttributeValues(CropImageOptions options) {
        mCropWindowHandler.setInitialAttributeValues(options);

        setSnapRadius(options.snapRadius);

        setGuidelines(options.guidelines);

        setFixedAspectRatio(options.fixAspectRatio);

        setAspectRatioX(options.aspectRatioX);

        setAspectRatioY(options.aspectRatioY);

        mTouchRadius = options.touchRadius;

        mInitialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio;

        mBorderPaint = getNewPaintOrNull(options.borderLineThickness, options.borderLineColor);

        mBorderCornerOffset = options.borderCornerOffset;
        mBorderCornerLength = options.borderCornerLength;
        mBorderCornerPaint = getNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor);

        mGuidelinePaint = getNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor);

        mBackgroundPaint = getNewPaint(options.backgroundColor);
    }

    private void initCropWindow() {
        float leftLimit = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0);
        float topLimit = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0);
        float rightLimit = Math.min(BitmapUtils.getRectRight(mBoundsPoints), getWidth());
        float bottomLimit = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), getHeight());

        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return;
        }

        RectF rect = new RectF();

        initializedCropWindow = true;

        float horizontalPadding = mInitialCropWindowPaddingRatio * (rightLimit - leftLimit);
        float verticalPadding = mInitialCropWindowPaddingRatio * (bottomLimit - topLimit);

        if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            // 표시 된 이미지 기준으로 crop window 위치를 가져옴
            rect.left = leftLimit + mInitialCropWindowRect.left / mCropWindowHandler.getScaleFactorWidth();
            rect.top = topLimit + mInitialCropWindowRect.top / mCropWindowHandler.getScaleFactorHeight();
            rect.right = rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.getScaleFactorWidth();
            rect.bottom = rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.getScaleFactorHeight();

            // 부동 소수점 오류 보정
            rect.left = Math.max(leftLimit, rect.left);
            rect.top = Math.max(topLimit, rect.top);
            rect.right = Math.min(rightLimit, rect.right);
            rect.bottom = Math.min(bottomLimit, rect.bottom);

        } else if (mFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {
            // 이미지 비율이 crop window 비율보다 큰 경우 이미지의 height가 초기 길이, 그렇지 않으면 반대로
            float bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit);
            if (bitmapAspectRatio > mTargetAspectRatio) {

                rect.top = topLimit + verticalPadding;
                rect.bottom = bottomLimit - verticalPadding;

                float centerX = getWidth() / 2f;

                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;

                // 최소 40dp 제한
                float cropWidth = Math.max(mCropWindowHandler.getMinCropWidth(), rect.height() * mTargetAspectRatio);

                float halfCropWidth = cropWidth / 2f;
                rect.left = centerX - halfCropWidth;
                rect.right = centerX + halfCropWidth;

            } else {

                rect.left = leftLimit + horizontalPadding;
                rect.right = rightLimit - horizontalPadding;

                float centerY = getHeight() / 2f;

                // 40dp 제한
                float cropHeight = Math.max(mCropWindowHandler.getMinCropHeight(), rect.width() / mTargetAspectRatio);

                float halfCropHeight = cropHeight / 2f;
                rect.top = centerY - halfCropHeight;
                rect.bottom = centerY + halfCropHeight;
            }
        } else {
            // 이미지 기준으로 10% padding 적용
            rect.left = leftLimit + horizontalPadding;
            rect.top = topLimit + verticalPadding;
            rect.right = rightLimit - horizontalPadding;
            rect.bottom = bottomLimit - verticalPadding;
        }

        fixCropWindowRectByRules(rect);

        mCropWindowHandler.setRect(rect);
    }

    /**
     * 주어진 rect를 bitmap rect에 맞추고 최소, 최대 길이를 비율대로 fix
     */
    private void fixCropWindowRectByRules(RectF rect) {
        if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
            float adj = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2;
            rect.left -= adj;
            rect.right += adj;
        }
        if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
            float adj = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2;
            rect.top -= adj;
            rect.bottom += adj;
        }
        if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
            float adj = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2;
            rect.left += adj;
            rect.right -= adj;
        }
        if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
            float adj = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2;
            rect.top += adj;
            rect.bottom -= adj;
        }

        calculateBounds();
        if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
            float leftLimit = Math.max(mCalcBounds.left, 0);
            float topLimit = Math.max(mCalcBounds.top, 0);
            float rightLimit = Math.min(mCalcBounds.right, getWidth());
            float bottomLimit = Math.min(mCalcBounds.bottom, getHeight());
            if (rect.left < leftLimit) {
                rect.left = leftLimit;
            }
            if (rect.top < topLimit) {
                rect.top = topLimit;
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit;
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit;
            }
        }
        if (mFixAspectRatio && Math.abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                float adj = Math.abs(rect.height() * mTargetAspectRatio - rect.width()) / 2;
                rect.left += adj;
                rect.right -= adj;
            } else {
                float adj = Math.abs(rect.width() / mTargetAspectRatio - rect.height()) / 2;
                rect.top += adj;
                rect.bottom -= adj;
            }
        }
    }

    /**
     * crop area의 테두리, 가이드라인, 반투명 배경 draw
     */
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        // crop 영역의 반투명 배경을 그림
        drawBackground(canvas);

        if (mCropWindowHandler.showGuidelines()) {
            if (mGuidelines == CropImageView.Guidelines.ON) {
                drawGuidelines(canvas);
            }
        }

        drawBorders(canvas);

        drawCorners(canvas);
    }

    /**
     * crop 영역 밖의 반투명 배경 draw
     */
    private void drawBackground(Canvas canvas) {

        RectF rect = mCropWindowHandler.getRect();

        float left = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0);
        float top = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0);
        float right = Math.min(BitmapUtils.getRectRight(mBoundsPoints), getWidth());
        float bottom = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), getHeight());

        if (Build.VERSION.SDK_INT <= 17) {
            canvas.drawRect(left, top, right, rect.top, mBackgroundPaint);
            canvas.drawRect(left, rect.bottom, right, bottom, mBackgroundPaint);
            canvas.drawRect(left, rect.top, rect.left, rect.bottom, mBackgroundPaint);
            canvas.drawRect(rect.right, rect.top, right, rect.bottom, mBackgroundPaint);
        } else {
            canvas.save();
            canvas.clipRect(rect, Region.Op.XOR);
            canvas.drawRect(left, top, right, bottom, mBackgroundPaint);
            canvas.restore();
        }

    }

    /**
     * 각 2줄의 수직, 수평 가이드라인
     */
    private void drawGuidelines(Canvas canvas) {
        if (mGuidelinePaint != null) {
            float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(sw, sw);

            float oneThirdCropWidth = rect.width() / 3;
            float oneThirdCropHeight = rect.height() / 3;

            // vertical
            float x1 = rect.left + oneThirdCropWidth;
            float x2 = rect.right - oneThirdCropWidth;
            canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint);
            canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint);

            // horizontal
            float y1 = rect.top + oneThirdCropHeight;
            float y2 = rect.bottom - oneThirdCropHeight;
            canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint);
            canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint);
        }
    }

    /**
     * crop 영역의 border
     */
    private void drawBorders(Canvas canvas) {
        if (mBorderPaint != null) {
            float w = mBorderPaint.getStrokeWidth();
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w / 2, w / 2);

            canvas.drawRect(rect, mBorderPaint);
        }
    }

    /**
     * crop 영역의 corner
     */
    private void drawCorners(Canvas canvas) {
        if (mBorderCornerPaint != null) {

            float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            float cornerWidth = mBorderCornerPaint.getStrokeWidth();
            float w = cornerWidth / 2 + mBorderCornerOffset;
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w, w);

            float cornerOffset = (cornerWidth - lineWidth) / 2;
            float cornerExtension = cornerWidth / 2 + cornerOffset;

            // Top left
            canvas.drawLine(rect.left - cornerOffset, rect.top - cornerExtension, rect.left - cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.top - cornerOffset, rect.left + mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);

            // Top right
            canvas.drawLine(rect.right + cornerOffset, rect.top - cornerExtension, rect.right + cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.top - cornerOffset, rect.right - mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(rect.left - cornerOffset, rect.bottom + cornerExtension, rect.left - cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.bottom + cornerOffset, rect.left + mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(rect.right + cornerOffset, rect.bottom + cornerExtension, rect.right + cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.bottom + cornerOffset, rect.right - mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);
        }
    }

    private static Paint getNewPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        return paint;
    }

    private static Paint getNewPaintOrNull(float thickness, int color) {
        if (thickness > 0) {
            Paint borderPaint = new Paint();
            borderPaint.setColor(color);
            borderPaint.setStrokeWidth(thickness);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setAntiAlias(true);
            return borderPaint;
        } else {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onActionDown(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    onActionUp();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    onActionMove(event.getX(), event.getY());
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 눌려진 위치에 따라 crop window의 이동이 시작
     * 눌려진 위치가 crop window보다 특정값 이상으로 멀면 null 반환
     */
    private void onActionDown(float x, float y) {
        mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, mTouchRadius);
        if (mMoveHandler != null) {
            invalidate();
        }
    }

    /**
     * 핸들러 clear
     */
    private void onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null;
            callOnCropWindowChanged(false);
            invalidate();
        }
    }

    /**
     * down 이벤트시 생성된 move handler로 crop window의 이동을 처리함
     * 이 핸들러는 crop window의 move/ resize 처리를 함
     */
    private void onActionMove(float x, float y) {
        if (mMoveHandler != null) {
            float snapRadius = mSnapRadius;
            RectF rect = mCropWindowHandler.getRect();

            mMoveHandler.move(rect, x, y, mCalcBounds, mViewWidth, mViewHeight, snapRadius, mFixAspectRatio, mTargetAspectRatio);
            mCropWindowHandler.setRect(rect);
            callOnCropWindowChanged(true);
            invalidate();
        }
    }

    /**
     * 현재 crop window의 바운더리 영역
     */
    private boolean calculateBounds() {
        float left = BitmapUtils.getRectLeft(mBoundsPoints);
        float top = BitmapUtils.getRectTop(mBoundsPoints);
        float right = BitmapUtils.getRectRight(mBoundsPoints);
        float bottom = BitmapUtils.getRectBottom(mBoundsPoints);

        mCalcBounds.set(left, top, right, bottom);
        return false;

    }


    private void callOnCropWindowChanged(boolean inProgress) {
        try {
            if (mCropWindowChangeListener != null) {
                mCropWindowChangeListener.onCropWindowChanged(inProgress);
            }
        } catch (Exception e) {
            Log.e("MEI", "Exception in crop window changed", e);
        }
    }

    public interface CropWindowChangeListener {
        void onCropWindowChanged(boolean inProgress);
    }

}