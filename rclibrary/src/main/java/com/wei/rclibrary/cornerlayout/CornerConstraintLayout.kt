package com.wei.rclibrary.cornerlayout

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

open class CornerConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val presenter = CornerViewPresenter(this)

    init {
        presenter.loadAttrs(context, attrs)
        presenter.bindStyle()
    }

    fun setRadius(
        cornerRadius: Float,
        topLeft: Boolean,
        topRight: Boolean,
        bottomRight: Boolean,
        bottomLeft: Boolean
    ) {
        presenter.setRadius(cornerRadius, topLeft, topRight, bottomRight, bottomLeft)
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
        presenter.setupMeasure()
    }

    override fun draw(canvas: Canvas) {
        presenter.drawClip(canvas)
        super.draw(canvas)
    }

    fun changeColor(
        @ColorRes backgroundNormalColorRes: Int, @ColorRes backgroundPressedColorRes: Int? = null
    ) {
        presenter.setupBackgroundDrawable(
            ContextCompat.getColor(context, backgroundNormalColorRes),
            if (backgroundPressedColorRes == null) null else ContextCompat.getColor(
                context, backgroundPressedColorRes
            )
        )
        presenter.bindStyle()
    }

    fun changeColorInt(
        @ColorInt backgroundNormalColorRes: Int, @ColorInt backgroundPressedColorRes: Int? = null
    ) {
        presenter.setupBackgroundDrawable(
            backgroundNormalColorRes, backgroundPressedColorRes
        )
        presenter.bindStyle()
    }
}
