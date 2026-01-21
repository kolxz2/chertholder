package ru.kolxz2.chertholder

import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isGone

internal class CardStackTransforms(
    private val container: ViewGroup,
    private val config: Config,
) {
    data class Config(
        val indexGhost: Int,
        val indexBack: Int,
        val indexMiddle: Int,
        val indexFront: Int,
        val scaleGhost: Float,
        val scaleBack: Float,
        val scaleMiddle: Float,
        val scaleFront: Float,
        val alphaBack: Float,
        val alphaMiddle: Float,
        val alphaFront: Float,
        val alphaDefault: Float,
        val alphaHidden: Float,
        val stackTransformDurationMs: Long,
        val pivotYTop: Float,
        val pivotXCenterDivisor: Float,
    )

    fun apply(
        shouldAnimate: Boolean,
        isFocusAnimationRunning: Boolean,
        currentOffset12Px: Int,
        currentOffset23Px: Int,
    ) {
        var minOffset = Int.MAX_VALUE
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.isGone) continue
            if (i != config.indexGhost) {
                minOffset = minOf(minOffset, topOffsetForIndex(i, currentOffset12Px, currentOffset23Px))
            }
        }
        if (minOffset == Int.MAX_VALUE) minOffset = 0

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.isGone) continue

            if (i == config.indexGhost) {
                child.pivotX = child.width / config.pivotXCenterDivisor
                child.pivotY = config.pivotYTop

                if (isFocusAnimationRunning) {
                    // While focus animation is running, ghost is driven by animator.
                    continue
                }

                child.animate().cancel()
                child.translationY = 0f
                child.scaleX = config.scaleGhost
                child.scaleY = config.scaleGhost
                child.alpha = config.alphaHidden
                continue
            }

            val topOffset = topOffsetForIndex(i, currentOffset12Px, currentOffset23Px) - minOffset
            val scale = scaleForIndex(i)
            val alpha = when (i) {
                config.indexBack -> config.alphaBack
                config.indexMiddle -> config.alphaMiddle
                config.indexFront -> config.alphaFront
                else -> config.alphaDefault
            }

            child.pivotX = child.width / config.pivotXCenterDivisor
            child.pivotY = config.pivotYTop

            if (shouldAnimate && child.isLaidOut) {
                child.animate().cancel()
                child.animate()
                    .translationY(topOffset.toFloat())
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(alpha)
                    .setDuration(config.stackTransformDurationMs)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                child.translationY = topOffset.toFloat()
                child.scaleX = scale
                child.scaleY = scale
                child.alpha = alpha
            }
        }
    }

    fun topOffsetForIndex(childIndex: Int, currentOffset12Px: Int, currentOffset23Px: Int): Int {
        return when (childIndex) {
            config.indexGhost -> 0
            config.indexBack -> 0
            config.indexMiddle -> currentOffset23Px
            else -> currentOffset23Px + currentOffset12Px
        }
    }

    fun scaleForIndex(childIndex: Int): Float {
        return when (childIndex) {
            config.indexGhost -> config.scaleGhost
            config.indexBack -> config.scaleBack
            config.indexMiddle -> config.scaleMiddle
            else -> config.scaleFront
        }
    }
}

