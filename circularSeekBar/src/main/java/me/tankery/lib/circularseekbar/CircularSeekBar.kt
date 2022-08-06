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

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Paint.Cap
import android.graphics.RectF
import android.graphics.Color
import android.graphics.BlurMaskFilter
import android.graphics.PathMeasure
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CircularSeekBar @JvmOverloads constructor(
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
    private val circlePaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the circle fill.
     */
    private val circleFillPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the active circle (represents progress).
     */
    private val circleProgressPaint: Paint = Paint()

    /**
     * If progress glow is disabled, there is no glow from the progress bar when filled
     *
     * NOTE: To enable glow effect, please make sure this view is rendering with hardware
     * accelerate disabled. (Checkout this doc for details of hardware accelerate:
     * https://developer.android.com/guide/topics/graphics/hardware-accel)
     */
    private var disableProgressGlow = false

    /**
     * `Paint` instance used to draw the glow from the active circle.
     */
    private val circleProgressGlowPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the center of the pointer.
     * Note: This is broken on 4.0+, as BlurMasks do not work with hardware acceleration.
     */
    private val pointerPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the halo of the pointer.
     * Note: The halo is the part that changes transparency.
     */
    private val pointerHaloPaint: Paint = Paint()

    /**
     * `Paint` instance used to draw the border of the pointer, outside of the halo.
     */
    private val pointerHaloBorderPaint: Paint = Paint()

    /**
     * The style of the circle, can be butt, round or square.
     */
    var circleStyle: Cap = Cap.ROUND
        set(style) {
            field = style
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * current in negative half cycle.
     */
    private var isInNegativeHalf = false

    /**
     * The width of the circle (in pixels).
     * Sets the circle stroke width.
     * @param width the width of the circle
     */
    var circleStrokeWidth = 0f
        set(width) {
            field = width
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * The X radius of the circle (in pixels).
     */
    private var circleXRadius = 0f

    /**
     * The Y radius of the circle (in pixels).
     */
    private var circleYRadius = 0f

    /**
     * If disable pointer, we can't seek the progress.
     */
    private var disablePointer = false

    /**
     * The radius of the pointer (in pixels).
     *
     * Sets the pointer pointer stroke width.
     * @param width the width of the pointer
     */
    var pointerStrokeWidth = 0f
        set(width) {
            field = width
            initPaints()
            recalculateAll()
            invalidate()
        }

    /**
     * The width of the pointer halo (in pixels).
     */
    private var pointerHaloWidth = 0f

    /**
     * The width of the pointer halo border (in pixels).
     */
    private var pointerHaloBorderWidth = 0f

    /**
     * Angle of the pointer arc.
     * Default is 0, the pointer is a circle when angle is 0 and the style is round.
     * Can not be less than 0. can not be greater than 360.
     *
     * Gets the pointer angle.
     * @return Angle for the pointer (0..360)
     */
    var pointerAngle = 0f
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
    var startAngle = 0f
        set(angle) {
            field = angle
            if (angle % 360f == endAngle % 360f) {
                //mStartAngle = mStartAngle + 1f;
                endAngle -= SMALL_DEGREE_BIAS
            }
            recalculateAll()
            invalidate()
        }

    /**
     * End angle of the CircularSeekBar.
     * Note: If mStartAngle and mEndAngle are set to the same angle, 0.1 is subtracted
     * from the mEndAngle to make the circle function properly.
     */
    var endAngle = 0f
        set(angle) {
            field = if (startAngle % 360f == endAngle % 360f) {
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
    var pointerColor = DEFAULT_POINTER_COLOR
        set(color) {
            field = color
            pointerPaint.color = color
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
    var pointerHaloColor = DEFAULT_POINTER_HALO_COLOR
        set(color) {
            field = color
            pointerHaloPaint.color = field
            invalidate()
        }

    /**
     * Holds the color value for `mPointerHaloPaint` before the `Paint` instance is created.
     */
    private var pointerHaloColorOnTouch = DEFAULT_POINTER_HALO_COLOR_ONTOUCH

    /**
     * Holds the color value for `mCirclePaint` before the `Paint` instance is created.
     *
     * Gets the circle color.
     *
     * Sets the circle color.
     * @param color the color of the circle
     * @return An integer color value for the circle
     */
    var circleColor = DEFAULT_CIRCLE_COLOR
        set(color) {
            field = color
            circlePaint.color = color
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
    var circleFillColor = DEFAULT_CIRCLE_FILL_COLOR
        set(color) {
            field = color
            circleFillPaint.color = color
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
    var circleProgressColor = DEFAULT_CIRCLE_PROGRESS_COLOR
        set(color) {
            field = color
            circleProgressPaint.color = color
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
    var pointerAlpha = DEFAULT_POINTER_ALPHA
        set(alpha) {
            if (alpha in 0..255) {
                field = alpha
                pointerHaloPaint.alpha = alpha
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
    var pointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH
        set(alpha) {
            if (alpha in 0..255) {
                field = alpha
            }
        }

    /**
     * Distance (in degrees) that the the circle/semi-circle makes up.
     * This amount represents the max of the circle in degrees.
     */
    private var totalCircleDegrees = 0f

    /**
     * Distance (in degrees) that the current progress makes up in the circle.
     */
    private var progressDegrees = 0f

    /**
     * `Path` used to draw the circle/semi-circle.
     */
    private val circlePath: Path = Path()

    /**
     * `Path` used to draw the progress on the circle.
     */
    private val circleProgressPath: Path = Path()

    /**
     * `Path` used to draw the pointer arc on the circle.
     */
    private val circlePointerPath: Path = Path()

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
    var max = 0f
        set(max) {
            if (max > 0) {
                if (max <= progressActual) {
                    progressActual = 0f // If the new max is less than current progress, set progress to zero
                    onCircularSeekBarChangeListener?.onProgressChanged(
                        this,
                        if (isInNegativeHalf) -progressActual else progressActual,
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
    private var progressActual = 0f

    /**
     * Get the progress of the CircularSeekBar.
     *
     * Set the progress of the CircularSeekBar.
     * If the progress is the same, then any listener will not receive a onProgressChanged event.
     *
     * @param progress The progress to set the CircularSeekBar to.
     * @return The progress of the CircularSeekBar.
     */
    var progress: Float
        get() {
            val progress = max * progressDegrees / totalCircleDegrees
            return if (isInNegativeHalf) -progress else progress
        }
        set(progress) {
            if (progressActual != progress) {
                if (isNegativeEnabled) {
                    if (progress < 0) {
                        progressActual = -progress
                        isInNegativeHalf = true
                    } else {
                        progressActual = progress
                        isInNegativeHalf = false
                    }
                } else {
                    progressActual = progress
                }

                onCircularSeekBarChangeListener?.onProgressChanged(this, progress, false)

                recalculateAll()
                invalidate()
            }
        }

    /**
     * Used for enabling/disabling the negative progress bar.
     */
    var isNegativeEnabled = false

    /**
     * If true, then the user can specify the X and Y radii.
     * If false, then the View itself determines the size of the CircularSeekBar.
     */
    private var customRadii = false

    /**
     * Maintain a perfect circle (equal x and y radius), regardless of view or custom attributes.
     * The smaller of the two radii will always be used in this case.
     * The default is to be a circle and not an ellipse, due to the behavior of the ellipse.
     */
    private var maintainEqualCircle = false

    /**
     * Once a user has touched the circle, this determines if moving outside the circle is able
     * to change the position of the pointer (and in turn, the progress).
     */
    private var moveOutsideCircle = false

    /**
     * Used for enabling/disabling the lock option for easier hitting of the 0 progress mark.
     */
    var isLockEnabled = true

    /**
     * Used for when the user moves beyond the start of the circle when moving counter clockwise.
     * Makes it easier to hit the 0 progress mark.
     */
    private var lockAtStart = true

    /**
     * Used for when the user moves beyond the end of the circle when moving clockwise.
     * Makes it easier to hit the 100% (max) progress mark.
     */
    private var lockAtEnd = false

    /**
     * If progress is zero, hide the progress bar.
     */
    private var hideProgressWhenEmpty = false

    /**
     * When the user is touching the circle on ACTION_DOWN, this is set to true.
     * Used when touching the CircularSeekBar.
     */
    private var userIsMovingPointer = false

    /**
     * The width of the circle used in the `RectF` that is used to draw it.
     * Based on either the View width or the custom X radius.
     */
    private var circleWidth = 0f

    /**
     * The height of the circle used in the `RectF` that is used to draw it.
     * Based on either the View width or the custom Y radius.
     */
    private var circleHeight = 0f

    /**
     * Represents the progress mark on the circle, in geometric degrees.
     * This is not provided by the user; it is calculated;
     */
    private var pointerPosition = 0f

    /**
     * Pointer position in terms of X and Y coordinates.
     */
    private val pointerPositionXY = FloatArray(2)

    /**
     * Listener.
     */
    private var onCircularSeekBarChangeListener: OnCircularSeekBarChangeListener? = null

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
        circleXRadius = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_x_radius,
            DEFAULT_CIRCLE_X_RADIUS
        )
        circleYRadius = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_y_radius,
            DEFAULT_CIRCLE_Y_RADIUS
        )
        pointerStrokeWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_stroke_width,
            DEFAULT_POINTER_STROKE_WIDTH
        )
        pointerHaloWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_width,
            DEFAULT_POINTER_HALO_WIDTH
        )
        pointerHaloBorderWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_border_width,
            DEFAULT_POINTER_HALO_BORDER_WIDTH
        )
        circleStrokeWidth = attrArray.getDimension(
            R.styleable.cs_CircularSeekBar_cs_circle_stroke_width,
            DEFAULT_CIRCLE_STROKE_WIDTH
        )
        val circleStyleAttribute =
            attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_circle_style, DEFAULT_CIRCLE_STYLE)
        circleStyle = Cap.values()[circleStyleAttribute]
        pointerColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_color,
            DEFAULT_POINTER_COLOR
        )
        pointerHaloColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_color,
            DEFAULT_POINTER_HALO_COLOR
        )
        pointerHaloColorOnTouch = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_pointer_halo_color_ontouch,
            DEFAULT_POINTER_HALO_COLOR_ONTOUCH
        )
        circleColor =
            attrArray.getColor(R.styleable.cs_CircularSeekBar_cs_circle_color, DEFAULT_CIRCLE_COLOR)
        circleProgressColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_circle_progress_color,
            DEFAULT_CIRCLE_PROGRESS_COLOR
        )
        circleFillColor = attrArray.getColor(
            R.styleable.cs_CircularSeekBar_cs_circle_fill,
            DEFAULT_CIRCLE_FILL_COLOR
        )
        pointerAlpha = Color.alpha(pointerHaloColor)
        pointerAlphaOnTouch = attrArray.getInt(
            R.styleable.cs_CircularSeekBar_cs_pointer_alpha_ontouch,
            DEFAULT_POINTER_ALPHA_ONTOUCH
        )
        if (pointerAlphaOnTouch > 255 || pointerAlphaOnTouch < 0) {
            pointerAlphaOnTouch = DEFAULT_POINTER_ALPHA_ONTOUCH
        }
        max = attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_max, DEFAULT_MAX).toFloat()
        progressActual =
            attrArray.getInt(R.styleable.cs_CircularSeekBar_cs_progress, DEFAULT_PROGRESS).toFloat()
        customRadii = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_use_custom_radii,
            DEFAULT_USE_CUSTOM_RADII
        )
        maintainEqualCircle = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_maintain_equal_circle,
            DEFAULT_MAINTAIN_EQUAL_CIRCLE
        )
        moveOutsideCircle = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_move_outside_circle,
            DEFAULT_MOVE_OUTSIDE_CIRCLE
        )
        isLockEnabled = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_lock_enabled,
            DEFAULT_LOCK_ENABLED
        )
        disablePointer = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_disable_pointer,
            DEFAULT_DISABLE_POINTER
        )
        isNegativeEnabled = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_negative_enabled,
            DEFAULT_NEGATIVE_ENABLED
        )
        isInNegativeHalf = false
        disableProgressGlow = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_disable_progress_glow,
            DEFAULT_DISABLE_PROGRESS_GLOW
        )
        hideProgressWhenEmpty = attrArray.getBoolean(
            R.styleable.cs_CircularSeekBar_cs_hide_progress_when_empty,
            DEFAULT_CS_HIDE_PROGRESS_WHEN_EMPTY
        )

        // Modulo 360 right now to avoid constant conversion
        startAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_start_angle,
            DEFAULT_START_ANGLE
        ) % 360f) % 360f
        endAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_end_angle,
            DEFAULT_END_ANGLE
        ) % 360f) % 360f

        // Disable negative progress if is semi-oval.
        if (startAngle != endAngle) {
            isNegativeEnabled = false
        }
        if (startAngle % 360f == endAngle % 360f) {
            //mStartAngle = mStartAngle + 1f;
            endAngle = endAngle - SMALL_DEGREE_BIAS
        }

        // Modulo 360 right now to avoid constant conversion
        pointerAngle = (360f + attrArray.getFloat(
            R.styleable.cs_CircularSeekBar_cs_pointer_angle,
            DEFAULT_POINTER_ANGLE
        ) % 360f) % 360f
        if (pointerAngle == 0f) {
            pointerAngle = SMALL_DEGREE_BIAS
        }
        if (disablePointer) {
            pointerStrokeWidth = 0f
            pointerHaloWidth = 0f
            pointerHaloBorderWidth = 0f
        }
    }

    /**
     * Initializes the `Paint` objects with the appropriate styles.
     */
    private fun initPaints() {
        circlePaint.isAntiAlias = true
        circlePaint.isDither = true
        circlePaint.color = circleColor
        circlePaint.strokeWidth = circleStrokeWidth
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeJoin = Paint.Join.ROUND
        circlePaint.strokeCap = circleStyle

        circleFillPaint.isAntiAlias = true
        circleFillPaint.isDither = true
        circleFillPaint.color = circleFillColor
        circleFillPaint.style = Paint.Style.FILL

        circleProgressPaint.isAntiAlias = true
        circleProgressPaint.isDither = true
        circleProgressPaint.color = circleProgressColor
        circleProgressPaint.strokeWidth = circleStrokeWidth
        circleProgressPaint.style = Paint.Style.STROKE
        circleProgressPaint.strokeJoin = Paint.Join.ROUND
        circleProgressPaint.strokeCap = circleStyle
        if (!disableProgressGlow) {
            circleProgressGlowPaint.set(circleProgressPaint)
            circleProgressGlowPaint.maskFilter = BlurMaskFilter(
                PROGRESS_GLOW_RADIUS_DP * DPTOPX_SCALE,
                BlurMaskFilter.Blur.NORMAL
            )
        }

        pointerPaint.isAntiAlias = true
        pointerPaint.isDither = true
        pointerPaint.color = pointerColor
        pointerPaint.strokeWidth = pointerStrokeWidth
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeJoin = Paint.Join.ROUND
        pointerPaint.strokeCap = circleStyle

        pointerHaloPaint.set(pointerPaint)
        pointerHaloPaint.color = pointerHaloColor
        pointerHaloPaint.alpha = pointerAlpha
        pointerHaloPaint.strokeWidth = pointerStrokeWidth + pointerHaloWidth * 2f

        pointerHaloBorderPaint.set(pointerPaint)
        pointerHaloBorderPaint.strokeWidth = pointerHaloBorderWidth
        pointerHaloBorderPaint.style = Paint.Style.STROKE
    }

    /**
     * Calculates the total degrees between mStartAngle and mEndAngle, and sets mTotalCircleDegrees
     * to this value.
     */
    private fun calculateTotalDegrees() {
        totalCircleDegrees =
            (360f - (startAngle - endAngle)) % 360f // Length of the entire circle/arc
        if (totalCircleDegrees <= 0f) {
            totalCircleDegrees = 360f
        }
    }

    /**
     * Calculate the degrees that the progress represents. Also called the sweep angle.
     * Sets mProgressDegrees to that value.
     */
    private fun calculateProgressDegrees() {
        progressDegrees =
            if (isInNegativeHalf) startAngle - pointerPosition else pointerPosition - startAngle // Verified
        progressDegrees =
            if (progressDegrees < 0) 360f + progressDegrees else progressDegrees // Verified
    }

    /**
     * Calculate the pointer position (and the end of the progress arc) in degrees.
     * Sets mPointerPosition to that value.
     */
    private fun calculatePointerPosition() {
        val progressPercent = progressActual / max
        val progressDegree = progressPercent * totalCircleDegrees
        pointerPosition = startAngle + if (isInNegativeHalf) -progressDegree else progressDegree
        pointerPosition =
            (if (pointerPosition < 0) 360f + pointerPosition else pointerPosition) % 360f
    }

    private fun calculatePointerXYPosition() {
        var pm = PathMeasure(circleProgressPath, false)
        val returnValue = pm.getPosTan(pm.length, pointerPositionXY, null)
        if (!returnValue) {
            pm = PathMeasure(circlePath, false)
            pm.getPosTan(0f, pointerPositionXY, null)
        }
    }

    /**
     * Reset the `Path` objects with the appropriate values.
     */
    private fun resetPaths() {
        if (isInNegativeHalf) {
            circlePath.reset()
            circlePath.addArc(pathCircle, startAngle - totalCircleDegrees, totalCircleDegrees)

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            val extendStart = startAngle - progressDegrees - pointerAngle / 2.0f
            var extendDegrees = progressDegrees + pointerAngle
            if (extendDegrees >= 360f) {
                extendDegrees = 360f - SMALL_DEGREE_BIAS
            }
            circleProgressPath.reset()
            circleProgressPath.addArc(pathCircle, extendStart, extendDegrees)
            val pointerStart = pointerPosition - pointerAngle / 2.0f
            circlePointerPath.reset()
            circlePointerPath.addArc(pathCircle, pointerStart, pointerAngle)
        } else {
            circlePath.reset()
            circlePath.addArc(pathCircle, startAngle, totalCircleDegrees)

            // beside progress path it self, we also draw a extend arc to math the pointer arc.
            val extendStart = startAngle - pointerAngle / 2.0f
            var extendDegrees = progressDegrees + pointerAngle
            if (extendDegrees >= 360f) {
                extendDegrees = 360f - SMALL_DEGREE_BIAS
            }
            circleProgressPath.reset()
            circleProgressPath.addArc(pathCircle, extendStart, extendDegrees)
            val pointerStart = pointerPosition - pointerAngle / 2.0f
            circlePointerPath.reset()
            circlePointerPath.addArc(pathCircle, pointerStart, pointerAngle)
        }
    }

    /**
     * Initialize the `RectF` objects with the appropriate values.
     */
    private fun resetRects() {
        pathCircle[-circleWidth, -circleHeight, circleWidth] = circleHeight
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(width / 2f, height / 2f)
        canvas.drawPath(circlePath, circleFillPaint)
        canvas.drawPath(circlePath, circlePaint)
        val ableToGoNegative =
            isNegativeEnabled && Math.abs(totalCircleDegrees - 360f) < SMALL_DEGREE_BIAS * 2
        // Hide progress bar when progress is 0
        // Also make sure we still draw progress when has pointer or able to go negative
        val shouldHideProgress = hideProgressWhenEmpty && progressDegrees == 0f &&
                disablePointer && !ableToGoNegative
        if (!shouldHideProgress) {
            if (!disableProgressGlow) {
                canvas.drawPath(circleProgressPath, circleProgressGlowPaint)
            }
            canvas.drawPath(circleProgressPath, circleProgressPaint)
        }
        if (!disablePointer) {
            if (userIsMovingPointer) {
                canvas.drawPath(circlePointerPath, pointerHaloPaint)
            }
            canvas.drawPath(circlePointerPath, pointerPaint)
            // TODO, find a good way to draw halo border.
//            if (mUserIsMovingPointer) {
//                canvas.drawCircle(mPointerPositionXY[0], mPointerPositionXY[1],
//                        (mPointerStrokeWidth /2f) + mPointerHaloWidth + (mPointerHaloBorderWidth / 2f),
//                        mPointerHaloBorderPaint);
//            }
        }
    }

    private fun setProgressBasedOnAngle(angle: Float) {
        pointerPosition = angle
        calculateProgressDegrees()
        progressActual = max * progressDegrees / totalCircleDegrees
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
        if (maintainEqualCircle) {
            val min = Math.min(width, height)
            setMeasuredDimension(min, min)
        } else {
            setMeasuredDimension(width, height)
        }
        val isHardwareAccelerated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                isHardwareAccelerated && layerType != LAYER_TYPE_SOFTWARE
        val hasGlowEffect = !disableProgressGlow && !isHardwareAccelerated

        // Set the circle width and height based on the view for the moment
        val padding = Math.max(
            circleStrokeWidth / 2f,
            pointerStrokeWidth / 2 + pointerHaloWidth + pointerHaloBorderWidth
        ) +
                if (hasGlowEffect) PROGRESS_GLOW_RADIUS_DP * DPTOPX_SCALE else 0f
        circleHeight = height / 2f - padding
        circleWidth = width / 2f - padding

        // If it is not set to use custom
        if (customRadii) {
            // Check to make sure the custom radii are not out of the view. If they are, just use the view values
            if (circleYRadius - padding < circleHeight) {
                circleHeight = circleYRadius - padding
            }
            if (circleXRadius - padding < circleWidth) {
                circleWidth = circleXRadius - padding
            }
        }
        if (maintainEqualCircle) { // Applies regardless of how the values were determined
            val min = Math.min(circleHeight, circleWidth)
            circleHeight = min
            circleWidth = min
        }
        recalculateAll()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (disablePointer || !isEnabled) return false

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
            if (circleStrokeWidth < minimumTouchTarget) { // If the width is less than the minimumTouchTarget, use the minimumTouchTarget
                minimumTouchTarget / 2
            } else {
                circleStrokeWidth / 2 // Otherwise use the width
            }
        val outerRadius = Math.max(
            circleHeight,
            circleWidth
        ) + additionalRadius // Max outer radius of the circle, including the minimumTouchTarget or wheel width
        val innerRadius = Math.min(
            circleHeight,
            circleWidth
        ) - additionalRadius // Min inner radius of the circle, including the minimumTouchTarget or wheel width
        additionalRadius =
            if (pointerStrokeWidth < minimumTouchTarget / 2) { // If the pointer radius is less than the minimumTouchTarget, use the minimumTouchTarget
                minimumTouchTarget / 2
            } else {
                pointerStrokeWidth // Otherwise use the radius
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
        cwDistanceFromStart = touchAngle - startAngle // Verified
        cwDistanceFromStart =
            if (cwDistanceFromStart < 0) 360f + cwDistanceFromStart else cwDistanceFromStart // Verified
        ccwDistanceFromStart = 360f - cwDistanceFromStart // Verified
        cwDistanceFromEnd = touchAngle - endAngle // Verified
        cwDistanceFromEnd =
            if (cwDistanceFromEnd < 0) 360f + cwDistanceFromEnd else cwDistanceFromEnd // Verified
        ccwDistanceFromEnd = 360f - cwDistanceFromEnd // Verified
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // These are only used for ACTION_DOWN for handling if the pointer was the part that was touched
                val pointerRadiusDegrees = (pointerStrokeWidth * 180 / (Math.PI * Math.max(
                    circleHeight,
                    circleWidth
                ))).toFloat()
                val pointerDegrees = Math.max(pointerRadiusDegrees, pointerAngle / 2f)
                cwDistanceFromPointer = touchAngle - pointerPosition
                cwDistanceFromPointer =
                    if (cwDistanceFromPointer < 0) 360f + cwDistanceFromPointer else cwDistanceFromPointer
                ccwDistanceFromPointer = 360f - cwDistanceFromPointer
                // This is for if the first touch is on the actual pointer.
                if (touchEventRadius >= innerRadius && touchEventRadius <= outerRadius &&
                    (cwDistanceFromPointer <= pointerDegrees || ccwDistanceFromPointer <= pointerDegrees)
                ) {
                    setProgressBasedOnAngle(pointerPosition)
                    pointerHaloPaint.alpha = pointerAlphaOnTouch
                    pointerHaloPaint.color = pointerHaloColorOnTouch
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    userIsMovingPointer = true
                    lockAtEnd = false
                    lockAtStart = false
                } else if (cwDistanceFromStart > totalCircleDegrees) { // If the user is touching outside of the start AND end
                    userIsMovingPointer = false
                    return false
                } else if (touchEventRadius >= innerRadius && touchEventRadius <= outerRadius) { // If the user is touching near the circle
                    setProgressBasedOnAngle(touchAngle)
                    pointerHaloPaint.alpha = pointerAlphaOnTouch
                    pointerHaloPaint.color = pointerHaloColorOnTouch
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onStartTrackingTouch(this)
                    onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                    userIsMovingPointer = true
                    lockAtEnd = false
                    lockAtStart = false
                } else { // If the user is not touching near the circle
                    userIsMovingPointer = false
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> if (userIsMovingPointer) {
                val smallInCircle = totalCircleDegrees / 3f
                var cwPointerFromStart = pointerPosition - startAngle
                cwPointerFromStart =
                    if (cwPointerFromStart < 0) cwPointerFromStart + 360f else cwPointerFromStart
                val touchOverStart = ccwDistanceFromStart < smallInCircle
                val touchOverEnd = cwDistanceFromEnd < smallInCircle
                val pointerNearStart = cwPointerFromStart < smallInCircle
                val pointerNearEnd = cwPointerFromStart > totalCircleDegrees - smallInCircle
                val progressNearZero = progressActual < max / 3f
                val progressNearMax = progressActual > max / 3f * 2f
                if (progressNearMax) {  // logic for end lock.
                    if (pointerNearStart) { // negative end
                        lockAtEnd = touchOverStart
                    } else if (pointerNearEnd) {    // positive end
                        lockAtEnd = touchOverEnd
                    }
                } else if (progressNearZero && isNegativeEnabled) {   // logic for negative flip
                    if (touchOverEnd) isInNegativeHalf = false else if (touchOverStart) {
                        isInNegativeHalf = true
                    }
                } else if (progressNearZero) {  // logic for start lock
                    if (pointerNearStart) {
                        lockAtStart = touchOverStart
                    }
                }
                if (lockAtStart && isLockEnabled) {
                    // TODO: Add a check if mProgress is already 0, in which case don't call the listener
                    progressActual = 0f
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                } else if (lockAtEnd && isLockEnabled) {
                    progressActual = max
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                } else if (moveOutsideCircle || touchEventRadius <= outerRadius) {
                    if (cwDistanceFromStart <= totalCircleDegrees) {
                        setProgressBasedOnAngle(touchAngle)
                    }
                    recalculateAll()
                    invalidate()
                    onCircularSeekBarChangeListener?.onProgressChanged(this, progress, true)
                }
            } else {
                return false
            }
            MotionEvent.ACTION_UP -> {
                pointerHaloPaint.alpha = pointerAlpha
                pointerHaloPaint.color = pointerHaloColor
                if (userIsMovingPointer) {
                    userIsMovingPointer = false
                    invalidate()
                    onCircularSeekBarChangeListener?.onStopTrackingTouch(this)
                } else {
                    return false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pointerHaloPaint.alpha = pointerAlpha
                pointerHaloPaint.color = pointerHaloColor
                userIsMovingPointer = false
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
        state.putFloat("MAX", max)
        state.putFloat("PROGRESS", progressActual)
        state.putInt("circleColor", circleColor)
        state.putInt("circleProgressColor", circleProgressColor)
        state.putInt("pointerColor", pointerColor)
        state.putInt("pointerHaloColor", pointerHaloColor)
        state.putInt("pointerHaloColorOnTouch", pointerHaloColorOnTouch)
        state.putInt("pointerAlpha", pointerAlpha)
        state.putInt("pointerAlphaOnTouch", pointerAlphaOnTouch)
        state.putFloat("pointerAngle", pointerAngle)
        state.putBoolean("disablePointer", disablePointer)
        state.putBoolean("lockEnabled", isLockEnabled)
        state.putBoolean("negativeEnabled", isNegativeEnabled)
        state.putBoolean("disableProgressGlow", disableProgressGlow)
        state.putBoolean("isInNegativeHalf", isInNegativeHalf)
        state.putInt("circleStyle", circleStyle.ordinal)
        state.putBoolean("hideProgressWhenEmpty", hideProgressWhenEmpty)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as Bundle
        val superState = savedState.getParcelable<Parcelable>("PARENT")
        super.onRestoreInstanceState(superState)
        max = savedState.getFloat("MAX")
        progressActual = savedState.getFloat("PROGRESS")
        circleColor = savedState.getInt("circleColor")
        circleProgressColor = savedState.getInt("circleProgressColor")
        pointerColor = savedState.getInt("pointerColor")
        pointerHaloColor = savedState.getInt("pointerHaloColor")
        pointerHaloColorOnTouch = savedState.getInt("pointerHaloColorOnTouch")
        pointerAlpha = savedState.getInt("pointerAlpha")
        pointerAlphaOnTouch = savedState.getInt("pointerAlphaOnTouch")
        pointerAngle = savedState.getFloat("pointerAngle")
        disablePointer = savedState.getBoolean("disablePointer")
        isLockEnabled = savedState.getBoolean("lockEnabled")
        isNegativeEnabled = savedState.getBoolean("negativeEnabled")
        disableProgressGlow = savedState.getBoolean("disableProgressGlow")
        isInNegativeHalf = savedState.getBoolean("isInNegativeHalf")
        circleStyle = Cap.values()[savedState.getInt("circleStyle")]
        hideProgressWhenEmpty = savedState.getBoolean("hideProgressWhenEmpty")
        initPaints()
        recalculateAll()
    }

    fun setOnSeekBarChangeListener(l: OnCircularSeekBarChangeListener?) {
        onCircularSeekBarChangeListener = l
    }

    /**
     * custom any `Paint`
     */
    private fun applyCustomPaint(paint: Paint, block: (Paint) -> Unit) {
        block.invoke(paint)
        invalidate()
    }

    /**
     * custom `Paint` instance used to draw the circle fill.
     */
    fun applyCustomCircleFillPaint(block: (Paint) -> Unit) {
        applyCustomPaint(circleFillPaint, block)
    }

    /**
     * custom `Paint` instance used to draw the inactive circle.
     */
    fun applyCustomCirclePaint(block: (Paint) -> Unit) {
        applyCustomPaint(circlePaint, block)
    }

    /**
     * custom `Paint` instance used to draw the active circle (represents progress).
     */
    fun applyCustomCircleProgressPaint(block: (Paint) -> Unit) {
        applyCustomPaint(circleProgressPaint, block)
    }

    /**
     * custom `Paint` instance used to draw the center of the pointer.
     */
    fun applyCustomPointerPaint(block: (Paint) -> Unit) {
        applyCustomPaint(pointerPaint, block)
    }

    /**
     * custom `Paint` instance used to draw the glow from the active circle.
     */
    fun applyCustomCircleProgressGlowPaint(block: (Paint) -> Unit) {
        applyCustomPaint(circleProgressGlowPaint, block)
    }

    /**
     * custom `Paint` instance used to draw the halo of the pointer.
     */
    fun applyCustomPointerHaloPaint(block: (Paint) -> Unit) {
        applyCustomPaint(pointerHaloPaint, block)
    }

    /**
     * custom `Paint` instance used to draw the border of the pointer, outside of the halo.
     */
    fun applyCustomPointerHaloBorderPaint(block: (Paint) -> Unit) {
        applyCustomPaint(pointerHaloBorderPaint, block)
    }

    /**
     * Listener for the CircularSeekBar. Implements the same methods as the normal OnSeekBarChangeListener.
     */
    interface OnCircularSeekBarChangeListener {
        fun onProgressChanged(
            circularSeekBar: CircularSeekBar?,
            progress: Float,
            fromUser: Boolean
        )

        fun onStopTrackingTouch(seekBar: CircularSeekBar?)
        fun onStartTrackingTouch(seekBar: CircularSeekBar?)
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