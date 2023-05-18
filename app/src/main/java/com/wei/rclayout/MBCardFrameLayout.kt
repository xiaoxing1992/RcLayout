package com.wei.rclayout

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.wei.rclibrary.cardlayout.CardFrameLayout
import skin.support.widget.SkinCompatSupportable

class MBCardFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CardFrameLayout(context, attrs), SkinCompatSupportable {

    private var mbBackgroundColor = R.color.purple_200
    private var mbBackgroundPressedColor = R.color.purple_500
    private var mbStrokeColor = R.color.purple_700
    private var mbStrokeWidth = 10
    private var mbRadius = 12

    init {
        context.withStyledAttributes(attrs, R.styleable.MBCardFrameLayout, 0) {
            mbBackgroundColor = getResourceId(
                R.styleable.MBCardFrameLayout_mb_card_background_color, mbBackgroundColor
            )
            mbBackgroundPressedColor = getResourceId(
                R.styleable.MBCardFrameLayout_mb_card_background_color_pressed,
                mbBackgroundPressedColor
            )
            mbStrokeColor = getResourceId(
                R.styleable.MBCardFrameLayout_mb_card_stroke_color, mbStrokeColor
            )
            mbStrokeWidth = getDimensionPixelSize(
                R.styleable.MBCardFrameLayout_mb_card_corner_radius, mbStrokeWidth
            )
            mbRadius = getDimensionPixelSize(
                R.styleable.MBCardFrameLayout_mb_card_corner_radius, mbRadius
            )
        }
        bindStyle()
    }

    private fun bindStyle() {
        changeColorInt(
            backgroundNormalColor = ContextCompat.getColor(this.context, mbBackgroundColor),
            backgroundPressedColor = ContextCompat.getColor(context, mbBackgroundPressedColor),
            strokeColor = ContextCompat.getColor(context, mbStrokeColor)
        )
        //setStrokeWidth(mbStrokeWidth.toFloat())
        setRadius(mbRadius.toFloat())
    }

    override fun applySkin() {
        bindStyle()
    }
}