package ru.kolxz2.chertholder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isGone
import coil.load
import ru.kolxz2.chertholder.databinding.MainPageFocusAccountCardBinding
import kotlin.math.roundToInt

class CardStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        private const val INDEX_GHOST = 0
        private const val INDEX_BACK = 1
        private const val INDEX_MIDDLE = 2
        private const val INDEX_FRONT = 3
        
        private const val TOTAL_CARD_COUNT = 4
        private const val MAX_VISIBLE_CARD_COUNT = 3
        private const val MIN_VISIBLE_CARD_COUNT = 1
        
        private const val Z_STEP_DP = 1f
        private const val Z_MULTIPLIER_BACK = 1f
        private const val Z_MULTIPLIER_MIDDLE = 2f
        private const val Z_MULTIPLIER_FRONT = 3f
        
        private const val EXTRA_DROP_DP = 24f
        
        private const val ANIMATION_DURATION_DROP_MS = 220L
        private const val ANIMATION_DURATION_RISE_MS = 280L
        private const val ANIMATION_DURATION_SPACING_MS = 260L
        private const val ANIMATION_DURATION_STACK_MS = 180L
        
        private const val OVERSHOOT_TENSION = 0.9f
        
        private const val CUBIC_BEZIER_X1 = 0.34f
        private const val CUBIC_BEZIER_Y1 = 1.56f
        private const val CUBIC_BEZIER_X2 = 0.64f
        private const val CUBIC_BEZIER_Y2 = 1f
        
        private const val PIVOT_DIVISOR = 2f
        private const val PIVOT_Y_OFFSET = 0f
        
        private const val MIN_STACK_HEIGHT = 0
        private const val CARD_COUNT_THRESHOLD_MIDDLE = 2
        private const val CARD_COUNT_THRESHOLD_BACK = 3
    }

    private val baseOffset12Px = dpToPx(9f)
    private val baseOffset23Px = dpToPx(7f)
    private val collapsedOffset12Px = dpToPx(8f)
    private val collapsedOffset23Px = dpToPx(5f)

    private var currentOffset12Px: Int = baseOffset12Px
    private var currentOffset23Px: Int = baseOffset23Px

    private val scaleGhost = 0.5f
    private val scaleBack = 0.64f
    private val scaleMiddle = 0.8f
    private val scaleFront = 1.0f

    private var animateNextLayout: Boolean = false
    private var isCollapsed: Boolean = false
    private var spacingAnimator: ValueAnimator? = null

    private val bindings: List<MainPageFocusAccountCardBinding> = buildList(TOTAL_CARD_COUNT) {
        val inflater = LayoutInflater.from(context)
        repeat(TOTAL_CARD_COUNT) {
            add(MainPageFocusAccountCardBinding.inflate(inflater, this@CardStackLayout, true))
        }
    }

    private var visibleCardCount: Int = MAX_VISIBLE_CARD_COUNT

    init {
        for ((index, binding) in bindings.withIndex()) {
            binding.cardImage.visibility = GONE
            val layoutParams = binding.root.layoutParams as? LayoutParams ?: generateDefaultLayoutParams()
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            binding.root.layoutParams = layoutParams
        }

        val zStep = dpToPxF(Z_STEP_DP)
        bindings.getOrNull(INDEX_GHOST)?.root?.translationZ = 0f
        bindings.getOrNull(INDEX_BACK)?.root?.translationZ = Z_MULTIPLIER_BACK * zStep
        bindings.getOrNull(INDEX_MIDDLE)?.root?.translationZ = Z_MULTIPLIER_MIDDLE * zStep
        bindings.getOrNull(INDEX_FRONT)?.root?.translationZ = Z_MULTIPLIER_FRONT * zStep

        bindings.getOrNull(INDEX_GHOST)?.let { ghost ->
            ghost.root.alpha = 0f
            ghost.root.isClickable = false
            ghost.root.isFocusable = false
            ghost.root.isFocusableInTouchMode = false
            ghost.root.isEnabled = false
            ghost.root.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            ghost.cardImage.visibility = GONE
        }

        setCardCount(visibleCardCount)
    }

    fun setImageUrls(urls: List<String>, animateLayout: Boolean = true) {
        val limited = urls.take(3)

        val visible = limited.size.coerceIn(1, 3)
        setCardCount(visible, animateLayout = animateLayout)

        for (binding in bindings) {
            binding.cardImage.load(null)
            binding.cardImage.visibility = GONE
        }

        val targetBindingIndices = intArrayOf(INDEX_FRONT, INDEX_MIDDLE, INDEX_BACK)
        for (i in limited.indices) {
            val bindingIndex = targetBindingIndices.getOrNull(i) ?: continue
            val binding = bindings.getOrNull(bindingIndex) ?: continue
            val url = limited[i]

            binding.cardImage.visibility = VISIBLE
            binding.cardImage.load(url)
        }
    }

    fun setCardCount(count: Int, animateLayout: Boolean = true) {
        val clamped = count.coerceIn(MIN_VISIBLE_CARD_COUNT, MAX_VISIBLE_CARD_COUNT)
        visibleCardCount = clamped

        val showBack = visibleCardCount >= CARD_COUNT_THRESHOLD_BACK
        val showMiddle = visibleCardCount >= CARD_COUNT_THRESHOLD_MIDDLE
        val showFront = true

        bindings.getOrNull(INDEX_GHOST)?.root?.visibility = VISIBLE
        bindings.getOrNull(INDEX_BACK)?.root?.visibility = if (showBack) VISIBLE else GONE
        bindings.getOrNull(INDEX_MIDDLE)?.root?.visibility = if (showMiddle) VISIBLE else GONE
        bindings.getOrNull(INDEX_FRONT)?.root?.visibility = if (showFront) VISIBLE else GONE

        val shouldAnimate = animateLayout
        animateNextLayout = animateLayout
        requestLayout()
        post {
            if (isLaidOut) {
                applyStackTransforms(shouldAnimate = shouldAnimate)
                animateNextLayout = false
            }
        }
        invalidate()
    }

    fun startAnimation1(newUrls: List<String>) {
        if (!isLaidOut) {
            post { startAnimation1(newUrls) }
            return
        }

        for (i in 0 until childCount) {
            getChildAt(i)?.animate()?.cancel()
        }

        val baseY = translationY
        val extraDrop = dpToPxF(EXTRA_DROP_DP)
        val dropTo = baseY + height.toFloat() + extraDrop

        animate().cancel()
        animate()
            .translationY(dropTo)
            .setDuration(ANIMATION_DURATION_DROP_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                setImageUrls(newUrls, animateLayout = false)

                post {
                    animate().cancel()
                    animate()
                        .translationY(baseY)
                        .setDuration(ANIMATION_DURATION_RISE_MS)
                        .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
                        .start()
                }
            }
            .start()
    }

    fun startAnimation2() {
        if (!isLaidOut) {
            post { startAnimation2() }
            return
        }

        spacingAnimator?.cancel()
        spacingAnimator = null

        for (i in 0 until childCount) {
            getChildAt(i)?.animate()?.cancel()
        }

        val targetCollapsed = !isCollapsed
        isCollapsed = targetCollapsed

        val start12 = currentOffset12Px
        val start23 = currentOffset23Px
        val end12 = if (targetCollapsed) collapsedOffset12Px else baseOffset12Px
        val end23 = if (targetCollapsed) collapsedOffset23Px else baseOffset23Px

        val interpolator = CubicBezierInterpolator(
            CUBIC_BEZIER_X1, CUBIC_BEZIER_Y1,
            CUBIC_BEZIER_X2, CUBIC_BEZIER_Y2
        )

        spacingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION_SPACING_MS
            this.interpolator = interpolator
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                currentOffset12Px = lerpInt(start12, end12, t)
                currentOffset23Px = lerpInt(start23, end23, t)
                applyStackTransforms(shouldAnimate = false)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentOffset12Px = end12
                    currentOffset23Px = end23
                    spacingAnimator = null
                    applyStackTransforms(shouldAnimate = false)
                }

                override fun onAnimationCancel(animation: Animator) {
                    spacingAnimator = null
                }
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var maxChildHeight = 0
        var minOffset = Int.MAX_VALUE
        var maxOffset = Int.MIN_VALUE

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            val lp = child.layoutParams as LayoutParams
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin
            maxChildHeight = maxOf(maxChildHeight, childHeight)

            if (i != INDEX_GHOST) {
                val offset = topOffsetForIndex(i)
                minOffset = minOf(minOffset, offset)
                maxOffset = maxOf(maxOffset, offset)
            }
        }

        val stackHeight = if (minOffset == Int.MAX_VALUE) {
            MIN_STACK_HEIGHT
        } else {
            (maxOffset - minOffset).coerceAtLeast(MIN_STACK_HEIGHT)
        }
        val desiredHeight = paddingTop + stackHeight + maxChildHeight + paddingBottom

        setMeasuredDimension(
            measuredWidth,
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }


    private fun applyStackTransforms(shouldAnimate: Boolean) {
        var minOffset = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            if (i != INDEX_GHOST) {
                minOffset = minOf(minOffset, topOffsetForIndex(i))
            }
        }
        if (minOffset == Int.MAX_VALUE) minOffset = MIN_STACK_HEIGHT

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue

            if (i == INDEX_GHOST) {
                child.pivotX = child.width / 2f
                child.pivotY = 0f
                child.animate().cancel()
                child.translationY = 0f
                child.scaleX = scaleGhost
                child.scaleY = scaleGhost
                child.alpha = 0f
                continue
            }

            val topOffset = topOffsetForIndex(i) - minOffset
            val scale = scaleForIndex(i)

            child.pivotX = child.width / PIVOT_DIVISOR
            child.pivotY = PIVOT_Y_OFFSET

            if (shouldAnimate && child.isLaidOut) {
                child.animate().cancel()
                child.animate()
                    .translationY(topOffset.toFloat())
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION_STACK_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                child.translationY = topOffset.toFloat()
                child.scaleX = scale
                child.scaleY = scale
                child.alpha = 1f
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPxF(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    private fun lerpInt(start: Int, end: Int, t: Float): Int {
        return (start + (end - start) * t).roundToInt()
    }

    private fun topOffsetForIndex(childIndex: Int): Int {
        return when (childIndex) {
            INDEX_GHOST -> 0
            INDEX_BACK -> 0
            INDEX_MIDDLE -> currentOffset23Px
            else -> currentOffset23Px + currentOffset12Px
        }
    }

    private fun scaleForIndex(childIndex: Int): Float {
        return when (childIndex) {
            INDEX_GHOST -> scaleGhost
            INDEX_BACK -> scaleBack
            INDEX_MIDDLE -> scaleMiddle
            else -> scaleFront
        }
    }
}


