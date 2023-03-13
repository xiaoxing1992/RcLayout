package com.wei.rclibrary.cardlayout

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
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

    fun createMaskBackground(backgroundColor: Int?, pressedColor: Int?): Drawable? {
        if ((backgroundColor == null || backgroundColor == Color.TRANSPARENT)) {
            return null
        }
        return createMaskBackground(backgroundColor, pressedColor)
    }

    fun createMaskBackground(
        backgroundColor: Int, pressedColor: Int?,
    ): Drawable? {
        if (pressedColor == null || pressedColor == Color.TRANSPARENT) {
            return null
        }

        // ripple 的显示的背景颜色的是指定颜色的 50 % 透明 。 所以需要有加深颜色
        val doubleAlphaColor = ColorUtils.setAlphaComponent(
            pressedColor, min(2 * Color.alpha(pressedColor), 255)
        )

        val outRadius = floatArrayOf(10f, 10f, 15f, 15f, 20f, 20f, 25f, 25f)
        val roundRectShape = RoundRectShape(outRadius, null, null)
        val maskDrawable = ShapeDrawable()
        maskDrawable.shape = roundRectShape;
        maskDrawable.paint.color = doubleAlphaColor
        maskDrawable.paint.style = Paint.Style.FILL

        val pressedColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_pressed), StateSet.NOTHING),
            intArrayOf(doubleAlphaColor, doubleAlphaColor)
        )
        val contentDrawable = ShapeDrawable()
        contentDrawable.shape = roundRectShape
        contentDrawable.paint.color = backgroundColor
        contentDrawable.paint.style = Paint.Style.FILL



        return RippleDrawable(pressedColors, contentDrawable, maskDrawable)
    }
}