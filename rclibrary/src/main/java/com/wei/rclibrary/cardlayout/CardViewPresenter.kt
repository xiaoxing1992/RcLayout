package com.wei.rclibrary.cardlayout

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import com.wei.rclibrary.R
import skin.support.widget.SkinCompatHelper

internal class CardViewPresenter(private val view: View) : SkinCompatHelper() {

    private var cornerRadius = 0f
    var aspectRatio = 0f
    @ColorInt private var strokeColor = Color.TRANSPARENT
    private var strokeWidth = 0f

    private val strokeRect = RectF()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var contentBackgroundDrawable: Drawable? = null

    init {
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0, view.width, view.height, cornerRadius
                )
            }
        }
    }

    fun loadAttrs(context: Context, attributeSet: AttributeSet?) {
        if (attributeSet == null) {
            return
        }
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CardViewLayout)
        cornerRadius = typedArray.getDimension(R.styleable.CardViewLayout_cv_card_radius, 0f)
        strokeWidth = typedArray.getDimension(R.styleable.CardViewLayout_cv_card_stroke_width, 0f)
        strokeColor = typedArray.getColor(
            R.styleable.CardViewLayout_cv_card_stroke_color, Color.TRANSPARENT
        )
        aspectRatio = typedArray.getFloat(R.styleable.CardViewLayout_sky_aspect_ratio, 0f)
        val backgroundNormalColor = typedArray.getColor(
            R.styleable.CardViewLayout_cv_background_color, Color.TRANSPARENT
        )
        val backgroundPressedColor = typedArray.getColor(
            R.styleable.CardViewLayout_cv_background_color_pressed, Color.TRANSPARENT
        )
        typedArray.recycle()
        setupBackgroundDrawable(backgroundNormalColor, backgroundPressedColor)

        if (view is ViewGroup || strokeWidth > 0) {
            view.setWillNotDraw(false)
        }
        changeStrokeColor(strokeColor)
    }

    fun setRadius(cornerRadius: Float) {
        if (this.cornerRadius == cornerRadius) {
            return
        }
        this.cornerRadius = cornerRadius
        view.invalidateOutline()
        if (strokeWidth > 0) {
            view.invalidate()
        }
    }

    fun onSizeChanged(w: Int, h: Int) {
        if (strokeWidth > 0) {
            strokeRect.set(0f, 0f, w.toFloat(), h.toFloat())
            strokeRect.inset(strokeWidth / 2f, strokeWidth / 2f)
        }
    }

    fun drawStroke(canvas: Canvas) {
        if (strokeWidth > 0) {
            canvas.drawRoundRect(
                strokeRect, cornerRadius - strokeWidth / 2f, cornerRadius - strokeWidth / 2f, paint
            )
        }
    }

    fun setupBackgroundDrawable(backgroundNormalColor: Int?, backgroundPressedColor: Int?) {
        this.contentBackgroundDrawable = RippleBackgroundHelper.createBackground(
            backgroundNormalColor, backgroundPressedColor
        )
    }

    fun changeStrokeColor(@ColorInt strokeColor: Int) {
        if (strokeWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.color = strokeColor
        }
    }

    fun bindStyle(forceBind: Boolean = false) {
        if (contentBackgroundDrawable != null || forceBind) {
            view.background = contentBackgroundDrawable
        }
    }

    override fun applySkin() {
        if (contentBackgroundDrawable != null) {
            view.background = contentBackgroundDrawable
        }
    }
}