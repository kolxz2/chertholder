package ru.kolxz2.chertholder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import coil.load
import ru.kolxz2.chertholder.databinding.ItemCardBinding
import kotlin.math.roundToInt

class CardStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private companion object {
        private const val INDEX_GHOST = 0
        private const val INDEX_BACK = 1
        private const val INDEX_MIDDLE = 2
        private const val INDEX_FRONT = 3
    }

    // Spacing requirements:
    // - between front(1) and middle(2): 10dp
    // - between middle(2) and back(3): 7dp
    //
   // We layout from back to front:
    // back(3): 0
    // middle(2): 7dp
    // front(1): 7dp + 10dp = 17dp
    private val baseOffset12Px = dpToPx(10f)
    private val baseOffset23Px = dpToPx(7f)
    private val collapsedOffset12Px = dpToPx(4f)
    private val collapsedOffset23Px = dpToPx(3f)

    private var currentOffset12Px: Int = baseOffset12Px
    private var currentOffset23Px: Int = baseOffset23Px

    private val scaleGhost = 0.5f
    private val scaleBack = 0.64f
    private val scaleMiddle = 0.8f
    private val scaleFront = 1.0f

    private var animateNextLayout: Boolean = false
    private var isCollapsed: Boolean = false
    private var spacingAnimator: ValueAnimator? = null

    /**
     * Children order is important:
     * - index 0: ghost card (fully invisible, behind all others)
     * - index 1: back card (peeks the most)
     * - index 2: middle card
     * - index 3: front card (drawn last, on top)
     */
    private val bindings: List<ItemCardBinding> = buildList(4) {
        val inflater = LayoutInflater.from(context)
        repeat(4) {
            add(ItemCardBinding.inflate(inflater, this@CardStackLayout, true))
        }
    }

    private var visibleCardCount: Int = 3

    init {
        // Debug visuals: paint cards with different colors (no images for now).
        val colors = intArrayOf(
            Color.parseColor("#BBDEFB"), // light blue
            Color.parseColor("#C8E6C9"), // light green
            Color.parseColor("#FFE0B2")  // light orange
        )
        for ((index, binding) in bindings.withIndex()) {
            binding.imageView.visibility = View.GONE
            binding.cardView.setCardBackgroundColor(colors[index % colors.size])
        }

        // Ensure front card is visually on top even with elevations.
        val zStep = dpToPxF(1f)
        bindings.getOrNull(INDEX_GHOST)?.root?.translationZ = 0f
        bindings.getOrNull(INDEX_BACK)?.root?.translationZ = zStep
        bindings.getOrNull(INDEX_MIDDLE)?.root?.translationZ = 2f * zStep
        bindings.getOrNull(INDEX_FRONT)?.root?.translationZ = 3f * zStep

        // Ghost card: always present behind everything, but fully invisible and non-interactive.
        bindings.getOrNull(INDEX_GHOST)?.let { ghost ->
            ghost.root.alpha = 0f
            ghost.root.isClickable = false
            ghost.root.isFocusable = false
            ghost.root.isFocusableInTouchMode = false
            ghost.root.isEnabled = false
            ViewCompat.setImportantForAccessibility(
                ghost.root,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
            )
            ghost.imageView.visibility = View.GONE
        }

        // Default: show all 3 cards.
        setCardCount(visibleCardCount)
    }

    /**
     * Display images from URLs. Shows up to 3 images.
     *
     * Mapping for UX:
     * - urls[0] -> front card (index 3)
     * - urls[1] -> middle card (index 2)
     * - urls[2] -> back card (index 1)
     */
    fun setImageUrls(urls: List<String>, animateLayout: Boolean = true) {
        val limited = urls.take(3)

        // Keep at least 1 card visible (even with 0 URLs) to preserve the layout,
        // but hide the image when there's nothing to show.
        val visible = limited.size.coerceIn(1, 3)
        setCardCount(visible, animateLayout = animateLayout)

        // Clear/hide all images first (important for reuse and partial updates).
        for (binding in bindings) {
            binding.imageView.load(null)
            binding.imageView.visibility = View.GONE
        }

        // Fill in images using the defined mapping.
        val targetBindingIndices = intArrayOf(INDEX_FRONT, INDEX_MIDDLE, INDEX_BACK) // front, middle, back
        for (i in limited.indices) {
            val bindingIndex = targetBindingIndices.getOrNull(i) ?: continue
            val binding = bindings.getOrNull(bindingIndex) ?: continue
            val url = limited[i]

            binding.imageView.visibility = View.VISIBLE
            binding.imageView.load(url)
        }
    }

    /**
     * Set how many cards to show (1..3).
     * 1 -> only front card
     * 2 -> middle + front
     * 3 -> back + middle + front
     */
    fun setCardCount(count: Int, animateLayout: Boolean = true) {
        val clamped = count.coerceIn(1, 3)
        visibleCardCount = clamped

        // child index: 0=ghost, 1=back, 2=middle, 3=front
        val showBack = visibleCardCount >= 3
        val showMiddle = visibleCardCount >= 2
        val showFront = true

        // Ghost is always present but invisible (alpha=0) and should not affect layout math.
        bindings.getOrNull(INDEX_GHOST)?.root?.visibility = View.VISIBLE
        bindings.getOrNull(INDEX_BACK)?.root?.visibility = if (showBack) View.VISIBLE else View.GONE
        bindings.getOrNull(INDEX_MIDDLE)?.root?.visibility = if (showMiddle) View.VISIBLE else View.GONE
        bindings.getOrNull(INDEX_FRONT)?.root?.visibility = if (showFront) View.VISIBLE else View.GONE

        // Animate next layout pass when stack composition changes.
        animateNextLayout = animateLayout
        requestLayout()
        invalidate()
    }

    fun getCardCount(): Int = visibleCardCount

    /**
     * Animation #1: drop the whole stack down, replace cards, then rise back.
     *
     * Contract:
     * - The stack moves down out of view.
     * - Data is replaced while it's down (so the user doesn't see a hard swap).
     * - Then the stack returns to its original Y position.
     */
    fun startAnimation1(newUrls: List<String>) {
        if (!isLaidOut) {
            post { startAnimation1(newUrls) }
            return
        }

        // Cancel any per-card animations that might be running.
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
                // Replace while we're down.
                setImageUrls(newUrls, animateLayout = false)

                // Let layout/image work enqueue before we rise back up.
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

    /**
     * Animation #2: toggle stack spacing.
     *
     * Collapsed state:
     * - between front(1) and middle(2): 4dp
     * - between middle(2) and back(3): 3dp
     *
     * Expanded state restores the default: 10dp / 7dp.
     */
    fun startAnimation2() {
        if (!isLaidOut) {
            post { startAnimation2() }
            return
        }

        // Cancel any ongoing stack-spacing animation.
        spacingAnimator?.cancel()
        spacingAnimator = null

        // Cancel any per-card property animations (we will drive translationY directly).
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

    /**
     * Animation #3 (temporary): "swipe + tilt" the front card and return it back.
     * This reuses the old placeholder effect that previously lived in animation #2.
     */
    fun startAnimation3() {
        if (!isLaidOut) {
            post { startAnimation3() }
            return
        }

        val front = bindings.getOrNull(INDEX_FRONT)?.root ?: return
        if (front.visibility != View.VISIBLE) return

        front.animate().cancel()

        val baseTranslationX = front.translationX
        val baseRotation = front.rotation

        val swipeDistance = (width.takeIf { it > 0 } ?: front.width).toFloat() * 0.25f
        val targetTranslationX = baseTranslationX + swipeDistance

        front.animate()
            .translationX(targetTranslationX)
            .rotation(baseRotation + 8f)
            .setDuration(180L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                front.animate()
                    .translationX(baseTranslationX)
                    .rotation(baseRotation)
                    .setDuration(240L)
                    .setInterpolator(OvershootInterpolator(1.8f))
                    .start()
            }
            .start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxChildWidth = 0
        var maxChildHeight = 0
        var minOffset = Int.MAX_VALUE
        var maxOffset = Int.MIN_VALUE

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin
            maxChildWidth = maxOf(maxChildWidth, childWidth)
            maxChildHeight = maxOf(maxChildHeight, childHeight)

            // Ghost card must not affect the measured height of the visible stack.
            if (i != INDEX_GHOST) {
                val offset = topOffsetForIndex(i)
                minOffset = minOf(minOffset, offset)
                maxOffset = maxOf(maxOffset, offset)
            }
        }

        val desiredWidth = paddingLeft + maxChildWidth + paddingRight
        val stackHeight = if (minOffset == Int.MAX_VALUE) {
            0
        } else {
            (maxOffset - minOffset).coerceAtLeast(0)
        }
        val desiredHeight = paddingTop + stackHeight + maxChildHeight + paddingBottom

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val shouldAnimate = animateNextLayout
        animateNextLayout = false

        val availableWidth = (r - l) - paddingLeft - paddingRight

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val lp = child.layoutParams as MarginLayoutParams

            // Physically lay out all cards at the same top, centered by X.
            // Visual depth is done via translationY + scale (animatable properties).
            val totalWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val centeredLeft = paddingLeft + ((availableWidth - totalWidth) / 2)
            val left = centeredLeft + lp.leftMargin
            val top = paddingTop + lp.topMargin
            val right = left + child.measuredWidth
            val bottom = top + child.measuredHeight

            child.layout(left, top, right, bottom)
        }

        applyStackTransforms(shouldAnimate = shouldAnimate)
    }

    private fun applyStackTransforms(shouldAnimate: Boolean) {
        var minOffset = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            // Ghost card must not affect the normalization of translationY for visible cards.
            if (i != INDEX_GHOST) {
                minOffset = minOf(minOffset, topOffsetForIndex(i))
            }
        }
        if (minOffset == Int.MAX_VALUE) minOffset = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            if (i == INDEX_GHOST) {
                // Keep ghost at a fixed position/scale and fully invisible.
                child.pivotX = child.width / 2f
                child.pivotY = 0f
                child.animate().cancel()
                child.translationY = 0f
                child.scaleX = scaleGhost
                child.scaleY = scaleGhost
                child.alpha = 0f
                continue
            }

            // Shift so that the top-most visible card starts at 0.
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
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
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
        // index 0 = ghost, 1 = back, 2 = middle, 3 = front
        return when (childIndex) {
            INDEX_GHOST -> scaleGhost
            INDEX_BACK -> scaleBack
            INDEX_MIDDLE -> scaleMiddle
            else -> scaleFront
        }
    }
}


