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

    private val bindings: List<MainPageFocusAccountCardBinding> = buildList(4) {
        val inflater = LayoutInflater.from(context)
        repeat(4) {
            add(MainPageFocusAccountCardBinding.inflate(inflater, this@CardStackLayout, true))
        }
    }

    private var visibleCardCount: Int = 3

    init {
        for ((index, binding) in bindings.withIndex()) {
            binding.cardImage.visibility = GONE
            val layoutParams = binding.root.layoutParams as? LayoutParams ?: generateDefaultLayoutParams()
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            binding.root.layoutParams = layoutParams
        }

        val zStep = dpToPxF(1f)
        bindings.getOrNull(INDEX_GHOST)?.root?.translationZ = 0f
        bindings.getOrNull(INDEX_BACK)?.root?.translationZ = zStep
        bindings.getOrNull(INDEX_MIDDLE)?.root?.translationZ = 2f * zStep
        bindings.getOrNull(INDEX_FRONT)?.root?.translationZ = 3f * zStep

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
        val clamped = count.coerceIn(1, 3)
        visibleCardCount = clamped

        val showBack = visibleCardCount >= 3
        val showMiddle = visibleCardCount >= 2
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
        val extraDrop = dpToPxF(24f)
        val dropTo = baseY + height.toFloat() + extraDrop

        animate().cancel()
        animate()
            .translationY(dropTo)
            .setDuration(220L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                setImageUrls(newUrls, animateLayout = false)

                post {
                    animate().cancel()
                    animate()
                        .translationY(baseY)
                        .setDuration(280L)
                        .setInterpolator(OvershootInterpolator(0.9f))
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
            0.34f, 1.56f,
            0.64f, 1f
        )

        spacingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
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
            0
        } else {
            (maxOffset - minOffset).coerceAtLeast(0)
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
        if (minOffset == Int.MAX_VALUE) minOffset = 0

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

            child.pivotX = child.width / 2f
            child.pivotY = 0f

            if (shouldAnimate && child.isLaidOut) {
                child.animate().cancel()
                child.animate()
                    .translationY(topOffset.toFloat())
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(1f)
                    .setDuration(180L)
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


