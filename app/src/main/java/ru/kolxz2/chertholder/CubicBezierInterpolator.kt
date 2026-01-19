package ru.kolxz2.chertholder

import android.animation.TimeInterpolator
import android.view.animation.PathInterpolator

/**
 * Cubic-bezier interpolator compatible with CSS-like control points.
 *
 * Note: x1 and x2 must be in [0..1]. y values may overshoot for bounce-like curves.
 */
class CubicBezierInterpolator(
    controlX1: Float,
    controlY1: Float,
    controlX2: Float,
    controlY2: Float
) : TimeInterpolator {

    private val delegate = PathInterpolator(controlX1, controlY1, controlX2, controlY2)

    override fun getInterpolation(input: Float): Float = delegate.getInterpolation(input)
}


