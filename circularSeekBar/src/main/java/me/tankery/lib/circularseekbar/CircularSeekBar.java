/*
 *
 * Copyright 2013 Matt Joseph
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
 * This custom view/widget was inspired and guided by:
 *
 * HoloCircleSeekBar - Copyright 2012 Jes�s Manzano
 * HoloColorPicker - Copyright 2012 Lars Werkman (Designed by Marie Schweiz)
 *
 * Although I did not used the code from either project directly, they were both used as
 * reference material, and as a result, were extremely helpful.
 */

package me.tankery.lib.circularseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CircularSeekBar extends View {

    /**
     * Used to scale the dp units to pixels
     */
    private final float DPTOPX_SCALE = getResources().getDisplayMetrics().density;

    /**
     * Minimum touch target size in DP. 48dp is the Android design recommendation
     */
    private final float MIN_TOUCH_TARGET_DP = 48;

    // Default values
    private static final int DEFAULT_CIRCLE_STYLE = Paint.Cap.ROUND.ordinal();
    private static final float DEFAULT_CIRCLE_X_RADIUS = 30f;
    private static final float DEFAULT_CIRCLE_Y_RADIUS = 30f;
    private static final float DEFAULT_POINTER_STROKE_WIDTH = 14f;
    private static final float DEFAULT_POINTER_HALO_WIDTH = 6f;
    private static final float DEFAULT_POINTER_HALO_BORDER_WIDTH = 2f;
    private static final float DEFAULT_CIRCLE_STROKE_WIDTH = 5f;
    private static final float DEFAULT_START_ANGLE = 270f; // Geometric (clockwise, relative to 3 o'clock)
    private static final float DEFAULT_END_ANGLE = 270f; // Geometric (clockwise, relative to 3 o'clock)
    private static final float DEFAULT_POINTER_ANGLE = 0;
    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_PROGRESS = 0;
    private static final int DEFAULT_CIRCLE_COLOR = Color.DKGRAY;
    private static final int DEFAULT_CIRCLE_PROGRESS_COLOR = Color.argb(235, 74, 138, 255);
    private static final int DEFAULT_POINTER_COLOR = Color.argb(235, 74, 138, 255);
    private static final int DEFAULT_POINTER_HALO_COLOR = Color.argb(135, 74, 138, 255);
    private static final int DEFAULT_POINTER_HALO_COLOR_ONTOUCH = Color.argb(135, 74, 138, 255);
    private static final int DEFAULT_CIRCLE_FILL_COLOR = Color.TRANSPARENT;
    private static final int DEFAULT_POINTER_ALPHA = 135;
    private static final int DEFAULT_POINTER_ALPHA_ONTOUCH = 100;
    private static final boolean DEFAULT_USE_CUSTOM_RADII = false;
    private static final boolean DEFAULT_MAINTAIN_EQUAL_CIRCLE = true;
    private static final boolean DEFAULT_MOVE_OUTSIDE_CIRCLE = false;
    private static final boolean DEFAULT_LOCK_ENABLED = true;
    private static final boolean DEFAULT_DISABLE_POINTER = false;
    private static final boolean DEFAULT_NEGATIVE_ENABLED = false;

    /**
     * {@code Paint} instance used to draw the inactive circle.
     */
    private Paint mCirclePaint;

    /**
     * {@code Paint} instance used to draw the circle fill.
     */
    private Paint mCircleFillPaint;

    /**
     * {@code Paint} instance used to draw the active circle (represents progress).
     */
    private Paint mCircleProgressPaint;

    /**
     * {@code Paint} instance used to draw the glow from the active circle.
     */
    private Paint mCircleProgressGlowPaint;

    /**
     * {@code Paint} instance used to draw the center of the pointer.
     * Note: This is broken on 4.0+, as BlurMasks do not work with hardware acceleration.
     */
    private Paint mPointerPaint;

    /**
     * {@code Paint} instance used to draw the halo of the pointer.
     * Note: The halo is the part that changes transparency.
     */
    private Paint mPointerHaloPaint;

    /**
     * {@code Paint} instance used to draw the border of the pointer, outside of the halo.
     */
    private Paint mPointerHaloBorderPaint;

    /**
     * The style of the circle, can be butt, round or square.
     */
    private Paint.Cap mCircleStyle;

    /**
     * current in negative half cycle.
     */
    private boolean isInNegativeHalf;

    /**
     * The width of the circle (in pixels).
     */
    private float mCircleStrokeWidth;

    /**
     * The X radius of the circle (in pixels).
     */
    private float mCircleXRadius;

    /**
     * The Y radius of the circle (in pixels).
     */
    private float mCircleYRadius;

    /**
     * If disable pointer, we can't seek the progress.
     */
    private boolean mDisablePointer;

    /**
     * The radius of the pointer (in pixels).
     */
    private float mPointerStrokeWidth;

    /**
     * The width of the pointer halo (in pixels).
     */
    private float mPointerHaloWidth;

    /**
     * The width of the pointer halo border (in pixels).
     */
    private float mPointerHaloBorderWidth;

    /**
     * Angle of the pointer arc.
     * Default is 0, the pointer is a circle when angle is 0 and the style is round.
     * Can not less then 0. can not longer than 360.
     */
    private float mPointerAngle;

    /**
     * Start angle of the CircularSeekBar.
     * Note: If mStartAngle and mEndAngle are set to the same angle, 0.1 is subtracted
     * from the mEndAngle to make the circle function properly.
     */
    private float mStartAngle;

    /**
     * End angle of the CircularSeekBar.
     * Note: If mStartAngle and mEndAngle are set to the same angle, 0.1 is subtracted
     * from the mEndAngle to make the circle function properly.
     */
    private float mEndAngle;

    /**
     * {@code RectF} that represents the circle (or ellipse) of the seekbar.
     */
    private final RectF mCircleRectF = new RectF();

    /**
     * Holds the color value for {@code mPointerPaint} before the {@code Paint} instance is created.
     */
    private int mPointerColor = DEFAULT_POINTER_COLOR;

    /**
     * Holds the color value for {@code mPointerHaloPaint} before the {@code Paint} instance is created.
     */
    private int mPointerHaloColor = DEFAULT_POINTER_HALO_COLOR;

    /**
     * Holds the color value for {@code mPointerHaloPaint} before the {@code Paint} instance is created.
     */
    private int mPointerHaloColorOnTouch = DEFAULT_POINTER_HALO_COLOR_ONTOUCH;

    /**
     * Holds the color value for {@code mCirclePaint} before the {@code Paint} instance is created.
     */
    private int mCircleColor = DEFAULT_CIRCLE_COLOR;

    /**
     * Holds the color value for {@code mCircleFillPaint} before the {@code Paint} instance is created.
     */
    private int mCircleFillColor = DEFAULT_CIRCLE_FILL_COLOR;

    /**
     * Holds the color value for {@code mCircleProgressPaint} before the {@code Paint} instance is created.
     */
    private int mCircleProgressColor = DEFAULT_CIRCLE_PROGRESS_COLOR;

    /**
     * Holds the alpha value for {@code mPointerHaloPaint}.
     */
    private int mPointerAlpha = DEFAULT_POINTER_ALPHA;

    /**
     * Holds the OnTouch alpha value for {@code mPointerHaloPaint}.
     */
    private int mPointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH;

    /**
     * Distance (in degrees) that the the circle/semi-circle makes up.
     * This amount represents the max of the circle in degrees.
     */
    private float mTotalCircleDegrees;

    /**
     * Distance (in degrees) that the current progress makes up in the circle.
     */
    private float mProgressDegrees;

    /**
     * {@code Path} used to draw the circle/semi-circle.
     */
    private Path mCirclePath;

    /**
     * {@code Path} used to draw the progress on the circle.
     */
    private Path mCircleProgressPath;

    /**
     * {@code Path} used to draw the pointer arc on the circle.
     */
    private Path mCirclePonterPath;

    /**
     * Max value that this CircularSeekBar is representing.
     */
    private float mMax;

    /**
     * Progress value that this CircularSeekBar is representing.
     */
    private float mProgress;

    /**
     * Used for enabling/disabling the negative progress bar.
     * */
    private boolean negativeEnabled;

    /**
     * If true, then the user can specify the X and Y radii.
     * If false, then the View itself determines the size of the CircularSeekBar.
     */
    private boolean mCustomRadii;

    /**
     * Maintain a perfect circle (equal x and y radius), regardless of view or custom attributes.
     * The smaller of the two radii will always be used in this case.
     * The default is to be a circle and not an ellipse, due to the behavior of the ellipse.
     */
    private boolean mMaintainEqualCircle;

    /**
     * Once a user has touched the circle, this determines if moving outside the circle is able
     * to change the position of the pointer (and in turn, the progress).
     */
    private boolean mMoveOutsideCircle;

    /**
     * Used for enabling/disabling the lock option for easier hitting of the 0 progress mark.
     * */
    private boolean lockEnabled = true;

    /**
     * Used for when the user moves beyond the start of the circle when moving counter clockwise.
     * Makes it easier to hit the 0 progress mark.
     */
    private boolean lockAtStart = true;

    /**
     * Used for when the user moves beyond the end of the circle when moving clockwise.
     * Makes it easier to hit the 100% (max) progress mark.
     */
    private boolean lockAtEnd = false;

    /**
     * When the user is touching the circle on ACTION_DOWN, this is set to true.
     * Used when touching the CircularSeekBar.
     */
    private boolean mUserIsMovingPointer = false;

    /**
     * Represents the clockwise distance from {@code mStartAngle} to the touch angle.
     * Used when touching the CircularSeekBar.
     */
    private float cwDistanceFromStart;

    /**
     * Represents the counter-clockwise distance from {@code mStartAngle} to the touch angle.
     * Used when touching the CircularSeekBar.
     */
    private float ccwDistanceFromStart;

    /**
     * Represents the clockwise distance from {@code mEndAngle} to the touch angle.
     * Used when touching the CircularSeekBar.
     */
    private float cwDistanceFromEnd;

    /**
     * Represents the counter-clockwise distance from {@code mEndAngle} to the touch angle.
     * Used when touching the CircularSeekBar.
     * Currently unused, but kept just in case.
     */
    @SuppressWarnings("unused")
    private float ccwDistanceFromEnd;

    /**
     * The previous touch action value for {@code cwDistanceFromStart}.
     * Used when touching the CircularSeekBar.
     */
    private float lastCWDistanceFromStart;

    /**
     * Represents the clockwise distance from {@code mPointerPosition} to the touch angle.
     * Used when touching the CircularSeekBar.
     */
    private float cwDistanceFromPointer;

    /**
     * Represents the counter-clockwise distance from {@code mPointerPosition} to the touch angle.
     * Used when touching the CircularSeekBar.
     */
    private float ccwDistanceFromPointer;

    /**
     * True if the user is moving clockwise around the circle, false if moving counter-clockwise.
     * Used when touching the CircularSeekBar.
     */
    private boolean mIsMovingCW;

    /**
     * The width of the circle used in the {@code RectF} that is used to draw it.
     * Based on either the View width or the custom X radius.
     */
    private float mCircleWidth;

    /**
     * The height of the circle used in the {@code RectF} that is used to draw it.
     * Based on either the View width or the custom Y radius.
     */
    private float mCircleHeight;

    /**
     * Represents the progress mark on the circle, in geometric degrees.
     * This is not provided by the user; it is calculated;
     */
    private float mPointerPosition;

    /**
     * Pointer position in terms of X and Y coordinates.
     */
    private final float[] mPointerPositionXY = new float[2];

    /**
     * Listener.
     */
    private OnCircularSeekBarChangeListener mOnCircularSeekBarChangeListener;

    /**
     * Initialize the CircularSeekBar with the attributes from the XML style.
     * Uses the defaults defined at the top of this file when an attribute is not specified by the user.
     * @param attrArray TypedArray containing the attributes.
     */
    private void initAttributes(TypedArray attrArray) {
        mCircleXRadius = attrArray.getFloat(R.styleable.CircularSeekBar_cs_circle_x_radius, DEFAULT_CIRCLE_X_RADIUS) * DPTOPX_SCALE;
        mCircleYRadius = attrArray.getFloat(R.styleable.CircularSeekBar_cs_circle_y_radius, DEFAULT_CIRCLE_Y_RADIUS) * DPTOPX_SCALE;
        mPointerStrokeWidth = attrArray.getFloat(R.styleable.CircularSeekBar_cs_pointer_stroke_width, DEFAULT_POINTER_STROKE_WIDTH) * DPTOPX_SCALE;
        mPointerHaloWidth = attrArray.getFloat(R.styleable.CircularSeekBar_cs_pointer_halo_width, DEFAULT_POINTER_HALO_WIDTH) * DPTOPX_SCALE;
        mPointerHaloBorderWidth = attrArray.getFloat(R.styleable.CircularSeekBar_cs_pointer_halo_border_width, DEFAULT_POINTER_HALO_BORDER_WIDTH) * DPTOPX_SCALE;
        mCircleStrokeWidth = attrArray.getFloat(R.styleable.CircularSeekBar_cs_circle_stroke_width, DEFAULT_CIRCLE_STROKE_WIDTH) * DPTOPX_SCALE;

        int circleStyle = attrArray.getInt(R.styleable.CircularSeekBar_cs_circle_style, DEFAULT_CIRCLE_STYLE);
        mCircleStyle = Paint.Cap.values()[circleStyle];

        String tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_pointer_color);
        if (tempColor != null) {
            try {
                mPointerColor = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mPointerColor = DEFAULT_POINTER_COLOR;
            }
        }

        tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_pointer_halo_color);
        if (tempColor != null) {
            try {
                mPointerHaloColor = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mPointerHaloColor = DEFAULT_POINTER_HALO_COLOR;
            }
        }

        tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_pointer_halo_color_ontouch);
        if (tempColor != null) {
            try {
                mPointerHaloColorOnTouch = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mPointerHaloColorOnTouch = DEFAULT_POINTER_HALO_COLOR_ONTOUCH;
            }
        }

        tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_circle_color);
        if (tempColor != null) {
            try {
                mCircleColor = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mCircleColor = DEFAULT_CIRCLE_COLOR;
            }
        }

        tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_circle_progress_color);
        if (tempColor != null) {
            try {
                mCircleProgressColor = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mCircleProgressColor = DEFAULT_CIRCLE_PROGRESS_COLOR;
            }
        }

        tempColor = attrArray.getString(R.styleable.CircularSeekBar_cs_circle_fill);
        if (tempColor != null) {
            try {
                mCircleFillColor = Color.parseColor(tempColor);
            } catch (IllegalArgumentException e) {
                mCircleFillColor = DEFAULT_CIRCLE_FILL_COLOR;
            }
        }

        mPointerAlpha = Color.alpha(mPointerHaloColor);

        mPointerAlphaOnTouch = attrArray.getInt(R.styleable.CircularSeekBar_cs_pointer_alpha_ontouch, DEFAULT_POINTER_ALPHA_ONTOUCH);
        if (mPointerAlphaOnTouch > 255 || mPointerAlphaOnTouch < 0) {
            mPointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH;
        }

        mMax = attrArray.getInt(R.styleable.CircularSeekBar_cs_max, DEFAULT_MAX);
        mProgress = attrArray.getInt(R.styleable.CircularSeekBar_cs_progress, DEFAULT_PROGRESS);
        mCustomRadii = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_use_custom_radii, DEFAULT_USE_CUSTOM_RADII);
        mMaintainEqualCircle = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_maintain_equal_circle, DEFAULT_MAINTAIN_EQUAL_CIRCLE);
        mMoveOutsideCircle = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_move_outside_circle, DEFAULT_MOVE_OUTSIDE_CIRCLE);
        lockEnabled = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_lock_enabled, DEFAULT_LOCK_ENABLED);
        mDisablePointer = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_disable_pointer, DEFAULT_DISABLE_POINTER);
        negativeEnabled = attrArray.getBoolean(R.styleable.CircularSeekBar_cs_negative_enabled, DEFAULT_NEGATIVE_ENABLED);
        isInNegativeHalf = false;

        // Modulo 360 right now to avoid constant conversion
        mStartAngle = ((360f + (attrArray.getFloat((R.styleable.CircularSeekBar_cs_start_angle), DEFAULT_START_ANGLE) % 360f)) % 360f);
        mEndAngle = ((360f + (attrArray.getFloat((R.styleable.CircularSeekBar_cs_end_angle), DEFAULT_END_ANGLE) % 360f)) % 360f);

        if (mStartAngle == mEndAngle) {
            //mStartAngle = mStartAngle + 1f;
            mEndAngle = mEndAngle - .1f;
        }

        // Modulo 360 right now to avoid constant conversion
        mPointerAngle = ((360f + (attrArray.getFloat((R.styleable.CircularSeekBar_cs_pointer_angle), DEFAULT_POINTER_ANGLE) % 360f)) % 360f);
        if (mPointerAngle == 0f) {
            mPointerAngle = .1f;
        }

        if (mDisablePointer) {
            mPointerStrokeWidth = 0;
            mPointerHaloWidth = 0;
            mPointerHaloBorderWidth = 0;
        }

    }

    /**
     * Initializes the {@code Paint} objects with the appropriate styles.
     */
    private void initPaints() {
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setDither(true);
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setStrokeWidth(mCircleStrokeWidth);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeJoin(Paint.Join.ROUND);
        mCirclePaint.setStrokeCap(mCircleStyle);

        mCircleFillPaint = new Paint();
        mCircleFillPaint.setAntiAlias(true);
        mCircleFillPaint.setDither(true);
        mCircleFillPaint.setColor(mCircleFillColor);
        mCircleFillPaint.setStyle(Paint.Style.FILL);

        mCircleProgressPaint = new Paint();
        mCircleProgressPaint.setAntiAlias(true);
        mCircleProgressPaint.setDither(true);
        mCircleProgressPaint.setColor(mCircleProgressColor);
        mCircleProgressPaint.setStrokeWidth(mCircleStrokeWidth);
        mCircleProgressPaint.setStyle(Paint.Style.STROKE);
        mCircleProgressPaint.setStrokeJoin(Paint.Join.ROUND);
        mCircleProgressPaint.setStrokeCap(mCircleStyle);

        mCircleProgressGlowPaint = new Paint();
        mCircleProgressGlowPaint.set(mCircleProgressPaint);
        mCircleProgressGlowPaint.setMaskFilter(new BlurMaskFilter((5f * DPTOPX_SCALE), BlurMaskFilter.Blur.NORMAL));

        mPointerPaint = new Paint();
        mPointerPaint.setAntiAlias(true);
        mPointerPaint.setDither(true);
        mPointerPaint.setColor(mPointerColor);
        mPointerPaint.setStrokeWidth(mPointerStrokeWidth);
        mPointerPaint.setStyle(Paint.Style.STROKE);
        mPointerPaint.setStrokeJoin(Paint.Join.ROUND);
        mPointerPaint.setStrokeCap(mCircleStyle);

        mPointerHaloPaint = new Paint();
        mPointerHaloPaint.set(mPointerPaint);
        mPointerHaloPaint.setColor(mPointerHaloColor);
        mPointerHaloPaint.setAlpha(mPointerAlpha);
        mPointerHaloPaint.setStrokeWidth(mPointerStrokeWidth + mPointerHaloWidth * 2f);

        mPointerHaloBorderPaint = new Paint();
        mPointerHaloBorderPaint.set(mPointerPaint);
        mPointerHaloBorderPaint.setStrokeWidth(mPointerHaloBorderWidth);
        mPointerHaloBorderPaint.setStyle(Paint.Style.STROKE);

    }

    /**
     * Calculates the total degrees between mStartAngle and mEndAngle, and sets mTotalCircleDegrees
     * to this value.
     */
    private void calculateTotalDegrees() {
        mTotalCircleDegrees = (360f - (mStartAngle - mEndAngle)) % 360f; // Length of the entire circle/arc
        if (mTotalCircleDegrees <= 0f) {
            mTotalCircleDegrees = 360f;
        }
    }

    /**
     * Calculate the degrees that the progress represents. Also called the sweep angle.
     * Sets mProgressDegrees to that value.
     */
    private void calculateProgressDegrees() {
        mProgressDegrees = isInNegativeHalf ? mStartAngle - mPointerPosition : mPointerPosition - mStartAngle; // Verified
        mProgressDegrees = (mProgressDegrees < 0 ? 360f + mProgressDegrees : mProgressDegrees); // Verified
    }

    /**
     * Calculate the pointer position (and the end of the progress arc) in degrees.
     * Sets mPointerPosition to that value.
     */
    private void calculatePointerPosition() {
        float progressPercent = mProgress / mMax;
        float progressDegree = (progressPercent * mTotalCircleDegrees);
        mPointerPosition = mStartAngle + (isInNegativeHalf ? -progressDegree : progressDegree);
        mPointerPosition = (mPointerPosition < 0 ? 360f + mPointerPosition : mPointerPosition) % 360f;
    }

    private void calculatePointerXYPosition() {
        PathMeasure pm = new PathMeasure(mCircleProgressPath, false);
        boolean returnValue = pm.getPosTan(pm.getLength(), mPointerPositionXY, null);
        if (!returnValue) {
            pm = new PathMeasure(mCirclePath, false);
            returnValue = pm.getPosTan(0, mPointerPositionXY, null);
        }
    }

    /**
     * Initialize the {@code Path} objects with the appropriate values.
     */
    private void initPaths() {
        if (isInNegativeHalf) {
            mCirclePath = new Path();
            mCirclePath.addArc(mCircleRectF, mStartAngle - mTotalCircleDegrees, mTotalCircleDegrees);

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            float extendStart = mStartAngle - mProgressDegrees - mPointerAngle / 2.0f;
            float extendDegrees = mProgressDegrees + mPointerAngle;
            mCircleProgressPath = new Path();
            mCircleProgressPath.addArc(mCircleRectF, extendStart, extendDegrees);

            float pointerStart = mPointerPosition - mPointerAngle / 2.0f;
            mCirclePonterPath = new Path();
            mCirclePonterPath.addArc(mCircleRectF, pointerStart, mPointerAngle);
        } else {
            mCirclePath = new Path();
            mCirclePath.addArc(mCircleRectF, mStartAngle, mTotalCircleDegrees);

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            float extendStart = mStartAngle - mPointerAngle / 2.0f;
            mCircleProgressPath = new Path();
            mCircleProgressPath.addArc(mCircleRectF, extendStart, mProgressDegrees + mPointerAngle);

            float pointerStart = mPointerPosition - mPointerAngle / 2.0f;
            mCirclePonterPath = new Path();
            mCirclePonterPath.addArc(mCircleRectF, pointerStart, mPointerAngle);
        }
    }

    /**
     * Initialize the {@code RectF} objects with the appropriate values.
     */
    private void initRects() {
        mCircleRectF.set(-mCircleWidth, -mCircleHeight, mCircleWidth, mCircleHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(this.getWidth() / 2, this.getHeight() / 2);

        canvas.drawPath(mCirclePath, mCirclePaint);
        canvas.drawPath(mCircleProgressPath, mCircleProgressGlowPaint);
        canvas.drawPath(mCircleProgressPath, mCircleProgressPaint);

        canvas.drawPath(mCirclePath, mCircleFillPaint);

        if (!mDisablePointer) {
            if (mUserIsMovingPointer) {
                canvas.drawPath(mCirclePonterPath, mPointerHaloPaint);
            }
            canvas.drawPath(mCirclePonterPath, mPointerPaint);
            // TODO, find a good way to draw halo border.
//            if (mUserIsMovingPointer) {
//                canvas.drawCircle(mPointerPositionXY[0], mPointerPositionXY[1],
//                        (mPointerStrokeWidth /2f) + mPointerHaloWidth + (mPointerHaloBorderWidth / 2f),
//                        mPointerHaloBorderPaint);
//            }
        }
    }

    /**
     * Get the progress of the CircularSeekBar.
     * @return The progress of the CircularSeekBar.
     */
    public float getProgress() {
        float progress = mMax * mProgressDegrees / mTotalCircleDegrees;
        return isInNegativeHalf ? -progress : progress;
    }

    /**
     * Set the progress of the CircularSeekBar.
     * If the progress is the same, then any listener will not receive a onProgressChanged event.
     * @param progress The progress to set the CircularSeekBar to.
     */
    public void setProgress(float progress) {
        if (mProgress != progress) {
            if (negativeEnabled) {
                if (progress < 0) {
                    mProgress = -progress;
                    isInNegativeHalf = true;
                } else {
                    mProgress = progress;
                    isInNegativeHalf = false;
                }
            } else {
                mProgress = progress;
            }
            if (mOnCircularSeekBarChangeListener != null) {
                mOnCircularSeekBarChangeListener.onProgressChanged(this, progress, false);
            }

            recalculateAll();
            invalidate();
        }
    }

    private void setProgressBasedOnAngle(float angle) {
        mPointerPosition = angle;
        calculateProgressDegrees();
        mProgress = mMax * mProgressDegrees / mTotalCircleDegrees;
    }

    private void recalculateAll() {
        calculateTotalDegrees();
        calculatePointerPosition();
        calculateProgressDegrees();

        initRects();

        initPaths();

        calculatePointerXYPosition();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        if (mMaintainEqualCircle) {
            int min = Math.min(width, height);
            setMeasuredDimension(min, min);
        } else {
            setMeasuredDimension(width, height);
        }

        // Set the circle width and height based on the view for the moment
        float padding = Math.max(mCircleStrokeWidth / 2f,
                mPointerStrokeWidth + mPointerHaloWidth + mPointerHaloBorderWidth);
        mCircleHeight = height / 2f - padding;
        mCircleWidth = width / 2f - padding;

        // If it is not set to use custom
        if (mCustomRadii) {
            // Check to make sure the custom radii are not out of the view. If they are, just use the view values
            if ((mCircleYRadius - padding) < mCircleHeight) {
                mCircleHeight = mCircleYRadius - padding;
            }

            if ((mCircleXRadius - padding) < mCircleWidth) {
                mCircleWidth = mCircleXRadius - padding;
            }
        }

        if (mMaintainEqualCircle) { // Applies regardless of how the values were determined
            float min = Math.min(mCircleHeight, mCircleWidth);
            mCircleHeight = min;
            mCircleWidth = min;
        }

        recalculateAll();
    }

    public boolean isLockEnabled() {
        return lockEnabled;
    }

    public void setLockEnabled(boolean lockEnabled) {
        this.lockEnabled = lockEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDisablePointer)
            return false;

        // Convert coordinates to our internal coordinate system
        float x = event.getX() - getWidth() / 2;
        float y = event.getY() - getHeight() / 2;

        // Get the distance from the center of the circle in terms of x and y
        float distanceX = mCircleRectF.centerX() - x;
        float distanceY = mCircleRectF.centerY() - y;

        // Get the distance from the center of the circle in terms of a radius
        float touchEventRadius = (float) Math.sqrt((Math.pow(distanceX, 2) + Math.pow(distanceY, 2)));

        float minimumTouchTarget = MIN_TOUCH_TARGET_DP * DPTOPX_SCALE; // Convert minimum touch target into px
        float additionalRadius; // Either uses the minimumTouchTarget size or larger if the ring/pointer is larger

        if (mCircleStrokeWidth < minimumTouchTarget) { // If the width is less than the minimumTouchTarget, use the minimumTouchTarget
            additionalRadius = minimumTouchTarget / 2;
        }
        else {
            additionalRadius = mCircleStrokeWidth / 2; // Otherwise use the width
        }
        float outerRadius = Math.max(mCircleHeight, mCircleWidth) + additionalRadius; // Max outer radius of the circle, including the minimumTouchTarget or wheel width
        float innerRadius = Math.min(mCircleHeight, mCircleWidth) - additionalRadius; // Min inner radius of the circle, including the minimumTouchTarget or wheel width

        if (mPointerStrokeWidth < (minimumTouchTarget / 2)) { // If the pointer radius is less than the minimumTouchTarget, use the minimumTouchTarget
            additionalRadius = minimumTouchTarget / 2;
        }
        else {
            additionalRadius = mPointerStrokeWidth; // Otherwise use the radius
        }

        float touchAngle;
        touchAngle = (float) ((Math.atan2(y, x) / Math.PI * 180) % 360); // Verified
        touchAngle = (touchAngle < 0 ? 360 + touchAngle : touchAngle); // Verified

        cwDistanceFromStart = touchAngle - mStartAngle; // Verified
        cwDistanceFromStart = (cwDistanceFromStart < 0 ? 360f + cwDistanceFromStart : cwDistanceFromStart); // Verified
        ccwDistanceFromStart = 360f - cwDistanceFromStart; // Verified

        cwDistanceFromEnd = touchAngle - mEndAngle; // Verified
        cwDistanceFromEnd = (cwDistanceFromEnd < 0 ? 360f + cwDistanceFromEnd : cwDistanceFromEnd); // Verified
        ccwDistanceFromEnd = 360f - cwDistanceFromEnd; // Verified

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // These are only used for ACTION_DOWN for handling if the pointer was the part that was touched
            float pointerRadiusDegrees = (float) ((mPointerStrokeWidth * 180) / (Math.PI * Math.max(mCircleHeight, mCircleWidth)));
            float pointerDegrees = Math.max( pointerRadiusDegrees, (mPointerAngle / 2f) );
            cwDistanceFromPointer = touchAngle - mPointerPosition;
            cwDistanceFromPointer = (cwDistanceFromPointer < 0 ? 360f + cwDistanceFromPointer : cwDistanceFromPointer);
            ccwDistanceFromPointer = 360f - cwDistanceFromPointer;
            // This is for if the first touch is on the actual pointer.
            if ( ((touchEventRadius >= innerRadius) && (touchEventRadius <= outerRadius)) &&
                    ((cwDistanceFromPointer <= pointerDegrees) || (ccwDistanceFromPointer <= pointerDegrees)) ) {
                setProgressBasedOnAngle(mPointerPosition);
                lastCWDistanceFromStart = cwDistanceFromStart;
                mIsMovingCW = true;
                mPointerHaloPaint.setAlpha(mPointerAlphaOnTouch);
                mPointerHaloPaint.setColor(mPointerHaloColorOnTouch);
                recalculateAll();
                invalidate();
                if (mOnCircularSeekBarChangeListener != null) {
                    mOnCircularSeekBarChangeListener.onStartTrackingTouch(this);
                }
                mUserIsMovingPointer = true;
                lockAtEnd = false;
                lockAtStart = false;
            } else if (cwDistanceFromStart > mTotalCircleDegrees) { // If the user is touching outside of the start AND end
                mUserIsMovingPointer = false;
                return false;
            } else if ((touchEventRadius >= innerRadius) && (touchEventRadius <= outerRadius)) { // If the user is touching near the circle
                setProgressBasedOnAngle(touchAngle);
                lastCWDistanceFromStart = cwDistanceFromStart;
                mIsMovingCW = true;
                mPointerHaloPaint.setAlpha(mPointerAlphaOnTouch);
                mPointerHaloPaint.setColor(mPointerHaloColorOnTouch);
                recalculateAll();
                invalidate();
                if (mOnCircularSeekBarChangeListener != null) {
                    mOnCircularSeekBarChangeListener.onStartTrackingTouch(this);
                    mOnCircularSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                }
                mUserIsMovingPointer = true;
                lockAtEnd = false;
                lockAtStart = false;
            } else { // If the user is not touching near the circle
                mUserIsMovingPointer = false;
                return false;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mUserIsMovingPointer) {
                float smallInCircle = mTotalCircleDegrees / 3f;
                float cwPointerFromStart = mPointerPosition - mStartAngle;
                cwPointerFromStart = cwPointerFromStart < 0 ? cwPointerFromStart + 360f : cwPointerFromStart;

                boolean touchNearStart = cwDistanceFromStart < smallInCircle;
                boolean touchNearEnd = ccwDistanceFromEnd < smallInCircle;
                boolean pointerNearStart = cwPointerFromStart < smallInCircle;
                boolean pointerNearEnd = cwPointerFromStart > (mTotalCircleDegrees - smallInCircle);
                boolean progressNearZero = mProgress < mMax / 3f;
                boolean progressNearMax = mProgress > mMax / 3f * 2f;

                if (progressNearMax) {  // logic for end lock.
                    if (pointerNearStart) { // negative end
                        if (touchNearStart)
                            lockAtEnd = false;
                        else if (touchNearEnd) {
                            lockAtEnd = true;
                            lockAtStart = false;
                        }
                    } else if (pointerNearEnd) {    // positive end
                        if (touchNearEnd)
                            lockAtEnd = false;
                        else if (touchNearStart) {
                            lockAtEnd = true;
                            lockAtStart = false;
                        }
                    }
                } else if (progressNearZero && negativeEnabled) {   // logic for negative flip
                    if (touchNearStart)
                        isInNegativeHalf = false;
                    else if (touchNearEnd) {
                        isInNegativeHalf = true;
                    }
                } else if (progressNearZero && !negativeEnabled) {  // logic for start lock
                    if (pointerNearStart) {
                        if (touchNearStart)
                            lockAtStart = false;
                        else if (touchNearEnd) {
                            lockAtStart = true;
                            lockAtEnd = false;
                        }
                    }
                }

                if (lockAtStart && lockEnabled) {
                    // TODO: Add a check if mProgress is already 0, in which case don't call the listener
                    mProgress = 0;
                    recalculateAll();
                    invalidate();
                    if (mOnCircularSeekBarChangeListener != null) {
                        mOnCircularSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                    }

                } else if (lockAtEnd && lockEnabled) {
                    mProgress = mMax;
                    recalculateAll();
                    invalidate();
                    if (mOnCircularSeekBarChangeListener != null) {
                        mOnCircularSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                    }
                } else if ((mMoveOutsideCircle) || (touchEventRadius <= outerRadius)) {
                    if (!(cwDistanceFromStart > mTotalCircleDegrees)) {
                        setProgressBasedOnAngle(touchAngle);
                    }
                    recalculateAll();
                    invalidate();
                    if (mOnCircularSeekBarChangeListener != null) {
                        mOnCircularSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                    }
                } else {
                    break;
                }

                lastCWDistanceFromStart = cwDistanceFromStart;
            } else {
                return false;
            }
            break;
        case MotionEvent.ACTION_UP:
            mPointerHaloPaint.setAlpha(mPointerAlpha);
            mPointerHaloPaint.setColor(mPointerHaloColor);
            if (mUserIsMovingPointer) {
                mUserIsMovingPointer = false;
                invalidate();
                if (mOnCircularSeekBarChangeListener != null) {
                    mOnCircularSeekBarChangeListener.onStopTrackingTouch(this);
                }
            } else {
                return false;
            }
            break;
        case MotionEvent.ACTION_CANCEL: // Used when the parent view intercepts touches for things like scrolling
            mPointerHaloPaint.setAlpha(mPointerAlpha);
            mPointerHaloPaint.setColor(mPointerHaloColor);
            mUserIsMovingPointer = false;
            invalidate();
            break;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        return true;
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.CircularSeekBar, defStyle, 0);

        initAttributes(attrArray);

        attrArray.recycle();

        initPaints();
    }

    public CircularSeekBar(Context context) {
        super(context);
        init(null, 0);
    }

    public CircularSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircularSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable("PARENT", superState);
        state.putFloat("MAX", mMax);
        state.putFloat("PROGRESS", mProgress);
        state.putInt("mCircleColor", mCircleColor);
        state.putInt("mCircleProgressColor", mCircleProgressColor);
        state.putInt("mPointerColor", mPointerColor);
        state.putInt("mPointerHaloColor", mPointerHaloColor);
        state.putInt("mPointerHaloColorOnTouch", mPointerHaloColorOnTouch);
        state.putInt("mPointerAlpha", mPointerAlpha);
        state.putInt("mPointerAlphaOnTouch", mPointerAlphaOnTouch);
        state.putFloat("mPointerAngle", mPointerAngle);
        state.putBoolean("mDisablePointer", mDisablePointer);
        state.putBoolean("lockEnabled", lockEnabled);
        state.putBoolean("negativeEnabled", negativeEnabled);
        state.putBoolean("isInNegativeHalf", isInNegativeHalf);
        state.putInt("mCircleStyle", mCircleStyle.ordinal());

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;

        Parcelable superState = savedState.getParcelable("PARENT");
        super.onRestoreInstanceState(superState);

        mMax = savedState.getFloat("MAX");
        mProgress = savedState.getFloat("PROGRESS");
        mCircleColor = savedState.getInt("mCircleColor");
        mCircleProgressColor = savedState.getInt("mCircleProgressColor");
        mPointerColor = savedState.getInt("mPointerColor");
        mPointerHaloColor = savedState.getInt("mPointerHaloColor");
        mPointerHaloColorOnTouch = savedState.getInt("mPointerHaloColorOnTouch");
        mPointerAlpha = savedState.getInt("mPointerAlpha");
        mPointerAlphaOnTouch = savedState.getInt("mPointerAlphaOnTouch");
        mPointerAngle = savedState.getFloat("mPointerAngle");
        lockEnabled = savedState.getBoolean("lockEnabled");
        mDisablePointer = savedState.getBoolean("mDisablePointer");
        negativeEnabled = savedState.getBoolean("negativeEnabled");
        isInNegativeHalf = savedState.getBoolean("isInNegativeHalf");
        mCircleStyle = Paint.Cap.values()[savedState.getInt("mCircleStyle")];

        initPaints();

        recalculateAll();
    }


    public void setOnSeekBarChangeListener(OnCircularSeekBarChangeListener l) {
        mOnCircularSeekBarChangeListener = l;
    }

    /**
    * Listener for the CircularSeekBar. Implements the same methods as the normal OnSeekBarChangeListener.
    */
    public interface OnCircularSeekBarChangeListener {

        public abstract void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser);

        public abstract void onStopTrackingTouch(CircularSeekBar seekBar);

        public abstract void onStartTrackingTouch(CircularSeekBar seekBar);
    }

    /**
     * Sets the circle color.
     * @param color the color of the circle
     */
    public void setCircleColor(int color) {
        mCircleColor = color;
        mCirclePaint.setColor(mCircleColor);
        invalidate();
    }

    /**
     * Gets the circle color.
     * @return An integer color value for the circle
     */
    public int getCircleColor() {
        return mCircleColor;
    }

    /**
     * Sets the circle progress color.
     * @param color the color of the circle progress
     */
    public void setCircleProgressColor(int color) {
        mCircleProgressColor = color;
        mCircleProgressPaint.setColor(mCircleProgressColor);
        invalidate();
    }

    /**
     * Gets the circle progress color.
     * @return An integer color value for the circle progress
     */
    public int getCircleProgressColor() {
        return mCircleProgressColor;
    }

    /**
     * Sets the pointer color.
     * @param color the color of the pointer
     */
    public void setPointerColor(int color) {
        mPointerColor = color;
        mPointerPaint.setColor(mPointerColor);
        invalidate();
    }

    /**
     * Gets the pointer color.
     * @return An integer color value for the pointer
     */
    public int getPointerColor() {
        return mPointerColor;
    }

    /**
     * Sets the pointer halo color.
     * @param color the color of the pointer halo
     */
    public void setPointerHaloColor(int color) {
        mPointerHaloColor = color;
        mPointerHaloPaint.setColor(mPointerHaloColor);
        invalidate();
    }

    /**
     * Gets the pointer halo color.
     * @return An integer color value for the pointer halo
     */
    public int getPointerHaloColor() {
        return mPointerHaloColor;
    }

    /**
     * Sets the pointer alpha.
     * @param alpha the alpha of the pointer
     */
    public void setPointerAlpha(int alpha) {
        if (alpha >=0 && alpha <= 255) {
            mPointerAlpha = alpha;
            mPointerHaloPaint.setAlpha(mPointerAlpha);
            invalidate();
        }
    }

    /**
     * Gets the pointer alpha value.
     * @return An integer alpha value for the pointer (0..255)
     */
    public int getPointerAlpha() {
        return mPointerAlpha;
    }

    /**
     * Sets the pointer alpha when touched.
     * @param alpha the alpha of the pointer (0..255) when touched
     */
    public void setPointerAlphaOnTouch(int alpha) {
        if (alpha >=0 && alpha <= 255) {
            mPointerAlphaOnTouch = alpha;
        }
    }

    /**
     * Gets the pointer alpha value when touched.
     * @return An integer alpha value for the pointer (0..255) when touched
     */
    public int getPointerAlphaOnTouch() {
        return mPointerAlphaOnTouch;
    }

    /**
     * Sets the pointer angle.
     * @param angle the angle of the pointer
     */
    public void setPointerAngle(float angle) {
        // Modulo 360 right now to avoid constant conversion
        angle = ((360f + (angle % 360f)) % 360f);
        if (angle == 0f) {
            angle = .1f;
        }
        if (angle != mPointerAngle) {
            mPointerAngle = angle;
            recalculateAll();
            invalidate();
        }
    }

    /**
     * Gets the pointer angle.
     * @return Angle for the pointer (0..360)
     */
    public float getPointerAngle() {
        return mPointerAngle;
    }

    /**
     * Sets the circle fill color.
     * @param color the color of the circle fill
     */
    public void setCircleFillColor(int color) {
        mCircleFillColor = color;
        mCircleFillPaint.setColor(mCircleFillColor);
        invalidate();
    }

    /**
     * Gets the circle fill color.
     * @return An integer color value for the circle fill
     */
    public int getCircleFillColor() {
        return mCircleFillColor;
    }

    /**
     * Set the max of the CircularSeekBar.
     * If the new max is less than the current progress, then the progress will be set to zero.
     * If the progress is changed as a result, then any listener will receive a onProgressChanged event.
     * @param max The new max for the CircularSeekBar.
     */
    public void setMax(float max) {
        if (max > 0) { // Check to make sure it's greater than zero
            if (max <= mProgress) {
                mProgress = 0; // If the new max is less than current progress, set progress to zero
                if (mOnCircularSeekBarChangeListener != null) {
                    mOnCircularSeekBarChangeListener.onProgressChanged(this, isInNegativeHalf ? -mProgress : mProgress, false);
                }
            }
            mMax = max;

            recalculateAll();
            invalidate();
        }
    }

    /**
     * Get the current max of the CircularSeekBar.
     * @return Synchronized integer value of the max.
     */
    public synchronized float getMax() {
        return mMax;
    }

}
