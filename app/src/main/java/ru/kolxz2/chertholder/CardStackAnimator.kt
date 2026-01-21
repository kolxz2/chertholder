package ru.kolxz2.chertholder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import kotlin.math.roundToInt

internal class CardStackAnimator(
    private val container: ViewGroup,
    private val slots: CardStackSlots,
    private val deck: CardStackDeck,
    private val config: Config,
    private val getVisibleCardCount: () -> Int,
    private val getCurrentOffset12Px: () -> Int,
    private val getCurrentOffset23Px: () -> Int,
    private val setCurrentOffsetsPx: (offset12Px: Int, offset23Px: Int) -> Unit,
    private val applyStackTransforms: (shouldAnimate: Boolean) -> Unit,
    private val setImageUrls: (urls: List<String>, animateLayout: Boolean) -> Unit,
    private val rotateRolesAfterScroll: () -> Unit,
    private val dpToPx: (Float) -> Int,
    private val dpToPxF: (Float) -> Float,
) {
    data class Config(
        val maxVisibleCards: Int,
        val minCardsForMiddle: Int,
        val minCardsForBack: Int,
        val baseOffset12Dp: Float,
        val baseOffset23Dp: Float,
        val collapsedOffset12Dp: Float,
        val collapsedOffset23Dp: Float,
        val scaleGhost: Float,
        val alphaBack: Float,
        val alphaMiddle: Float,
        val alphaFront: Float,
        val animation3DurationMs: Long,
        val cardChangeDropDurationMs: Long,
        val cardChangeRiseDurationMs: Long,
        val compressAnimationDurationMs: Long,
        val overshootTension: Float,
        val cubicBezierX1: Float,
        val cubicBezierY1: Float,
        val cubicBezierX2: Float,
        val cubicBezierY2: Float,
        val cardDropExtraDp: Float,
    )

    private var spacingAnimator: ValueAnimator? = null
    private var isCollapsed: Boolean = false

    var isFocusAnimationRunning: Boolean = false
        private set

    fun animateCardChange(newUrls: List<String>) {
        if (!container.isLaidOut) {
            container.post { animateCardChange(newUrls) }
            return
        }

        for (i in 0 until container.childCount) {
            container.getChildAt(i)?.animate()?.cancel()
        }

        val baseY = container.translationY
        val extraDrop = dpToPxF(config.cardDropExtraDp)
        val dropTo = baseY + container.height.toFloat() + extraDrop

        container.animate().cancel()
        container.animate()
            .translationY(dropTo)
            .setDuration(config.cardChangeDropDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                setImageUrls(newUrls, false)

                container.post {
                    container.animate().cancel()
                    container.animate()
                        .translationY(baseY)
                        .setDuration(config.cardChangeRiseDurationMs)
                        .setInterpolator(OvershootInterpolator(config.overshootTension))
                        .start()
                }
            }
            .start()
    }

    fun animateCompress() {
        if (!container.isLaidOut) {
            container.post { animateCompress() }
            return
        }

        spacingAnimator?.cancel()
        spacingAnimator = null

        for (i in 0 until container.childCount) {
            container.getChildAt(i)?.animate()?.cancel()
        }

        val targetCollapsed = !isCollapsed
        isCollapsed = targetCollapsed

        val start12 = getCurrentOffset12Px()
        val start23 = getCurrentOffset23Px()

        val end12 = if (targetCollapsed) {
            dpToPx(config.collapsedOffset12Dp)
        } else {
            dpToPx(config.baseOffset12Dp)
        }
        val end23 = if (targetCollapsed) {
            dpToPx(config.collapsedOffset23Dp)
        } else {
            dpToPx(config.baseOffset23Dp)
        }

        val interpolator = CubicBezierInterpolator(
            config.cubicBezierX1, config.cubicBezierY1,
            config.cubicBezierX2, config.cubicBezierY2,
        )

        spacingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.compressAnimationDurationMs
            this.interpolator = interpolator
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                val current12 = interpolateInt(start12, end12, t)
                val current23 = interpolateInt(start23, end23, t)
                setCurrentOffsetsPx(current12, current23)
                applyStackTransforms(false)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    setCurrentOffsetsPx(end12, end23)
                    spacingAnimator = null
                    applyStackTransforms(false)
                }

                override fun onAnimationCancel(animation: Animator) {
                    spacingAnimator = null
                }
            })
            start()
        }
    }

    fun animateFocusChange() {
        if (!container.isLaidOut) {
            container.post { animateFocusChange() }
            return
        }

        val visibleCardCount = getVisibleCardCount()
        if (visibleCardCount < config.minCardsForBack || isFocusAnimationRunning) return

        val context = buildFocusContextOrNull(visibleCardCount) ?: return
        runFocusAnimation(context, visibleCardCount)
    }

    private fun runFocusAnimation(context: FocusContext, visibleCardCount: Int) {
        isFocusAnimationRunning = true
        resetRunningAnimations()
        deck.prepareGhostForIncomingBack(slots)
        context.ghost.visibility = View.VISIBLE

        val metrics = buildFocusMetrics(context)

        animateMiddleAndBack(context, metrics, visibleCardCount)
        animateFrontAndGhost(context, metrics)
    }

    private fun resetRunningAnimations() {
        spacingAnimator?.cancel()
        spacingAnimator = null
        slots.forEachBinding { it.root.animate().cancel() }
    }

    private data class FocusMetrics(
        val frontY: Float,
        val middleY: Float,
        val backY: Float,
        val frontScale: Float,
        val middleScale: Float,
        val backScale: Float,
        val dropBy: Float,
        val duration: Long,
        val interpolator: AccelerateDecelerateInterpolator,
    )

    private fun buildFocusMetrics(context: FocusContext): FocusMetrics {
        val frontY = context.front.translationY
        val middleY = context.middle.translationY
        val backY = context.back.translationY
        val frontScale = context.front.scaleX
        val middleScale = context.middle.scaleX
        val backScale = context.back.scaleX

        val ghostStartOffset = container.height.toFloat() + dpToPxF(config.cardDropExtraDp)
        context.ghost.translationY = backY + ghostStartOffset
        context.ghost.scaleX = config.scaleGhost
        context.ghost.scaleY = config.scaleGhost
        context.ghost.alpha = config.alphaBack

        val dropBy = container.height.toFloat() + dpToPxF(config.cardDropExtraDp)
        val duration = config.animation3DurationMs
        val interpolator = AccelerateDecelerateInterpolator()

        return FocusMetrics(
            frontY = frontY,
            middleY = middleY,
            backY = backY,
            frontScale = frontScale,
            middleScale = middleScale,
            backScale = backScale,
            dropBy = dropBy,
            duration = duration,
            interpolator = interpolator,
        )
    }

    private fun animateMiddleAndBack(
        context: FocusContext,
        metrics: FocusMetrics,
        visibleCardCount: Int,
    ) {
        if (visibleCardCount >= config.minCardsForMiddle) {
            context.middle.animate()
                .translationY(metrics.frontY)
                .scaleX(metrics.frontScale)
                .scaleY(metrics.frontScale)
                .alpha(config.alphaFront)
                .setDuration(metrics.duration)
                .setInterpolator(metrics.interpolator)
                .start()
        }

        if (visibleCardCount >= config.minCardsForBack) {
            context.back.animate()
                .translationY(metrics.middleY)
                .scaleX(metrics.middleScale)
                .scaleY(metrics.middleScale)
                .alpha(config.alphaMiddle)
                .setDuration(metrics.duration)
                .setInterpolator(metrics.interpolator)
                .start()

            context.ghost.animate()
                .translationY(metrics.backY)
                .scaleX(metrics.backScale)
                .scaleY(metrics.backScale)
                .setDuration(metrics.duration)
                .setInterpolator(metrics.interpolator)
                .start()
        }
    }

    private fun animateFrontAndGhost(
        context: FocusContext,
        metrics: FocusMetrics,
    ) {
        context.front.animate()
            .translationY(metrics.frontY + metrics.dropBy)
            .setDuration(metrics.duration)
            .setInterpolator(metrics.interpolator)
            .withEndAction {
                isFocusAnimationRunning = false
                rotateRolesAfterScroll()
            }
            .start()
    }

    private data class FocusContext(
        val ghost: View,
        val back: View,
        val middle: View,
        val front: View,
    )

    private fun buildFocusContextOrNull(visibleCardCount: Int): FocusContext? {
        val ghost = slots.ghostRoot ?: return null
        val back = slots.backRoot ?: return null
        val middle = slots.middleRoot ?: return null
        val front = slots.frontRoot ?: return null

        val isFrontVisible = front.isVisible
        val isMiddleVisibleEnough =
            visibleCardCount < config.minCardsForMiddle || middle.isVisible
        val isBackVisibleEnough =
            visibleCardCount < config.minCardsForBack || back.isVisible

        val isValid = isFrontVisible && isMiddleVisibleEnough && isBackVisibleEnough
        if (!isValid) return null

        return FocusContext(
            ghost = ghost,
            back = back,
            middle = middle,
            front = front,
        )
    }

    private fun interpolateInt(start: Int, end: Int, t: Float): Int {
        return (start + (end - start) * t).roundToInt()
    }
}

