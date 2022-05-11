/*
 *
 * Copyright 2013 Matt Joseph
 * Copyright 2018 Tankery Chen
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
package me.tankery.lib.circularseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.Paint.Cap
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CircularSeekBarV2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {
    /**
     * Used to scale the dp units to pixels
     */
    private val DPTOPX_SCALE = resources.displayMetrics.density

    /**
     * `Paint` instance used to draw the inactive circle.
     */
    private val mCirclePaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the circle fill.
     */
    private val mCircleFillPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the active circle (represents progress).
     */
    private val mCircleProgressPaint: Paint = Paint()

    /**
     * If progress glow is disabled, there is no glow from the progress bar when filled
     *
     * NOTE: To enable glow effect, please make sure this view is rendering with hardware
     * accelerate disabled. (Checkout this doc for details of hardware accelerate:
     * https://developer.android.com/guide/topics/graphics/hardware-accel)
     */
    private var mDisableProgressGlow = false

    /**
     * `Paint` instance used to draw the glow from the active circle.
     */
    private val mCircleProgressGlowPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the center of the pointer.
     * Note: This is broken on 4.0+, as BlurMasks do not work with hardware acceleration.
     */
    private val mPointerPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the halo of the pointer.
     * Note: The halo is the part that changes transparency.
     */
    private val mPointerHaloPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the border of the pointer, outside of the halo.
     */
    private val mPointerHaloBorderPaint: Paint = Paint()

    /**
     * The style of the circle, can be butt, round or square.
     */
    var mCircleStyle: Cap = Cap.ROUND
        set(style) {
            field = style
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * current in negative half cycle.
     */
    private var mIsInNegativeHalf = false

    /**
     * The width of the circle (in pixels).
     * Sets the circle stroke width.
     * @param width the width of the circle
     */
    var mCircleStrokeWidth = 0f
        set(width) {
            field = width
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * The X radius of the circle (in pixels).
     */
    private var mCircleXRadius = 0f

    /**
     * The Y radius of the circle (in pixels).
     */
    private var mCircleYRadius = 0f

    /**
     * If disable pointer, we can't seek the progress.
     */
    private var mDisablePointer = false

    /**
     * The radius of the pointer (in pixels).
     *
     * Sets the pointer pointer stroke width.
     * @param width the width of the pointer
     */
    var mPointerStrokeWidth = 0f
        set(width) {
            field = width
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * The width of the pointer halo (in pixels).
     */
    private var mPointerHaloWidth = 0f

    /**
     * The width of the pointer halo border (in pixels).
     */
    private var mPointerHaloBorderWidth = 0f

    /**
     * Angle of the pointer arc.
     * Default is 0, the pointer is a circle when angle is 0 and the style is round.
     * Can not be less than 0. can not be greater than 360.
     *
     * Gets the pointer angle.
     * @return Angle for the pointer (0..360)
     */
    var mPointerAngle = 0f
        set(angle) {
            // Modulo 360 right now to avoid constant conversion
            var normalizedAngle = (360f + angle % 360f) % 360f
            if (normalizedAngle == 0f) {
                normalizedAngle = SMALL_DEGREE_BIAS
            }
            if (normalizedAngle != field) {
                field = normalizedAngle
                recalculateAll()
                invalidate()
            }
        }

    /**
     * Start angle of the CircularSeekBar.
     * Note: If mStartAngle and mEndAngle are set to the same angle, 0.1 is subtracted
     * from the mEndAngle to make the circle function properly.
     */
    var mStartAngle = 0f
        set(angle) {
            field = angle
            if (angle % 360f == mEndAngle % 360f) {
                //mStartAngle = mStartAngle + 1f;
                mEndAngle -= SMALL_DEGREE_BIAS
            }
            recalculateAll()
            invalidate()
        }

    /**
     * End angle of the CircularSeekBar.
     * Note: If mStartAngle and mEndAngle are set to the same angle, 0.1 is subtracted
     * from the mEndAngle to make the circle function properly.
     */
    var mEndAngle = 0f
        set(angle) {
            field = if (mStartAngle % 360f == mEndAngle % 360f) {
                //mStartAngle = mStartAngle + 1f;
                angle - SMALL_DEGREE_BIAS
            } else {
                angle
            }
            recalculateAll()
            invalidate()
        }

    /**
     * `RectF` that represents the circle (or ellipse) of the seekbar.
     */
    val pathCircle: RectF = RectF()

    /**
     * Holds the color value for `mPointerPaint` before the `Paint` instance is created.
     *
     * Gets the pointer color.
     *
     * Sets the pointer color.
     * @param color the color of the pointer
     * @return An integer color value for the pointer
     */
    var mPointerColor = DEFAULT_POINTER_COLOR
        set(color) {
            field = color
            mPointerPaint.color = color
            invalidate()
        }

    /**
     * Holds the color value for `mPointerHaloPaint` before the `Paint` instance is created.
     *
     * Gets the pointer halo color.
     *
     * Sets the pointer halo color.
     *
     * @param color the color of the pointer halo
     * @return An integer color value for the pointer halo
     */
    var mPointerHaloColor = DEFAULT_POINTER_HALO_COLOR
        set(color) {
            field = color
            mPointerHaloPaint.color = field
            invalidate()
        }

    /**
     * Holds the color value for `mPointerHaloPaint` before the `Paint` instance is created.
     */
    private var mPointerHaloColorOnTouch = DEFAULT_POINTER_HALO_COLOR_ONTOUCH

    /**
     * Holds the color value for `mCirclePaint` before the `Paint` instance is created.
     *
     * Gets the circle color.
     *
     * Sets the circle color.
     * @param color the color of the circle
     * @return An integer color value for the circle
     */
    var mCircleColor = DEFAULT_CIRCLE_COLOR
        set(color) {
            field = color
            mCirclePaint.color = color
            invalidate()
        }

    /**
     * Holds the color value for `mCircleFillPaint` before the `Paint` instance is created.
     *
     * Gets the circle fill color.
     *
     * Sets the circle fill color.
     * @param color the color of the circle fill
     * @return An integer color value for the circle fill
     */
    var mCircleFillColor = DEFAULT_CIRCLE_FILL_COLOR
        set(color) {
            field = color
            mCircleFillPaint.color = color
            invalidate()
        }

    /**
     * Holds the color value for `mCircleProgressPaint` before the `Paint` instance is created.
     *
     * Gets the circle progress color.
     *
     * Sets the circle progress color.
     * @param color the color of the circle progress
     * @return An integer color value for the circle progress
     */
    var mCircleProgressColor = DEFAULT_CIRCLE_PROGRESS_COLOR
        set(color) {
            field = color
            mCircleProgressPaint.color = color
            invalidate()
        }

    /**
     * Holds the alpha value for `mPointerHaloPaint`.
     *
     * Gets the pointer alpha value.
     *
     * Sets the pointer alpha.
     * @param alpha the alpha of the pointer
     * @return An integer alpha value for the pointer (0..255)
     */
    var mPointerAlpha = DEFAULT_POINTER_ALPHA
        set(alpha) {
            if (alpha in 0..255) {
                field = alpha
                mPointerHaloPaint.alpha = alpha
                invalidate()
            }
        }

    /**
     * Holds the OnTouch alpha value for `mPointerHaloPaint`.
     *
     * Gets the pointer alpha value when touched.
     *
     * Sets the pointer alpha when touched.
     * @param alpha the alpha of the pointer (0..255) when touched
     * @return An integer alpha value for the pointer (0..255) when touched
     */
    var mPointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH
        set(alpha) {
            if (alpha in 0..255) {
                field = alpha
            }
        }

    /**
     * Distance (in degrees) that the the circle/semi-circle makes up.
     * This amount represents the max of the circle in degrees.
     */
    private var mTotalCircleDegrees = 0f

    /**
     * Distance (in degrees) that the current progress makes up in the circle.
     */
    private var mProgressDegrees = 0f

    /**
     * `Path` used to draw the circle/semi-circle.
     */
    private val mCirclePath: Path = Path()

    /**
     * `Path` used to draw the progress on the circle.
     */
    private val mCircleProgressPath: Path = Path()

    /**
     * `Path` used to draw the pointer arc on the circle.
     */
    private val mCirclePointerPath: Path = Path()

    /**
     * Max value that this CircularSeekBar is representing.
     *
     * Get the current max of the CircularSeekBar.
     *
     * Set the max of the CircularSeekBar.
     * If the new max is less than the current progress, then the progress will be set to zero.
     * If the progress is changed as a result, then any listener will receive a onProgressChanged event.
     * @param max The new max for the CircularSeekBar.
     * @return Synchronized integer value of the max.
     */
    @get:Synchronized
    var mMax = 0f
        set(max) {
            if (max > 0) {
                if (max <= mProgress) {
                    mProgress = 0f // If the new max is less than current progress, set progress to zero
                    mOnCircularSeekBarChangeListener?.onProgressChanged(
                        this,
                        if (mIsInNegativeHalf) -mProgress else mProgress,
                        false
                    )
                }
                field = max
                recalculateAll()
                invalidate()
            }
        }

    /**
     * Progress value that this CircularSeekBar is representing.
     */
    private var mProgress = 0f

    /**
     * Used for enabling/disabling the negative progress bar.
     */
    var isNegativeEnabled = false

    /**
     * If true, then the user can specify the X and Y radii.
     * If false, then the View itself determines the size of the CircularSeekBar.
     */
    private var mCustomRadii = false

    /**
     * Maintain a perfect circle (equal x and y radius), regardless of view or custom attributes.
     * The smaller of the two radii will always be used in this case.
     * The default is to be a circle and not an ellipse, due to the behavior of the ellipse.
     */
    private var mMaintainEqualCircle = false

    /**
     * Once a user has touched the circle, this determines if moving outside the circle is able
     * to change the position of the pointer (and in turn, the progress).
     */
    private var mMoveOutsideCircle = false

    /**
     * Used for enabling/disabling the lock option for easier hitting of the 0 progress mark.
     */
    var isLockEnabled = true

    /**
     * Used for when the user moves beyond the start of the circle when moving counter clockwise.
     * Makes it easier to hit the 0 progress mark.
     */
    private var mLockAtStart = true

    /**
     * Used for when the user moves beyond the end of the circle when moving clockwise.
     * Makes it easier to hit the 100% (max) progress mark.
     */
    private var mLockAtEnd = false

    /**
     * If progress is zero, hide the progress bar.
     */
    private var mHideProgressWhenEmpty = false

    /**
     * When the user is touching the circle on ACTION_DOWN, this is set to true.
     * Used when touching the CircularSeekBar.
     */
    private var mUserIsMovingPointer = false

    /**
     * The width of the circle used in the `RectF` that is used to draw it.
     * Based on either the View width or the custom X radius.
     */
    private var mCircleWidth = 0f

    /**
     * The height of the circle used in the `RectF` that is used to draw it.
     * Based on either the View width or the custom Y radius.
     */
    private var mCircleHeight = 0f

    /**
     * Represents the progress mark on the circle, in geometric degrees.
     * This is not provided by the user; it is calculated;
     */
    private var mPointerPosition = 0f

    /**
     * Pointer position in terms of X and Y coordinates.
     */
    private val mPointerPositionXY = FloatArray(2)

    /**
     * Listener.
     */
    private var mOnCircularSeekBarChangeListener: OnCircularSeekBarChangeListener? = null

    init {
        val attrArray = context.theme.obtainStyledAttributes(attrs, R.styleable.cs_CircularSeekBar, defStyleAttr, defStyleRes)
        initPaints()
        initAttributes(attrArray)
        attrArray.recycle()
    }

    /**
     * Initialize the CircularSeekBar with the attributes from the XML style.
     * Uses the defaults defined at the top of this file when an attribute is not specified by the user.
     * @param attrArray TypedArray containing the attributes.
     */
    private fun initAttributes(attrArray: TypedArray) {
        mCircleXRadius = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_x_radius,
            DEFAULT_CIRCLE_X_RADIUS
        )
        mCircleYRadius = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_y_radius,
            DEFAULT_CIRCLE_Y_RADIUS
        )
        mPointerStrokeWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_stroke_width,
            DEFAULT_POINTER_STROKE_WIDTH
        )
        mPointerHaloWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_width,
            DEFAULT_POINTER_HALO_WIDTH
        )
        mPointerHaloBorderWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_border_width,
            DEFAULT_POINTER_HALO_BORDER_WIDTH
        )
        mCircleStrokeWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_stroke_width,
            DEFAULT_CIRCLE_STROKE_WIDTH
        )
        val circleStyle =
            attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_circle_style, DEFAULT_CIRCLE_STYLE)
        mCircleStyle = Cap.values()[circleStyle]
        mPointerColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_color,
            DEFAULT_POINTER_COLOR
        )
        mPointerHaloColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_color,
            DEFAULT_POINTER_HALO_COLOR
        )
        mPointerHaloColorOnTouch = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_color_ontouch,
            DEFAULT_POINTER_HALO_COLOR_ONTOUCH
        )
        mCircleColor =
            attrArray.getColor(R.styleable.cs_CircularSeekBar_cs_circle_color, DEFAULT_CIRCLE_COLOR)
        mCircleProgressColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_circle_progress_color,
            DEFAULT_CIRCLE_PROGRESS_COLOR
        )
        mCircleFillColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_circle_fill,
            DEFAULT_CIRCLE_FILL_COLOR
        )
        mPointerAlpha = Color.alpha(mPointerHaloColor)
        mPointerAlphaOnTouch = attrArray.getInt(
            R.styleable.cs_CircularSeekBar_cs_pointer_alpha_ontouch,
            DEFAULT_POINTER_ALPHA_ONTOUCH
        )
        if (mPointerAlphaOnTouch > 255 || mPointerAlphaOnTouch < 0) {
            mPointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH
        }
        mMax = attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_max, DEFAULT_MAX).toFloat()
        mProgress =
            attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_progress, DEFAULT_PROGRESS).toFloat()
        mCustomRadii = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_use_custom_radii,
            DEFAULT_USE_CUSTOM_RADII
        )
        mMaintainEqualCircle = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_maintain_equal_circle,
            DEFAULT_MAINTAIN_EQUAL_CIRCLE
        )
        mMoveOutsideCircle = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_move_outside_circle,
            DEFAULT_MOVE_OUTSIDE_CIRCLE
        )
        isLockEnabled = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_lock_enabled,
            DEFAULT_LOCK_ENABLED
        )
        mDisablePointer = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_disable_pointer,
            DEFAULT_DISABLE_POINTER
        )
        isNegativeEnabled = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_negative_enabled,
            DEFAULT_NEGATIVE_ENABLED
        )
        mIsInNegativeHalf = false
        mDisableProgressGlow = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_disable_progress_glow,
            DEFAULT_DISABLE_PROGRESS_GLOW
        )
        mHideProgressWhenEmpty = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_hide_progress_when_empty,
            DEFAULT_CS_HIDE_PROGRESS_WHEN_EMPTY
        )

        // Modulo 360 right now to avoid constant conversion
        mStartAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_start_angle,
            DEFAULT_START_ANGLE
        ) % 360f) % 360f
        mEndAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_end_angle,
            DEFAULT_END_ANGLE
        ) % 360f) % 360f

        // Disable negative progress if is semi-oval.
        if (mStartAngle != mEndAngle) {
            isNegativeEnabled = false
        }
        if (mStartAngle % 360f == mEndAngle % 360f) {
            //mStartAngle = mStartAngle + 1f;
            mEndAngle = mEndAngle - SMALL_DEGREE_BIAS
        }

        // Modulo 360 right now to avoid constant conversion
        mPointerAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_pointer_angle,
            DEFAULT_POINTER_ANGLE
        ) % 360f) % 360f
        if (mPointerAngle == 0f) {
            mPointerAngle = SMALL_DEGREE_BIAS
        }
        if (mDisablePointer) {
            mPointerStrokeWidth = 0f
            mPointerHaloWidth = 0f
            mPointerHaloBorderWidth = 0f
        }
    }

    /**
     * Initializes the `Paint` objects with the appropriate styles.
     */
    private fun initPaints() {
        mCirclePaint.isAntiAlias = true
        mCirclePaint.isDither = true
        mCirclePaint.color = mCircleColor
        mCirclePaint.strokeWidth = mCircleStrokeWidth
        mCirclePaint.style = Paint.Style.STROKE
        mCirclePaint.strokeJoin = Paint.Join.ROUND
        mCirclePaint.strokeCap = mCircleStyle

        mCircleFillPaint.isAntiAlias = true
        mCircleFillPaint.isDither = true
        mCircleFillPaint.color = mCircleFillColor
        mCircleFillPaint.style = Paint.Style.FILL

        mCircleProgressPaint.isAntiAlias = true
        mCircleProgressPaint.isDither = true
        mCircleProgressPaint.color = mCircleProgressColor
        mCircleProgressPaint.strokeWidth = mCircleStrokeWidth
        mCircleProgressPaint.style = Paint.Style.STROKE
        mCircleProgressPaint.strokeJoin = Paint.Join.ROUND
        mCircleProgressPaint.strokeCap = mCircleStyle
        if (!mDisableProgressGlow) {
            mCircleProgressGlowPaint.set(mCircleProgressPaint)
            mCircleProgressGlowPaint.maskFilter = BlurMaskFilter(
                PROGRESS_GLOW_RADIUS_DP * DPTOPX_SCALE,
                BlurMaskFilter.Blur.NORMAL
            )
        }

        mPointerPaint.isAntiAlias = true
        mPointerPaint.isDither = true
        mPointerPaint.color = mPointerColor
        mPointerPaint.strokeWidth = mPointerStrokeWidth
        mPointerPaint.style = Paint.Style.STROKE
        mPointerPaint.strokeJoin = Paint.Join.ROUND
        mPointerPaint.strokeCap = mCircleStyle

        mPointerHaloPaint.set(mPointerPaint)
        mPointerHaloPaint.color = mPointerHaloColor
        mPointerHaloPaint.alpha = mPointerAlpha
        mPointerHaloPaint.strokeWidth = mPointerStrokeWidth + mPointerHaloWidth * 2f

        mPointerHaloBorderPaint.set(mPointerPaint)
        mPointerHaloBorderPaint.strokeWidth = mPointerHaloBorderWidth
        mPointerHaloBorderPaint.style = Paint.Style.STROKE
    }

    /**
     * Calculates the total degrees between mStartAngle and mEndAngle, and sets mTotalCircleDegrees
     * to this value.
     */
    private fun calculateTotalDegrees() {
        mTotalCircleDegrees =
            (360f - (mStartAngle - mEndAngle)) % 360f // Length of the entire circle/arc
        if (mTotalCircleDegrees <= 0f) {
            mTotalCircleDegrees = 360f
        }
    }

    /**
     * Calculate the degrees that the progress represents. Also called the sweep angle.
     * Sets mProgressDegrees to that value.
     */
    private fun calculateProgressDegrees() {
        mProgressDegrees =
            if (mIsInNegativeHalf) mStartAngle - mPointerPosition else mPointerPosition - mStartAngle // Verified
        mProgressDegrees =
            if (mProgressDegrees < 0) 360f + mProgressDegrees else mProgressDegrees // Verified
    }

    /**
     * Calculate the pointer position (and the end of the progress arc) in degrees.
     * Sets mPointerPosition to that value.
     */
    private fun calculatePointerPosition() {
        val progressPercent = mProgress / mMax
        val progressDegree = progressPercent * mTotalCircleDegrees
        mPointerPosition = mStartAngle + if (mIsInNegativeHalf) -progressDegree else progressDegree
        mPointerPosition =
            (if (mPointerPosition < 0) 360f + mPointerPosition else mPointerPosition) % 360f
    }

    private fun calculatePointerXYPosition() {
        var pm = PathMeasure(mCircleProgressPath, false)
        val returnValue = pm.getPosTan(pm.length, mPointerPositionXY, null)
        if (!returnValue) {
            pm = PathMeasure(mCirclePath, false)
            pm.getPosTan(0f, mPointerPositionXY, null)
        }
    }

    /**
     * Reset the `Path` objects with the appropriate values.
     */
    private fun resetPaths() {
        if (mIsInNegativeHalf) {
            mCirclePath.reset()
            mCirclePath.addArc(pathCircle, mStartAngle - mTotalCircleDegrees, mTotalCircleDegrees)

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            val extendStart = mStartAngle - mProgressDegrees - mPointerAngle / 2.0f
            var extendDegrees = mProgressDegrees + mPointerAngle
            if (extendDegrees >= 360f) {
                extendDegrees = 360f - SMALL_DEGREE_BIAS
            }
            mCircleProgressPath.reset()
            mCircleProgressPath.addArc(pathCircle, extendStart, extendDegrees)
            val pointerStart = mPointerPosition - mPointerAngle / 2.0f
            mCirclePointerPath.reset()
            mCirclePointerPath.addArc(pathCircle, pointerStart, mPointerAngle)
        } else {
            mCirclePath.reset()
            mCirclePath.addArc(pathCircle, mStartAngle, mTotalCircleDegrees)

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            val extendStart = mStartAngle - mPointerAngle / 2.0f
            var extendDegrees = mProgressDegrees + mPointerAngle
            if (extendDegrees >= 360f) {
                extendDegrees = 360f - SMALL_DEGREE_BIAS
            }
            mCircleProgressPath.reset()
            mCircleProgressPath.addArc(pathCircle, extendStart, extendDegrees)
            val pointerStart = mPointerPosition - mPointerAngle / 2.0f
            mCirclePointerPath.reset()
            mCirclePointerPath.addArc(pathCircle, pointerStart, mPointerAngle)
        }
    }

    /**
     * Initialize the `RectF` objects with the appropriate values.
     */
    private fun resetRects() {
        pathCircle[-mCircleWidth, -mCircleHeight, mCircleWidth] = mCircleHeight
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(width / 2f, height / 2f)
        canvas.drawPath(mCirclePath, mCircleFillPaint)
        canvas.drawPath(mCirclePath, mCirclePaint)
        val ableToGoNegative =
            isNegativeEnabled && Math.abs(mTotalCircleDegrees - 360f) < SMALL_DEGREE_BIAS * 2
        // Hide progress bar when progress is 0
        // Also make sure we still draw progress when has pointer or able to go negative
        val shouldHideProgress = mHideProgressWhenEmpty && mProgressDegrees == 0f &&
                mDisablePointer && !ableToGoNegative
        if (!shouldHideProgress) {
            if (!mDisableProgressGlow) {
                canvas.drawPath(mCircleProgressPath, mCircleProgressGlowPaint)
            }
            canvas.drawPath(mCircleProgressPath, mCircleProgressPaint)
        }
        if (!mDisablePointer) {
            if (mUserIsMovingPointer) {
                canvas.drawPath(mCirclePointerPath, mPointerHaloPaint)
            }
            canvas.drawPath(mCirclePointerPath, mPointerPaint)
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
    /**
     * Set the progress of the CircularSeekBar.
     * If the progress is the same, then any listener will not receive a onProgressChanged event.
     * @param progress The progress to set the CircularSeekBar to.
     */
    var progress: Float
        get() {
            val progress = mMax * mProgressDegrees / mTotalCircleDegrees
            return if (mIsInNegativeHalf) -progress else progress
        }
        set(progress) {
            if (mProgress != progress) {
                if (isNegativeEnabled) {
                    if (progress < 0) {
                        mProgress = -progress
                        mIsInNegativeHalf = true
                    } else {
                        mProgress = progress
                        mIsInNegativeHalf = false
                    }
                } else {
                    mProgress = progress
                }

                mOnCircularSeekBarChangeListener?.onProgressChanged(this, progress, false)

                recalculateAll()
                invalidate()
            }
        }

    private fun setProgressBasedOnAngle(angle: Float) {
        mPointerPosition = angle
        calculateProgressDegrees()
        mProgress = mMax * mProgressDegrees / mTotalCircleDegrees
    }

    private fun recalculateAll() {
        calculateTotalDegrees()
        calculatePointerPosition()
        calculateProgressDegrees()
        resetRects()
        resetPaths()
        calculatePointerXYPosition()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        var width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        if (height == 0) height = width
        if (width == 0) width = height
        if (mMaintainEqualCircle) {
            val min = Math.min(width, height)
            setMeasuredDimension(min, min)
        } else {
            setMeasuredDimension(width, height)
        }
        val isHardwareAccelerated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                isHardwareAccelerated && layerType != LAYER_TYPE_SOFTWARE
        val hasGlowEffect = !mDisableProgressGlow && !isHardwareAccelerated

        // Set the circle width and height based on the view for the moment
        val padding = Math.max(
            mCircleStrokeWidth / 2f,
            mPointerStrokeWidth / 2 + mPointerHaloWidth + mPointerHaloBorderWidth
        ) +
                if (hasGlowEffect) PROGRESS_GLOW_RADIUS_DP * DPTOPX_SCALE else 0f
        mCircleHeight = height / 2f - padding
        mCircleWidth = width / 2f - padding

        // If it is not set to use custom
        if (mCustomRadii) {
            // Check to make sure the custom radii are not out of the view. If they are, just use the view values
            if (mCircleYRadius - padding < mCircleHeight) {
                mCircleHeight = mCircleYRadius - padding
            }
            if (mCircleXRadius - padding < mCircleWidth) {
                mCircleWidth = mCircleXRadius - padding
            }
        }
        if (mMaintainEqualCircle) { // Applies regardless of how the values were determined
            val min = Math.min(mCircleHeight, mCircleWidth)
            mCircleHeight = min
            mCircleWidth = min
        }
        recalculateAll()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mDisablePointer || !isEnabled) return false

        // Convert coordinates to our internal coordinate system
        val x = event.x - width / 2
        val y = event.y - height / 2

        // Get the distance from the center of the circle in terms of x and y
        val distanceX = pathCircle.centerX() - x
        val distanceY = pathCircle.centerY() - y

        // Get the distance from the center of the circle in terms of a radius
        val touchEventRadius =
            Math.sqrt(Math.pow(distanceX.toDouble(), 2.0) + Math.pow(distanceY.toDouble(), 2.0))
                .toFloat()
        val minimumTouchTarget =
            MIN_TOUCH_TARGET_DP * DPTOPX_SCALE // Convert minimum touch target into px
        var additionalRadius: Float // Either uses the minimumTouchTarget size or larger if the ring/pointer is larger
        additionalRadius =
            if (mCircleStrokeWidth < minimumTouchTarget) { // If the width is less than the minimumTouchTarget, use the minimumTouchTarget
                minimumTouchTarget / 2
            } else {
                mCircleStrokeWidth / 2 // Otherwise use the width
            }
        val outerRadius = Math.max(
            mCircleHeight,
            mCircleWidth
        ) + additionalRadius // Max outer radius of the circle, including the minimumTouchTarget or wheel width
        val innerRadius = Math.min(
            mCircleHeight,
            mCircleWidth
        ) - additionalRadius // Min inner radius of the circle, including the minimumTouchTarget or wheel width
        additionalRadius =
            if (mPointerStrokeWidth < minimumTouchTarget / 2) { // If the pointer radius is less than the minimumTouchTarget, use the minimumTouchTarget
                minimumTouchTarget / 2
            } else {
                mPointerStrokeWidth // Otherwise use the radius
            }
        var touchAngle: Float
        touchAngle =
            (Math.atan2(y.toDouble(), x.toDouble()) / Math.PI * 180 % 360).toFloat() // Verified
        touchAngle = if (touchAngle < 0) 360 + touchAngle else touchAngle // Verified

        /*
          Represents the clockwise distance from {@code mStartAngle} to the touch angle.
          Used when touching the CircularSeekBar.
         */
        var cwDistanceFromStart: Float

        /*
          Represents the counter-clockwise distance from {@code mStartAngle} to the touch angle.
          Used when touching the CircularSeekBar.
         */
        val ccwDistanceFromStart: Float

        /*
          Represents the clockwise distance from {@code mEndAngle} to the touch angle.
          Used when touching the CircularSeekBar.
         */
        var cwDistanceFromEnd: Float

        /*
          Represents the counter-clockwise distance from {@code mEndAngle} to the touch angle.
          Used when touching the CircularSeekBar.
          Currently unused, but kept just in case.
         */
        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
        val ccwDistanceFromEnd: Float

        /*
          Represents the clockwise distance from {@code mPointerPosition} to the touch angle.
          Used when touching the CircularSeekBar.
         */
        var cwDistanceFromPointer: Float

        /*
          Represents the counter-clockwise distance from {@code mPointerPosition} to the touch angle.
          Used when touching the CircularSeekBar.
         */
        val ccwDistanceFromPointer: Float
        cwDistanceFromStart = touchAngle - mStartAngle // Verified
        cwDistanceFromStart =
            if (cwDistanceFromStart < 0) 360f + cwDistanceFromStart else cwDistanceFromStart // Verified
        ccwDistanceFromStart = 360f - cwDistanceFromStart // Verified
        cwDistanceFromEnd = touchAngle - mEndAngle // Verified
        cwDistanceFromEnd =
            if (cwDistanceFromEnd < 0) 360f + cwDistanceFromEnd else cwDistanceFromEnd // Verified
        ccwDistanceFromEnd = 360f - cwDistanceFromEnd // Verified
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // These are only used for ACTION_DOWN for handling if the pointer was the part that was touched
                val pointerRadiusDegrees = (mPointerStrokeWidth * 180 / (Math.PI * Math.max(
                    mCircleHeight,
                    mCircleWidth
                ))).toFloat()
                val pointerDegrees = Math.max(pointerRadiusDegrees, mPointerAngle / 2f)
                cwDistanceFromPointer = touchAngle - mPointerPosition
                cwDistanceFromPointer =
                    if (cwDistanceFromPointer < 0) 360f + cwDistanceFromPointer else cwDistanceFromPointer
                ccwDistanceFromPointer = 360f - cwDistanceFromPointer
                // This is for if the first touch is on the actual pointer.
                if (touchEventRadius >= innerRadius && touchEventRadius <= outerRadius &&
                    (cwDistanceFromPointer <= pointerDegrees || ccwDistanceFromPointer <= pointerDegrees)
                ) {
                    setProgressBasedOnAngle(mPointerPosition)
                    mPointerHaloPaint.alpha = mPointerAlphaOnTouch
                    mPointerHaloPaint.color = mPointerHaloColorOnTouch
                    recalculateAll()
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    mUserIsMovingPointer = true
                    mLockAtEnd = false
                    mLockAtStart = false
                } else if (cwDistanceFromStart > mTotalCircleDegrees) { // If the user is touching outside of the start AND end
                    mUserIsMovingPointer = false
                    return false
                } else if (touchEventRadius >= innerRadius && touchEventRadius <= outerRadius) { // If the user is touching near the circle
                    setProgressBasedOnAngle(touchAngle)
                    mPointerHaloPaint.alpha = mPointerAlphaOnTouch
                    mPointerHaloPaint.color = mPointerHaloColorOnTouch
                    recalculateAll()
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    mOnCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    mUserIsMovingPointer = true
                    mLockAtEnd = false
                    mLockAtStart = false
                } else { // If the user is not touching near the circle
                    mUserIsMovingPointer = false
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> if (mUserIsMovingPointer) {
                val smallInCircle = mTotalCircleDegrees / 3f
                var cwPointerFromStart = mPointerPosition - mStartAngle
                cwPointerFromStart =
                    if (cwPointerFromStart < 0) cwPointerFromStart + 360f else cwPointerFromStart
                val touchOverStart = ccwDistanceFromStart < smallInCircle
                val touchOverEnd = cwDistanceFromEnd < smallInCircle
                val pointerNearStart = cwPointerFromStart < smallInCircle
                val pointerNearEnd = cwPointerFromStart > mTotalCircleDegrees - smallInCircle
                val progressNearZero = mProgress < mMax / 3f
                val progressNearMax = mProgress > mMax / 3f * 2f
                if (progressNearMax) {  // logic for end lock.
                    if (pointerNearStart) { // negative end
                        mLockAtEnd = touchOverStart
                    } else if (pointerNearEnd) {    // positive end
                        mLockAtEnd = touchOverEnd
                    }
                } else if (progressNearZero && isNegativeEnabled) {   // logic for negative flip
                    if (touchOverEnd) mIsInNegativeHalf = false else if (touchOverStart) {
                        mIsInNegativeHalf = true
                    }
                } else if (progressNearZero) {  // logic for start lock
                    if (pointerNearStart) {
                        mLockAtStart = touchOverStart
                    }
                }
                if (mLockAtStart && isLockEnabled) {
                    // TODO: Add a check if mProgress is already 0, in which case don't call the listener
                    mProgress = 0f
                    recalculateAll()
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                } else if (mLockAtEnd && isLockEnabled) {
                    mProgress = mMax
                    recalculateAll()
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                } else if (mMoveOutsideCircle || touchEventRadius <= outerRadius) {
                    if (cwDistanceFromStart <= mTotalCircleDegrees) {
                        setProgressBasedOnAngle(touchAngle)
                    }
                    recalculateAll()
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                }
            } else {
                return false
            }
            MotionEvent.ACTION_UP -> {
                mPointerHaloPaint.alpha = mPointerAlpha
                mPointerHaloPaint.color = mPointerHaloColor
                if (mUserIsMovingPointer) {
                    mUserIsMovingPointer = false
                    invalidate()
                    mOnCircularSeekBarChangeListener?.onStopTrackingTouch(this)
                } else {
                    return false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mPointerHaloPaint.alpha = mPointerAlpha
                mPointerHaloPaint.color = mPointerHaloColor
                mUserIsMovingPointer = false
                invalidate()
            }
        }
        if (event.action == MotionEvent.ACTION_MOVE && parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return true
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = Bundle()
        state.putParcelable("PARENT", superState)
        state.putFloat("MAX", mMax)
        state.putFloat("PROGRESS", mProgress)
        state.putInt("mCircleColor", mCircleColor)
        state.putInt("mCircleProgressColor", mCircleProgressColor)
        state.putInt("mPointerColor", mPointerColor)
        state.putInt("mPointerHaloColor", mPointerHaloColor)
        state.putInt("mPointerHaloColorOnTouch", mPointerHaloColorOnTouch)
        state.putInt("mPointerAlpha", mPointerAlpha)
        state.putInt("mPointerAlphaOnTouch", mPointerAlphaOnTouch)
        state.putFloat("mPointerAngle", mPointerAngle)
        state.putBoolean("mDisablePointer", mDisablePointer)
        state.putBoolean("mLockEnabled", isLockEnabled)
        state.putBoolean("mNegativeEnabled", isNegativeEnabled)
        state.putBoolean("mDisableProgressGlow", mDisableProgressGlow)
        state.putBoolean("mIsInNegativeHalf", mIsInNegativeHalf)
        state.putInt("mCircleStyle", mCircleStyle.ordinal)
        state.putBoolean("mHideProgressWhenEmpty", mHideProgressWhenEmpty)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as Bundle
        val superState = savedState.getParcelable<Parcelable>("PARENT")
        super.onRestoreInstanceState(superState)
        mMax = savedState.getFloat("MAX")
        mProgress = savedState.getFloat("PROGRESS")
        mCircleColor = savedState.getInt("mCircleColor")
        mCircleProgressColor = savedState.getInt("mCircleProgressColor")
        mPointerColor = savedState.getInt("mPointerColor")
        mPointerHaloColor = savedState.getInt("mPointerHaloColor")
        mPointerHaloColorOnTouch = savedState.getInt("mPointerHaloColorOnTouch")
        mPointerAlpha = savedState.getInt("mPointerAlpha")
        mPointerAlphaOnTouch = savedState.getInt("mPointerAlphaOnTouch")
        mPointerAngle = savedState.getFloat("mPointerAngle")
        mDisablePointer = savedState.getBoolean("mDisablePointer")
        isLockEnabled = savedState.getBoolean("mLockEnabled")
        isNegativeEnabled = savedState.getBoolean("mNegativeEnabled")
        mDisableProgressGlow = savedState.getBoolean("mDisableProgressGlow")
        mIsInNegativeHalf = savedState.getBoolean("mIsInNegativeHalf")
        mCircleStyle = Cap.values()[savedState.getInt("mCircleStyle")]
        mHideProgressWhenEmpty = savedState.getBoolean("mHideProgressWhenEmpty")
        initPaints()
        recalculateAll()
    }

    fun setOnSeekBarChangeListener(l: OnCircularSeekBarChangeListener?) {
        mOnCircularSeekBarChangeListener = l
    }

    /**
     * Listener for the CircularSeekBar. Implements the same methods as the normal OnSeekBarChangeListener.
     */
    interface OnCircularSeekBarChangeListener {
        fun onProgressChanged(
            circularSeekBar: CircularSeekBarV2?,
            progress: Float,
            fromUser: Boolean
        )

        fun onStopTrackingTouch(seekBar: CircularSeekBarV2?)
        fun onStartTrackingTouch(seekBar: CircularSeekBarV2?)
    }

    companion object {
        /**
         * Minimum touch target size in DP. 48dp is the Android design recommendation
         */
        private const val MIN_TOUCH_TARGET_DP = 48f

        /**
         * For some case we need the degree to have small bias to avoid overflow.
         */
        private const val SMALL_DEGREE_BIAS = .1f

        /**
         * Radius of progress glow, in dp unit.
         */
        private const val PROGRESS_GLOW_RADIUS_DP = 5f

        // Default values
        private val DEFAULT_CIRCLE_STYLE = Cap.ROUND.ordinal
        private const val DEFAULT_CIRCLE_X_RADIUS = 30f
        private const val DEFAULT_CIRCLE_Y_RADIUS = 30f
        private const val DEFAULT_POINTER_STROKE_WIDTH = 14f
        private const val DEFAULT_POINTER_HALO_WIDTH = 6f
        private const val DEFAULT_POINTER_HALO_BORDER_WIDTH = 0f
        private const val DEFAULT_CIRCLE_STROKE_WIDTH = 5f
        private const val DEFAULT_START_ANGLE = 270f // Geometric (clockwise, relative to 3 o'clock)
        private const val DEFAULT_END_ANGLE = 270f // Geometric (clockwise, relative to 3 o'clock)
        private const val DEFAULT_POINTER_ANGLE = 0f
        private const val DEFAULT_MAX = 100
        private const val DEFAULT_PROGRESS = 0
        private const val DEFAULT_CIRCLE_COLOR = Color.DKGRAY
        private val DEFAULT_CIRCLE_PROGRESS_COLOR = Color.argb(235, 74, 138, 255)
        private val DEFAULT_POINTER_COLOR = Color.argb(235, 74, 138, 255)
        private val DEFAULT_POINTER_HALO_COLOR = Color.argb(135, 74, 138, 255)
        private val DEFAULT_POINTER_HALO_COLOR_ONTOUCH = Color.argb(135, 74, 138, 255)
        private const val DEFAULT_CIRCLE_FILL_COLOR = Color.TRANSPARENT
        private const val DEFAULT_POINTER_ALPHA = 135
        private const val DEFAULT_POINTER_ALPHA_ONTOUCH = 100
        private const val DEFAULT_USE_CUSTOM_RADII = false
        private const val DEFAULT_MAINTAIN_EQUAL_CIRCLE = true
        private const val DEFAULT_MOVE_OUTSIDE_CIRCLE = false
        private const val DEFAULT_LOCK_ENABLED = true
        private const val DEFAULT_DISABLE_POINTER = false
        private const val DEFAULT_NEGATIVE_ENABLED = false
        private const val DEFAULT_DISABLE_PROGRESS_GLOW = true
        private const val DEFAULT_CS_HIDE_PROGRESS_WHEN_EMPTY = false
    }
}