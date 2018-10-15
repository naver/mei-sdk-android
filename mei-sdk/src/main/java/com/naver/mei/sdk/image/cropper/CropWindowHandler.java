package com.naver.mei.sdk.image.cropper;

import android.graphics.RectF;

/**
 * crop window의 move type, 위치에 관한 handler
 */
final class CropWindowHandler {
    private final RectF mEdges = new RectF();   // crop window의 좌표, 크기를 결정하는 4 꼭지점
    private final RectF mGetEdges = new RectF();    // 새로운 rect를 생성하지 않고 4개의 꼭지점을 얻는 용도로만 사용
    // pixel 기준
    private float mMinCropWindowWidth;
    private float mMinCropWindowHeight;
    private float mMaxCropWindowWidth;
    private float mMaxCropWindowHeight;
    private float mMinCropResultWidth;
    private float mMinCropResultHeight;
    private float mMaxCropResultWidth;
    private float mMaxCropResultHeight;
    private float mScaleFactorWidth = 1;    // 표시된 이미지와 실제 이미지 사이의 scale width
    private float mScaleFactorHeight = 1;   // // 표시된 이미지와 실제 이미지 사이의 scale height

    /**
     * crop window의 left/top/right/bottom 좌표
     */
    public RectF getRect() {
        mGetEdges.set(mEdges);
        return mGetEdges;
    }

    public float getMinCropWidth() {
        return Math.max(mMinCropWindowWidth, mMinCropResultWidth / mScaleFactorWidth);
    }

    public float getMinCropHeight() {
        return Math.max(mMinCropWindowHeight, mMinCropResultHeight / mScaleFactorHeight);
    }

    public float getMaxCropWidth() {
        return Math.min(mMaxCropWindowWidth, mMaxCropResultWidth / mScaleFactorWidth);
    }

    public float getMaxCropHeight() {
        return Math.min(mMaxCropWindowHeight, mMaxCropResultHeight / mScaleFactorHeight);
    }

    public float getScaleFactorWidth() {
        return mScaleFactorWidth;
    }

    public float getScaleFactorHeight() {
        return mScaleFactorHeight;
    }

    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mMinCropResultWidth = minCropResultWidth;
        mMinCropResultHeight = minCropResultHeight;
    }

    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mMaxCropResultWidth = maxCropResultWidth;
        mMaxCropResultHeight = maxCropResultHeight;
    }

    public void setCropWindowLimits(float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
        mMaxCropWindowWidth = maxWidth;
        mMaxCropWindowHeight = maxHeight;
        mScaleFactorWidth = scaleFactorWidth;
        mScaleFactorHeight = scaleFactorHeight;
    }

    public void setInitialAttributeValues(CropImageOptions options) {
        mMinCropWindowWidth = options.minCropWindowWidth;
        mMinCropWindowHeight = options.minCropWindowHeight;
        mMinCropResultWidth = options.minCropResultWidth;
        mMinCropResultHeight = options.minCropResultHeight;
        mMaxCropResultWidth = options.maxCropResultWidth;
        mMaxCropResultHeight = options.maxCropResultHeight;
    }

    public void setRect(RectF rect) {
        mEdges.set(rect);
    }

    /**
     * 가이드라인을 표시할 수 있는 충분한 크기인지 여부
     */
    public boolean showGuidelines() {
        return !(mEdges.width() < 100 || mEdges.height() < 100);
    }

    /**
     * 터치 좌표, 터치 반경, bonding 값을 고려하여 핸들이 눌러졌는지 여부 결정
     *
     * @param x 터치포인트의 x좌표
     * @param y 터치포인트의 y좌표
     * @param targetRadius 타겟 반경
     * @return 눌려진 핸들; 핸들이 눌러지지 않은 경우 null
     */
    public CropWindowMoveHandler getMoveHandler(float x, float y, float targetRadius) {
        CropWindowMoveHandler.Type type = getRectanglePressedMoveType(x, y, targetRadius);
        return type != null ? new CropWindowMoveHandler(type, this, x, y) : null;
    }

    private CropWindowMoveHandler.Type getRectanglePressedMoveType(float x, float y, float targetRadius) {
        CropWindowMoveHandler.Type moveType = null;

        /*
           TL T T T T TR
            L C C C C R
            L C C C C R
            L C C C C R
            L C C C C R
           BL B B B B BR
        */

        // corner >> side >> center handle 순으로 처리함
        if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.LEFT;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && !focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        }

        return moveType;
    }


    /**
     * 지정된 좌표가 코너 핸들의 타겟 영역 안에 있는지 확인
     *
     * @param x 터치된 x 좌표
     * @param y 터치된 y 좌표
     * @param handleX 코너 핸들의 x 좌표
     * @param handleY 코너 핸들의 y 좌표
     * @param targetRadius 대상 반경(pixel)
     * @return 터치한 곳이 대상 타겟 터치 존 안이면 true
     */
    private static boolean isInCornerTargetZone(float x, float y, float handleX, float handleY, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * 지정된 좌표가 가로 막대 핸들의 대상 터치 영역에 있는지 확인
     *
     * @param x 터치된 x 좌표
     * @param y 터치된 y 좌표
     * @param handleXStart 수평 막대 핸들의 왼쪽 x 좌표
     * @param handleXEnd 수평 막대 핸들의 오른쪽 x 좌표
     * @param handleY 수평 막대 핸들의 y 좌표
     * @param targetRadius 대상 반경(pixel)
     * @return 터치한 곳이 대상 타겟 터치 존 안이면 true
     */
    private static boolean isInHorizontalTargetZone(float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * 지정된 좌표가 세로 막대 핸들의 대상 접촉 영역에 있는지 확인
     *
     * @param x 터치된 x 좌표
     * @param y 터치된 y 좌표
     * @param handleX 수직 막대 핸들의 x 좌표
     * @param handleYStart 수직 막대 핸들의 상단 y 좌표
     * @param handleYEnd 수직 막대 핸들의 하단 y 좌표
     * @param targetRadius 대상 반경(pixel)
     * @return 터치한 곳이 대상 타겟 터치 존 안이면 true
     */
    private static boolean isInVerticalTargetZone(float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
    }

    /**
     * 지정된 좌표가 지정된 경계의 안에 포함되는지 여부 확인
     *
     * @param x 터치된 x 좌표
     * @param y 터치된 y 좌표
     * @param left 왼쪽 경계의 x 좌표
     * @param top 상단 경계의 y 좌표
     * @param right 오른쪽 경계의 x 좌표
     * @param bottom 하단 경계의 x 좌표
     * @return 터치 포인트가 경계 내부에 있으면 true
     */
    private static boolean isInCenterTargetZone(float x, float y, float left, float top, float right, float bottom) {
        return x > left && x < right && y > top && y < bottom;
    }

    /**
     * crop window의 핸들의 focus를 center로 둘지, side로 둘지 결정
     * 작은 이미지인 경우 center focus로 사용자가 이동할 수 있게 함
     * 큰 이미지인 경우 side focus로 사용자가 잡을 수 있게 함
     *
     * @return center에 focus를 맞출만큼 작으면 true. show_guidelines limit 미만
     */
    private boolean focusCenter() {
        return !showGuidelines();
    }
}