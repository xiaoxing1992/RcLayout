package com.wei.rclibrary.cornerlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.wei.rclibrary.R
import com.wei.rclibrary.cardlayout.RippleBackgroundHelper

class CornerViewPresenter(private val view: View) {

    private var radii = FloatArray(8)

    private var cornerRadius = 0f
    var aspectRatio = 0f
    private val rect = RectF()
    private var clipPath = Path()

    private var contentBackgroundDrawable: Drawable? = null

    init {
        if (view is ViewGroup) {
            view.setWillNotDraw(false)
        }
    }

    fun loadAttrs(context: Context, attributeSet: AttributeSet?) {
        if (attributeSet == null) {
            return
        }
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CornerViewLayout)
        cornerRadius = typedArray.getDimension(R.styleable.CornerViewLayout_cv_corner_radius, 0f)
        val topLeft: Boolean = typedArray.getBoolean(
            R.styleable.CornerViewLayout_cv_corner_top_left, true
        )
        val topRight: Boolean = typedArray.getBoolean(
            R.styleable.CornerViewLayout_cv_corner_top_right, true
        )
        val bottomLeft: Boolean = typedArray.getBoolean(
            R.styleable.CornerViewLayout_cv_corner_bottom_left, true
        )
        val bottomRight: Boolean = typedArray.getBoolean(
            R.styleable.CornerViewLayout_cv_corner_bottom_right, true
        )
        aspectRatio = typedArray.getFloat(R.styleable.CornerViewLayout_sky_aspect_ratio, 0f)
        val backgroundNormalColor = typedArray.getColor(
            R.styleable.CornerViewLayout_cv_background_color, Color.TRANSPARENT
        )
        val backgroundPressedColor = typedArray.getColor(
            R.styleable.CornerViewLayout_cv_background_color_pressed, Color.TRANSPARENT
        )
        typedArray.recycle()
        setupBackgroundDrawable(backgroundNormalColor, backgroundPressedColor)
        setRadius(cornerRadius, topLeft, topRight, bottomRight, bottomLeft)
    }

    fun setRadius(
        cornerRadius: Float,
        topLeft: Boolean,
        topRight: Boolean,
        bottomRight: Boolean,
        bottomLeft: Boolean
    ) {
        this.cornerRadius = cornerRadius

        if (topLeft) {
            radii[0] = cornerRadius
            radii[1] = cornerRadius
        } else {
            radii[0] = 0f
            radii[1] = 0f
        }

        if (topRight) {
            radii[2] = cornerRadius
            radii[3] = cornerRadius
        } else {
            radii[2] = 0f
            radii[3] = 0f
        }

        if (bottomRight) {
            radii[4] = cornerRadius
            radii[5] = cornerRadius
        } else {
            radii[4] = 0f
            radii[5] = 0f
        }

        if (bottomLeft) {
            radii[6] = cornerRadius
            radii[7] = cornerRadius
        } else {
            radii[6] = 0f
            radii[7] = 0f
        }

        view.invalidate()
    }

    fun setupMeasure() {
        rect.set(0f, 0f, view.measuredWidth.toFloat(), view.measuredHeight.toFloat())
    }

    fun drawClip(canvas: Canvas) {
        if (cornerRadius <= 0) {
            return
        }
        clipPath.reset()
        clipPath.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.clipPath(clipPath)
    }

    fun setupBackgroundDrawable(backgroundNormalColor: Int?, backgroundPressedColor: Int?) {
        this.contentBackgroundDrawable = RippleBackgroundHelper.createBackground(
            backgroundNormalColor, backgroundPressedColor
        )
    }

    fun bindStyle() {
        if (contentBackgroundDrawable != null) {
            view.background = contentBackgroundDrawable
        }
    }
}