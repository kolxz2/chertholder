package ru.kolxz2.chertholder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isGone
import coil.load
import ru.kolxz2.chertholder.databinding.MainPageFocusAccountCardBinding
import kotlin.math.roundToInt
import androidx.core.view.isVisible

class CardStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    private companion object {
        private const val INDEX_GHOST = 0
        private const val INDEX_BACK = 1
        private const val INDEX_MIDDLE = 2
        private const val INDEX_FRONT = 3

        private const val TOTAL_CARD_COUNT = 4
        private const val MAX_VISIBLE_CARDS = 3
        private const val MIN_VISIBLE_CARDS = 0
        private const val MIN_CARDS_FOR_FRONT = 1
        private const val MIN_CARDS_FOR_MIDDLE = 2
        private const val MIN_CARDS_FOR_BACK = 3
        private const val MIN_DECK_SIZE_FOR_ROTATION = 1

        private const val BASE_OFFSET_12_DP = 10f
        private const val BASE_OFFSET_23_DP = 7f
        private const val COLLAPSED_OFFSET_12_DP = 4f
        private const val COLLAPSED_OFFSET_23_DP = 3f

        private const val SCALE_GHOST = 0.5f
        private const val SCALE_BACK = 0.64f
        private const val SCALE_MIDDLE = 0.8f
        private const val SCALE_FRONT = 1.0f

        private const val ALPHA_BACK = 0.7f
        private const val ALPHA_MIDDLE = 0.85f
        private const val ALPHA_FRONT = 1.0f
        private const val ALPHA_DEFAULT = 1.0f
        private const val ALPHA_HIDDEN = 0f

        private const val ANIMATION_3_DURATION_MS = 500L
        private const val CARD_CHANGE_DROP_DURATION_MS = 500L
        private const val CARD_CHANGE_RISE_DURATION_MS = 500L
        private const val COMPRESS_ANIMATION_DURATION_MS = 500L
        private const val STACK_TRANSFORM_DURATION_MS = 500L

        private const val CARD_DROP_EXTRA_DP = 24f

        private const val OVERSHOOT_TENSION = 0.9f
        private const val CUBIC_BEZIER_X1 = 0.34f
        private const val CUBIC_BEZIER_Y1 = 1.56f
        private const val CUBIC_BEZIER_X2 = 0.64f
        private const val CUBIC_BEZIER_Y2 = 1f

        private const val Z_ORDER_STEP_DP = 1f

        private const val DECK_URL_NEXT_CANDIDATE_INDEX = 3
        private const val DECK_URL_FALLBACK_INDEX = 0

        private const val PIVOT_Y_TOP = 0f
        private const val PIVOT_X_CENTER_DIVISOR = 2f
    }

    private var currentOffset12Px: Int = dpToPx(BASE_OFFSET_12_DP)
    private var currentOffset23Px: Int = dpToPx(BASE_OFFSET_23_DP)

    private var animateNextLayout: Boolean = false
    private var isCollapsed: Boolean = false
    private var spacingAnimator: ValueAnimator? = null
    private var isAnimation3Running: Boolean = false

    private val bindings: MutableList<MainPageFocusAccountCardBinding> = buildList(TOTAL_CARD_COUNT) {
        val inflater = LayoutInflater.from(context)
        repeat(TOTAL_CARD_COUNT) {
            add(MainPageFocusAccountCardBinding.inflate(inflater, this@CardStackLayout, true))
        }
    }.toMutableList()

    private var visibleCardCount: Int = MAX_VISIBLE_CARDS
    private val deckUrls: MutableList<String> = mutableListOf()

    init {
        applyZOrder()

        bindings.getOrNull(INDEX_GHOST)?.let(::configureAsGhost)
        bindings.getOrNull(INDEX_BACK)?.let(::configureAsCard)
        bindings.getOrNull(INDEX_MIDDLE)?.let(::configureAsCard)
        bindings.getOrNull(INDEX_FRONT)?.let(::configureAsCard)

        // Initially ничего не показываем до прихода данных.
        updateVisibleCardsForCount(MIN_VISIBLE_CARDS, animateLayout = false)
    }

    fun setImageUrls(urls: List<String>, animateLayout: Boolean = true) {
        deckUrls.clear()
        deckUrls.addAll(urls)

        val limited = urls.take(MAX_VISIBLE_CARDS)

        visibleCardCount = limited.size.coerceIn(MIN_VISIBLE_CARDS, MAX_VISIBLE_CARDS)
        updateVisibleCardsForCount(visibleCardCount, animateLayout = animateLayout)

        for (binding in bindings) {
            binding.cardImage.load(null)
            binding.cardImage.visibility = GONE
        }

        val targetBindingIndices = intArrayOf(INDEX_FRONT, INDEX_MIDDLE, INDEX_BACK)
        for (i in limited.indices) {
            val bindingIndex = targetBindingIndices.getOrNull(i) ?: continue
            val binding = bindings.getOrNull(bindingIndex) ?: continue
            val url = limited[i]

            if (url.isBlank()) {
                binding.cardImage.visibility = GONE
            } else {
                binding.cardImage.visibility = VISIBLE
                binding.cardImage.load(url)
            }
        }

        prepareGhostForIncomingBack()
    }

    fun animateCardChange(newUrls: List<String>) {
        if (!isLaidOut) {
            post { animateCardChange(newUrls) }
            return
        }

        for (i in 0 until childCount) {
            getChildAt(i)?.animate()?.cancel()
        }

        val baseY = translationY
        val extraDrop = dpToPxF(CARD_DROP_EXTRA_DP)
        val dropTo = baseY + height.toFloat() + extraDrop

        animate().cancel()
        animate()
            .translationY(dropTo)
            .setDuration(CARD_CHANGE_DROP_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                setImageUrls(newUrls, animateLayout = false)

                post {
                    animate().cancel()
                    animate()
                        .translationY(baseY)
                        .setDuration(CARD_CHANGE_RISE_DURATION_MS)
                        .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
                        .start()
                }
            }
            .start()
    }

    fun animateCompress() {
        if (!isLaidOut) {
            post { animateCompress() }
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
        val end12 = if (targetCollapsed) {
            dpToPx(COLLAPSED_OFFSET_12_DP)
        } else {
            dpToPx(BASE_OFFSET_12_DP)
        }
        val end23 = if (targetCollapsed) {
            dpToPx(COLLAPSED_OFFSET_23_DP)
        } else {
            dpToPx(BASE_OFFSET_23_DP)
        }

        val interpolator = CubicBezierInterpolator(
            CUBIC_BEZIER_X1, CUBIC_BEZIER_Y1,
            CUBIC_BEZIER_X2, CUBIC_BEZIER_Y2
        )

        spacingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COMPRESS_ANIMATION_DURATION_MS
            this.interpolator = interpolator
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                currentOffset12Px = interpolateInt(start12, end12, t)
                currentOffset23Px = interpolateInt(start23, end23, t)
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

    fun animateFocusChange() {
        if (!isLaidOut) {
            post { animateFocusChange() }
            return
        }

        if (visibleCardCount < MIN_CARDS_FOR_BACK || isAnimation3Running) return

        val context = buildFocusContextOrNull() ?: return

        runFocusAnimation(context)
    }

    private fun runFocusAnimation(context: FocusContext) {
        isAnimation3Running = true

        spacingAnimator?.cancel()
        spacingAnimator = null
        for (binding in bindings) {
            binding.root.animate().cancel()
        }

        // Ensure ghost has correct incoming content.
        prepareGhostForIncomingBack()
        context.ghost.visibility = VISIBLE

        val frontY = context.front.translationY
        val middleY = context.middle.translationY
        val backY = context.back.translationY
        val frontScale = context.front.scaleX
        val middleScale = context.middle.scaleX
        val backScale = context.back.scaleX

        val ghostStartOffset = height.toFloat() + dpToPxF(CARD_DROP_EXTRA_DP)
        context.ghost.translationY = backY + ghostStartOffset
        context.ghost.scaleX = SCALE_GHOST
        context.ghost.scaleY = SCALE_GHOST
        context.ghost.alpha = ALPHA_BACK

        val dropBy = height.toFloat() + dpToPxF(CARD_DROP_EXTRA_DP)
        val duration = ANIMATION_3_DURATION_MS
        val interpolator = AccelerateDecelerateInterpolator()

        if (visibleCardCount >= MIN_CARDS_FOR_MIDDLE) {
            context.middle.animate()
                .translationY(frontY)
                .scaleX(frontScale)
                .scaleY(frontScale)
                .alpha(ALPHA_FRONT)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        if (visibleCardCount >= MIN_CARDS_FOR_BACK) {
            context.back.animate()
                .translationY(middleY)
                .scaleX(middleScale)
                .scaleY(middleScale)
                .alpha(ALPHA_MIDDLE)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()

            context.ghost.animate()
                .translationY(backY)
                .scaleX(backScale)
                .scaleY(backScale)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }

        context.front.animate()
            .translationY(frontY + dropBy)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction {
                isAnimation3Running = false
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

    private fun buildFocusContextOrNull(): FocusContext? {
        val ghostBinding = bindings.getOrNull(INDEX_GHOST)
        val backBinding = bindings.getOrNull(INDEX_BACK)
        val middleBinding = bindings.getOrNull(INDEX_MIDDLE)
        val frontBinding = bindings.getOrNull(INDEX_FRONT)

        if (ghostBinding == null || backBinding == null || middleBinding == null || frontBinding == null) {
            return null
        }

        val ghost = ghostBinding.root
        val back = backBinding.root
        val middle = middleBinding.root
        val front = frontBinding.root

        val isFrontVisible = front.isVisible
        val isMiddleVisibleEnough =
            visibleCardCount < MIN_CARDS_FOR_MIDDLE || middle.isVisible
        val isBackVisibleEnough =
            visibleCardCount < MIN_CARDS_FOR_BACK || back.isVisible

        val isValid = isFrontVisible && isMiddleVisibleEnough && isBackVisibleEnough
        if (!isValid) return null

        return FocusContext(
            ghost = ghost,
            back = back,
            middle = middle,
            front = front,
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxChildWidth = 0
        var maxChildHeight = 0
        var minOffset = Int.MAX_VALUE
        var maxOffset = Int.MIN_VALUE

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
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
            if (child.isGone) continue

            val lp = child.layoutParams as MarginLayoutParams

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
                if (isAnimation3Running) {
                    child.pivotX = child.width / PIVOT_X_CENTER_DIVISOR
                    child.pivotY = PIVOT_Y_TOP
                    continue
                }
                child.pivotX = child.width / PIVOT_X_CENTER_DIVISOR
                child.pivotY = PIVOT_Y_TOP
                child.animate().cancel()
                child.translationY = 0f
                child.scaleX = SCALE_GHOST
                child.scaleY = SCALE_GHOST
                child.alpha = ALPHA_HIDDEN
                continue
            }

            val topOffset = topOffsetForIndex(i) - minOffset
            val scale = scaleForIndex(i)
            val alpha = when (i) {
                INDEX_BACK -> ALPHA_BACK
                INDEX_MIDDLE -> ALPHA_MIDDLE
                INDEX_FRONT -> ALPHA_FRONT
                else -> ALPHA_DEFAULT
            }

            child.pivotX = child.width / PIVOT_X_CENTER_DIVISOR
            child.pivotY = PIVOT_Y_TOP

            if (shouldAnimate && child.isLaidOut) {
                child.animate().cancel()
                child.animate()
                    .translationY(topOffset.toFloat())
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(alpha)
                    .setDuration(STACK_TRANSFORM_DURATION_MS)
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

    private fun updateVisibleCardsForCount(count: Int, animateLayout: Boolean) {
        val clamped = count.coerceIn(MIN_VISIBLE_CARDS, MAX_VISIBLE_CARDS)
        visibleCardCount = clamped

        val showBack = clamped >= MIN_CARDS_FOR_BACK
        val showMiddle = clamped >= MIN_CARDS_FOR_MIDDLE
        val showFront = clamped >= MIN_CARDS_FOR_FRONT

        val ghostRoot = bindings.getOrNull(INDEX_GHOST)?.root
        val backRoot = bindings.getOrNull(INDEX_BACK)?.root
        val middleRoot = bindings.getOrNull(INDEX_MIDDLE)?.root
        val frontRoot = bindings.getOrNull(INDEX_FRONT)?.root

        ghostRoot?.visibility = if (clamped == MIN_VISIBLE_CARDS) GONE else VISIBLE
        backRoot?.visibility = if (showBack) VISIBLE else GONE
        middleRoot?.visibility = if (showMiddle) VISIBLE else GONE
        frontRoot?.visibility = if (showFront) VISIBLE else GONE

        animateNextLayout = animateLayout
        requestLayout()
        invalidate()
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

    private fun interpolateInt(start: Int, end: Int, t: Float): Int {
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
            INDEX_GHOST -> SCALE_GHOST
            INDEX_BACK -> SCALE_BACK
            INDEX_MIDDLE -> SCALE_MIDDLE
            else -> SCALE_FRONT
        }
    }

    private fun applyZOrder() {
        val zStep = dpToPxF(Z_ORDER_STEP_DP)
        bindings.getOrNull(INDEX_GHOST)?.root?.translationZ = INDEX_GHOST * zStep
        bindings.getOrNull(INDEX_BACK)?.root?.translationZ = INDEX_BACK * zStep
        bindings.getOrNull(INDEX_MIDDLE)?.root?.translationZ = INDEX_MIDDLE * zStep
        bindings.getOrNull(INDEX_FRONT)?.root?.translationZ = INDEX_FRONT * zStep
    }

    private fun configureAsGhost(binding: MainPageFocusAccountCardBinding) {
        binding.root.isClickable = false
        binding.root.isFocusable = false
        binding.root.isFocusableInTouchMode = false
        binding.root.isEnabled = false
        binding.root.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
    }

    private fun configureAsCard(binding: MainPageFocusAccountCardBinding) {
        binding.root.isEnabled = true
        binding.root.isClickable = false
        binding.root.isFocusable = false
        binding.root.isFocusableInTouchMode = false
        binding.root.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    }

    private fun incomingBackUrl(): String? {
        if (deckUrls.isEmpty()) return null
        val candidate = deckUrls.getOrNull(DECK_URL_NEXT_CANDIDATE_INDEX)
        if (!candidate.isNullOrBlank()) return candidate
        val fallback = deckUrls.getOrNull(DECK_URL_FALLBACK_INDEX)
        return fallback?.takeIf { it.isNotBlank() }
    }

    private fun prepareGhostForIncomingBack() {
        val ghost = bindings.getOrNull(INDEX_GHOST) ?: return
        val url = incomingBackUrl()
        if (url.isNullOrBlank()) {
            ghost.cardImage.load(null)
            ghost.cardImage.visibility = GONE
        } else {
            ghost.cardImage.visibility = VISIBLE
            ghost.cardImage.load(url)
        }
        configureAsGhost(ghost)
    }

    private fun rotateDeckUrlsLeft() {
        if (deckUrls.size <= MIN_DECK_SIZE_FOR_ROTATION) return
        val first = deckUrls.removeAt(DECK_URL_FALLBACK_INDEX)
        deckUrls.add(first)
    }

    private fun rotateRolesAfterScroll() {
        val oldGhost = bindings.getOrNull(INDEX_GHOST)
        val oldBack = bindings.getOrNull(INDEX_BACK)
        val oldMiddle = bindings.getOrNull(INDEX_MIDDLE)
        val oldFront = bindings.getOrNull(INDEX_FRONT)

        if (oldGhost == null || oldBack == null || oldMiddle == null || oldFront == null) {
            return
        }

        bindings.clear()
        bindings.add(oldFront)
        bindings.add(oldGhost)
        bindings.add(oldBack)
        bindings.add(oldMiddle)

        removeAllViews()
        addView(oldFront.root)
        addView(oldGhost.root)
        addView(oldBack.root)
        addView(oldMiddle.root)

        applyZOrder()

        rotateDeckUrlsLeft()

        bindings.getOrNull(INDEX_GHOST)?.let(::configureAsGhost)
        bindings.getOrNull(INDEX_BACK)?.let(::configureAsCard)
        bindings.getOrNull(INDEX_MIDDLE)?.let(::configureAsCard)
        bindings.getOrNull(INDEX_FRONT)?.let(::configureAsCard)

        updateVisibleCardsForCount(visibleCardCount, animateLayout = false)
        prepareGhostForIncomingBack()
        applyStackTransforms(shouldAnimate = false)
        requestLayout()
        invalidate()
    }
}
