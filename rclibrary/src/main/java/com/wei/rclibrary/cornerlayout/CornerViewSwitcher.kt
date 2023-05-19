package com.wei.rclibrary.cornerlayout

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ViewSwitcher

class CornerViewSwitcher @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewSwitcher(context, attrs) {

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
        presenter.setupMeasure()
    }

    override fun draw(canvas: Canvas) {
        presenter.drawClip(canvas)
        super.draw(canvas)
    }
}