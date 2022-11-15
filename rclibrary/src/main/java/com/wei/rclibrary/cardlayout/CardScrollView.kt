package com.wei.rclibrary.cardlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView

open class CardScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private val presenter = CardViewPresenter(this)

    init {
        presenter.loadAttrs(context, attrs)
        presenter.bindStyle()
    }

    fun setRadius(
        cornerRadius: Float
    ) {
        presenter.setRadius(cornerRadius)
    }

    fun setStrokeWidth(
        cornerRadius: Float
    ) {
        presenter.changeStrokeWidth(cornerRadius)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        presenter.onSizeChanged(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (presenter.aspectRatio > 0) {
            val width = measuredWidth
            val height = (width / presenter.aspectRatio).toInt()
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        presenter.drawStroke(canvas)
    }

    fun changeColor(
        @ColorRes backgroundNormalColorRes: Int,
        @ColorRes backgroundPressedColorRes: Int? = null,
        @ColorRes strokeColorRes: Int? = null
    ) {
        changeColorInt(
            backgroundNormalColor = ContextCompat.getColor(context, backgroundNormalColorRes),
            backgroundPressedColor = if (backgroundPressedColorRes == null) null else ContextCompat.getColor(
                context, backgroundPressedColorRes
            ),
            strokeColor = if (strokeColorRes == null) Color.TRANSPARENT else ContextCompat.getColor(
                context, strokeColorRes
            )
        )
    }

    fun changeColorInt(
        @ColorInt backgroundNormalColor: Int,
        @ColorInt backgroundPressedColor: Int? = null,
        @ColorInt strokeColor: Int? = null
    ) {
        presenter.setupBackgroundDrawable(backgroundNormalColor, backgroundPressedColor)
        presenter.changeStrokeColor(strokeColor ?: Color.TRANSPARENT)
        presenter.bindStyle(true)
    }
}