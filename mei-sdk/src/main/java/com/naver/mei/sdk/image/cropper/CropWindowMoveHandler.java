package com.naver.mei.sdk.image.cropper;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * move type(Horizontal, Vertical, Corner, Center)에 따라 crop window의 가장자리를 업데이트하는 핸들러
 */
final class CropWindowMoveHandler {
    private final float mMinCropWidth;
    private final float mMinCropHeight;
    private final float mMaxCropWidth;
    private final float mMaxCropHeight;
    private final Type mType;
    private final PointF mTouchOffset = new PointF();

    public CropWindowMoveHandler(Type type, CropWindowHandler cropWindowHandler, float touchX, float touchY) {
        mType = type;
        mMinCropWidth = cropWindowHandler.getMinCropWidth();
        mMinCropHeight = cropWindowHandler.getMinCropHeight();
        mMaxCropWidth = cropWindowHandler.getMaxCropWidth();
        mMaxCropHeight = cropWindowHandler.getMaxCropHeight();
        calculateTouchOffset(cropWindowHandler.getRect(), touchX, touchY);
    }

    /**
     * 터치 위치가 변경되면 crop window가 업데이트 됨
     * crop window의 위치와 크기를 변경함
     *
     * @param x 이 핸들의 새로운 x 좌표
     * @param y 이 핸들의 새로운 y 좌표
     * @param bounds 이미지의 경계 사각형
     * @param viewWidth
     * @param viewHeight
     * @param snapMargin snap 최대 거리(pixel)
     * @param fixedAspectRatio
     * @param aspectRatio
     */
    public void move(RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapMargin, boolean fixedAspectRatio, float aspectRatio) {
        // 손가락위치의 오프셋(터치위치와 핸들사이의 거리)을 좌표로 adjust
        float adjX = x + mTouchOffset.x;
        float adjY = y + mTouchOffset.y;

        if (mType == Type.CENTER) {
            moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
        } else {
            if (fixedAspectRatio) {
                moveSizeWithFixedAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin, aspectRatio);
            } else {
                moveSizeWithFreeAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
            }
        }
    }

    //region: Private methods

    /**
     * 지정된 핸들의 정확한 위치에서 터치 포인트의 오프셋 계산
     */
    private void calculateTouchOffset(RectF rect, float touchX, float touchY) {

        float touchOffsetX = 0;
        float touchOffsetY = 0;

        switch (mType) {
            case TOP_LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = rect.top - touchY;
                break;
            case TOP_RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = rect.top - touchY;
                break;
            case BOTTOM_LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = rect.bottom - touchY;
                break;
            case BOTTOM_RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = rect.bottom - touchY;
                break;
            case LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = 0;
                break;
            case TOP:
                touchOffsetX = 0;
                touchOffsetY = rect.top - touchY;
                break;
            case RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = 0;
                break;
            case BOTTOM:
                touchOffsetX = 0;
                touchOffsetY = rect.bottom - touchY;
                break;
            case CENTER:
                touchOffsetX = rect.centerX() - touchX;
                touchOffsetY = rect.centerY() - touchY;
                break;
            default:
                break;
        }

        mTouchOffset.x = touchOffsetX;
        mTouchOffset.y = touchOffsetY;
    }

    /**
     * center move는 크기를 변경하지 않고 crop window의 위치만 변경
     */
    private void moveCenter(RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapRadius) {
        float dx = x - rect.centerX();
        float dy = y - rect.centerY();
        if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
            dx /= 1.05f;
            mTouchOffset.x -= dx / 2;
        }
        if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
            dy /= 1.05f;
            mTouchOffset.y -= dy / 2;
        }
        rect.offset(dx, dy);
        snapEdgesToBounds(rect, bounds, snapRadius);
    }

    /**
     * 크기 변경
     */
    private void moveSizeWithFreeAspectRatio(RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapMargin) {
        switch (mType) {
            case TOP_LEFT:
                adjustTop(rect, y, bounds, snapMargin, 0, false, false);
                adjustLeft(rect, x, bounds, snapMargin, 0, false, false);
                break;
            case TOP_RIGHT:
                adjustTop(rect, y, bounds, snapMargin, 0, false, false);
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
                break;
            case BOTTOM_LEFT:
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
                adjustLeft(rect, x, bounds, snapMargin, 0, false, false);
                break;
            case BOTTOM_RIGHT:
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
                break;
            case LEFT:
                adjustLeft(rect, x, bounds, snapMargin, 0, false, false);
                break;
            case TOP:
                adjustTop(rect, y, bounds, snapMargin, 0, false, false);
                break;
            case RIGHT:
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
                break;
            case BOTTOM:
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
                break;
            default:
                break;
        }
    }

    /**
     * 비율 크기 변경
     */
    private void moveSizeWithFixedAspectRatio(RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapMargin, float aspectRatio) {
        switch (mType) {
            case TOP_LEFT:
                if (calculateAspectRatio(x, y, rect.right, rect.bottom) < aspectRatio) {
                    adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, false);
                    adjustLeftByAspectRatio(rect, aspectRatio);
                } else {
                    adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, false);
                    adjustTopByAspectRatio(rect, aspectRatio);
                }
                break;
            case TOP_RIGHT:
                if (calculateAspectRatio(rect.left, y, x, rect.bottom) < aspectRatio) {
                    adjustTop(rect, y, bounds, snapMargin, aspectRatio, false, true);
                    adjustRightByAspectRatio(rect, aspectRatio);
                } else {
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, false);
                    adjustTopByAspectRatio(rect, aspectRatio);
                }
                break;
            case BOTTOM_LEFT:
                if (calculateAspectRatio(x, rect.top, rect.right, y) < aspectRatio) {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, false);
                    adjustLeftByAspectRatio(rect, aspectRatio);
                } else {
                    adjustLeft(rect, x, bounds, snapMargin, aspectRatio, false, true);
                    adjustBottomByAspectRatio(rect, aspectRatio);
                }
                break;
            case BOTTOM_RIGHT:
                if (calculateAspectRatio(rect.left, rect.top, x, y) < aspectRatio) {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, false, true);
                    adjustRightByAspectRatio(rect, aspectRatio);
                } else {
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, false, true);
                    adjustBottomByAspectRatio(rect, aspectRatio);
                }
                break;
            case LEFT:
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, true);
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio);
                break;
            case TOP:
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, true);
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio);
                break;
            case RIGHT:
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, true);
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio);
                break;
            case BOTTOM:
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, true);
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio);
                break;
            default:
                break;
        }
    }

    /**
     * 경계밖으로 벗어난 경우(snap 포함) 처리
     */
    private void snapEdgesToBounds(RectF edges, RectF bounds, float margin) {
        if (edges.left < bounds.left + margin) {
            edges.offset(bounds.left - edges.left, 0);
        }
        if (edges.top < bounds.top + margin) {
            edges.offset(0, bounds.top - edges.top);
        }
        if (edges.right > bounds.right - margin) {
            edges.offset(bounds.right - edges.right, 0);
        }
        if (edges.bottom > bounds.bottom - margin) {
            edges.offset(0, bounds.bottom - edges.bottom);
        }
    }

    /**
     * crop window의 왼쪽 가장자리의 x 좌표를 가져옴
     *
     * @param left 왼쪽 경계가 드래그 되는 위치
     * @param bounds crop 되는 이미지의 경계
     * @param snapMargin 이미지 가장자리까지의 snap 거리 (pixel 단위)
     */
    private void adjustLeft(RectF rect, float left, RectF bounds, float snapMargin, float aspectRatio, boolean topMoves, boolean bottomMoves) {

        float newLeft = left;

        if (newLeft < 0) {
            newLeft /= 1.05f;
            mTouchOffset.x -= newLeft / 1.1f;
        }

        if (newLeft < bounds.left) {
            mTouchOffset.x -= (newLeft - bounds.left) / 2f;
        }

        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left;
        }

        // 수평 size 체크
        if (rect.right - newLeft < mMinCropWidth) {
            newLeft = rect.right - mMinCropWidth;
        }

        if (rect.right - newLeft > mMaxCropWidth) {
            newLeft = rect.right - mMaxCropWidth;
        }

        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left;
        }

        // 비율 설정된 경우 수직 bounds 설정
        if (aspectRatio > 0) {
            float newHeight = (rect.right - newLeft) / aspectRatio;

            // crop window가 수직으로 min보다 작은지 체크
            if (newHeight < mMinCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMinCropHeight * aspectRatio);
                newHeight = (rect.right - newLeft) / aspectRatio;
            }

            // crop window가 수직으로 max를 넘기는지 체크
            if (newHeight > mMaxCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMaxCropHeight * aspectRatio);
                newHeight = (rect.right - newLeft) / aspectRatio;
            }

            // top, bottom 경계가 비율 기준으로 full height 안에 포함되는지 체크
            if (topMoves && bottomMoves) {
                newLeft = Math.max(newLeft, Math.max(bounds.left, rect.right - bounds.height() * aspectRatio));
            } else {
                // top edge가 비율에 따라 경계 안에 있는지 체크
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newLeft = Math.max(bounds.left, rect.right - (rect.bottom - bounds.top) * aspectRatio);
                    newHeight = (rect.right - newLeft) / aspectRatio;
                }

                // bottom egde가 비율에 따라 경계 안에 있는지 체크
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newLeft = Math.max(newLeft, Math.max(bounds.left, rect.right - (bounds.bottom - rect.top) * aspectRatio));
                }
            }
        }

        rect.left = newLeft;
    }

    /**
     * 핸들위치, snap, 경계 등을 고려하여 오른쪽 엣지의 x 좌표를 가져옴
     *
     * @param right
     * @param bounds
     * @param viewWidth
     * @param snapMargin
     */
    private void adjustRight(RectF rect, float right, RectF bounds, int viewWidth, float snapMargin, float aspectRatio, boolean topMoves, boolean bottomMoves) {

        float newRight = right;

        if (newRight > viewWidth) {
            newRight = viewWidth + (newRight - viewWidth) / 1.05f;
            mTouchOffset.x -= (newRight - viewWidth) / 1.1f;
        }

        if (newRight > bounds.right) {
            mTouchOffset.x -= (newRight - bounds.right) / 2f;
        }

        // 끝 경계에 가까운 경우
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right;
        }

        // 수평으로 min보다 작은지 체크
        if (newRight - rect.left < mMinCropWidth) {
            newRight = rect.left + mMinCropWidth;
        }

        // 수평으로 max보다 큰지 체크
        if (newRight - rect.left > mMaxCropWidth) {
            newRight = rect.left + mMaxCropWidth;
        }

        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right;
        }

        // 비율이 적용된 경우 수직 경계 체크
        if (aspectRatio > 0) {
            float newHeight = (newRight - rect.left) / aspectRatio;

            // crop window의 수직 사이즈가 min보다 작은지
            if (newHeight < mMinCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMinCropHeight * aspectRatio);
                newHeight = (newRight - rect.left) / aspectRatio;
            }

            // crop window의 수직 사이즈가 max보다 큰지
            if (newHeight > mMaxCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMaxCropHeight * aspectRatio);
                newHeight = (newRight - rect.left) / aspectRatio;
            }

            // top, bottom 경계가 비율 기준으로 full height 안에 포함되는지 체크
            if (topMoves && bottomMoves) {
                newRight = Math.min(newRight, Math.min(bounds.right, rect.left + bounds.height() * aspectRatio));
            } else {
                // top edge가 비율에 따라 경계 안에 있는지 체크
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newRight = Math.min(bounds.right, rect.left + (rect.bottom - bounds.top) * aspectRatio);
                    newHeight = (newRight - rect.left) / aspectRatio;
                }

                // bottom edge가 비율에 따라 경계 안에 있는지 체크
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newRight = Math.min(newRight, Math.min(bounds.right, rect.left + (bounds.bottom - rect.top) * aspectRatio));
                }
            }
        }

        rect.right = newRight;
    }

    /**
     * 핸들의 위치, snap, 이미지 경계 기준으로 crop window top edge의 y 좌표를 가져옴
     *
     * @param top
     * @param bounds
     * @param snapMargin
     */
    private void adjustTop(RectF rect, float top, RectF bounds, float snapMargin, float aspectRatio, boolean leftMoves, boolean rightMoves) {

        float newTop = top;

        if (newTop < 0) {
            newTop /= 1.05f;
            mTouchOffset.y -= newTop / 1.1f;
        }

        if (newTop < bounds.top) {
            mTouchOffset.y -= (newTop - bounds.top) / 2f;
        }

        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top;
        }

        // crop window의 수직 사이즈가 min보다 작은지
        if (rect.bottom - newTop < mMinCropHeight) {
            newTop = rect.bottom - mMinCropHeight;
        }

        // crop window의 수직 사이즈가 max보다 큰지
        if (rect.bottom - newTop > mMaxCropHeight) {
            newTop = rect.bottom - mMaxCropHeight;
        }

        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top;
        }

        // 주어진 비율이 있는 경우 수평 경계 체크
        if (aspectRatio > 0) {
            float newWidth = (rect.bottom - newTop) * aspectRatio;

            // 가로 세로 비율 조정으로 인해 crop window가 가로로 너무 작지 않은지 확인
            if (newWidth < mMinCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - (mMinCropWidth / aspectRatio));
                newWidth = (rect.bottom - newTop) * aspectRatio;
            }

            // 가로 세로 비율 조정으로 인해 rop window가 가로로 너무 큰지 확인
            if (newWidth > mMaxCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - (mMaxCropWidth / aspectRatio));
                newWidth = (rect.bottom - newTop) * aspectRatio;
            }

            // left, right edge가 가로 세로 비율로 이동하면 전체 너비 범위 내에 있음을 확인
            if (leftMoves && rightMoves) {
                newTop = Math.max(newTop, Math.max(bounds.top, rect.bottom - bounds.width() / aspectRatio));
            } else {
                // left edge가 가로 세로 비율로 이동하면 범위 내에 있음을 확인
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newTop = Math.max(bounds.top, rect.bottom - (rect.right - bounds.left) / aspectRatio);
                    newWidth = (rect.bottom - newTop) * aspectRatio;
                }

                // right edge가 가로 세로 비율에 따라 이동하면 범위 내에 있음을 확인
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newTop = Math.max(newTop, Math.max(bounds.top, rect.bottom - (bounds.right - rect.left) / aspectRatio));
                }
            }
        }

        rect.top = newTop;
    }

    /**
     * 핸들 위치, 이미지 경계, snap 반경 기준으로 crop window의 bottom edge의 y 위치를 가져옴
     *
     * @param bottom
     * @param bounds
     * @param viewHeight
     * @param snapMargin
     */
    private void adjustBottom(RectF rect, float bottom, RectF bounds, int viewHeight, float snapMargin, float aspectRatio, boolean leftMoves, boolean rightMoves) {

        float newBottom = bottom;

        if (newBottom > viewHeight) {
            newBottom = viewHeight + (newBottom - viewHeight) / 1.05f;
            mTouchOffset.y -= (newBottom - viewHeight) / 1.1f;
        }

        if (newBottom > bounds.bottom) {
            mTouchOffset.y -= (newBottom - bounds.bottom) / 2f;
        }

        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom;
        }

        // crop window의 수직 사이즈가 min보다 작은지
        if (newBottom - rect.top < mMinCropHeight) {
            newBottom = rect.top + mMinCropHeight;
        }

        // crop window의 수직 사이즈가 max보다 큰지
        if (newBottom - rect.top > mMaxCropHeight) {
            newBottom = rect.top + mMaxCropHeight;
        }

        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom;
        }

        // 설정된 비율이 있는 경우 수평 경계 확인
        if (aspectRatio > 0) {
            float newWidth = (newBottom - rect.top) * aspectRatio;

            // crop window의 수평 사이즈가 min보다 작은지
            if (newWidth < mMinCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMinCropWidth / aspectRatio);
                newWidth = (newBottom - rect.top) * aspectRatio;
            }

            // crop window의 수평 사이즈가 max보다 큰지
            if (newWidth > mMaxCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMaxCropWidth / aspectRatio);
                newWidth = (newBottom - rect.top) * aspectRatio;
            }

            // left, right edge가 가로 세로 비율로 이동하면 전체 너비 범위 내에 있음을 확인
            if (leftMoves && rightMoves) {
                newBottom = Math.min(newBottom, Math.min(bounds.bottom, rect.top + bounds.width() / aspectRatio));
            } else {
                // left edge가 가로 세로 비율로 이동하면 범위 내에 있음을 확인합니다.
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newBottom = Math.min(bounds.bottom, rect.top + (rect.right - bounds.left) / aspectRatio);
                    newWidth = (newBottom - rect.top) * aspectRatio;
                }

                // right edge가 가로 세로 비율에 따라 이동하면 범위 내에 있음을 확인
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newBottom = Math.min(newBottom, Math.min(bounds.bottom, rect.top + (bounds.right - rect.left) / aspectRatio));
                }
            }
        }

        rect.bottom = newBottom;
    }

    /**
     * 현재 crop window의 height와 주어진 비율 기준으로 left edge 조정
     * right edge는 유지되고 left edge는 비율 유지를 위해 조정
     *
     */
    private void adjustLeftByAspectRatio(RectF rect, float aspectRatio) {
        rect.left = rect.right - rect.height() * aspectRatio;
    }

    /**
     * 현재 crop window width와 지정된 비율로 top edge 조정
     * bottom edge는 유지되고 top egde는 비율 유지를 위해 조정
     */
    private void adjustTopByAspectRatio(RectF rect, float aspectRatio) {
        rect.top = rect.bottom - rect.width() / aspectRatio;
    }

    /**
     * 현재 crop window의 height와 비율 기준으로 right edge를 조정
     * left edge는 위치에 유지되고 left는 비율을 높이 기준으로 유지하기 위해 조정
     */
    private void adjustRightByAspectRatio(RectF rect, float aspectRatio) {
        rect.right = rect.left + rect.height() * aspectRatio;
    }

    /**
     * crop window의 width와 주어진 비율 기준으로 bottom edge 조정
     * top edge는 주어진 위치에 유지 되고 top 은 width 와 비율을 맞추기 위해 조정
     */
    private void adjustBottomByAspectRatio(RectF rect, float aspectRatio) {
        rect.bottom = rect.top + rect.width() / aspectRatio;
    }

    /**
     * 현재 crop window의 높이와 주어진 비율 기준으로 left, right edge를 조정
     * right, left 모서리 둘 다 높이에 대한 비율을 유지하기 위해 중심을 기준으로 동등하게 조정
     *
     */
    private void adjustLeftRightByAspectRatio(RectF rect, RectF bounds, float aspectRatio) {
        rect.inset((rect.width() - rect.height() * aspectRatio) / 2, 0);
        if (rect.left < bounds.left) {
            rect.offset(bounds.left - rect.left, 0);
        }
        if (rect.right > bounds.right) {
            rect.offset(bounds.right - rect.right, 0);
        }
    }

    /**
     * 현재 crop window의 width와 주어진 비율 기준으로 top, bottom edge를 조정
     * top, left bottom 둘 다 width에 대한 비율을 유지하기 위해 중심을 기준으로 동등하게 조정
     */
    private void adjustTopBottomByAspectRatio(RectF rect, RectF bounds, float aspectRatio) {
        rect.inset(0, (rect.height() - rect.width() / aspectRatio) / 2);
        if (rect.top < bounds.top) {
            rect.offset(0, bounds.top - rect.top);
        }
        if (rect.bottom > bounds.bottom) {
            rect.offset(0, bounds.bottom - rect.bottom);
        }
    }

    /**
     * 주어진 사각형의 가로 세로 비율을 계산
     */
    private static float calculateAspectRatio(float left, float top, float right, float bottom) {
        return (right - left) / (bottom - top);
    }


    /**
     * crop window의 핸들 타입
     *
     * TL T T T T TR
     * L C C C C R
     * L C C C C R
     * L C C C C R
     * L C C C C R
     * BL B B B B BR
     *
     */
    public enum Type {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        CENTER
    }
}