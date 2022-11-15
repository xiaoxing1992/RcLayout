package com.wei.rclibrary.cardlayout

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.StateSet
import androidx.core.graphics.ColorUtils
import kotlin.math.min

object RippleBackgroundHelper {

    fun createBackground(backgroundColor: Int?, pressedColor: Int?): Drawable? {
        if ((backgroundColor == null || backgroundColor == Color.TRANSPARENT)) {
            return null
        }
        return createBackground(ColorDrawable(backgroundColor), pressedColor)
    }

    fun createBackground(background: Drawable?, pressedColor: Int?): Drawable? {
        if (background == null || pressedColor == null || pressedColor == Color.TRANSPARENT) {
            return background
        }
        // ripple 的显示的背景颜色的是指定颜色的 50 % 透明 。 所以需要有加深颜色
        val doubleAlphaColor = ColorUtils.setAlphaComponent(
            pressedColor, min(2 * Color.alpha(pressedColor), 255)
        )
        val pressedColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_pressed), StateSet.NOTHING),
            intArrayOf(doubleAlphaColor, doubleAlphaColor)
        )
        return RippleDrawable(pressedColors, background, ColorDrawable(Color.WHITE))
    }
}