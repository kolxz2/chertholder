package ru.kolxz2.chertholder

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.view.isGone

class CardStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    private companion object {
        private const val INDEX_GHOST = CardStackSlots.INDEX_GHOST
        private const val INDEX_BACK = CardStackSlots.INDEX_BACK
        private const val INDEX_MIDDLE = CardStackSlots.INDEX_MIDDLE
        private const val INDEX_FRONT = CardStackSlots.INDEX_FRONT

        private const val MAX_VISIBLE_CARDS = 3
        private const val MIN_VISIBLE_CARDS = 0
        private const val MIN_CARDS_FOR_FRONT = 1
        private const val MIN_CARDS_FOR_MIDDLE = 2
        private const val MIN_CARDS_FOR_BACK = 3

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

        private const val PIVOT_Y_TOP = 0f
        private const val PIVOT_X_CENTER_DIVISOR = 2f
    }

    private var currentOffset12Px: Int = dpToPx(BASE_OFFSET_12_DP)
    private var currentOffset23Px: Int = dpToPx(BASE_OFFSET_23_DP)

    private var animateNextLayout: Boolean = false

    private val slots: CardStackSlots = CardStackSlots(context = context, parent = this)

    private var visibleCardCount: Int = MAX_VISIBLE_CARDS
    private val deck: CardStackDeck = CardStackDeck()
    private val transforms: CardStackTransforms by lazy {
        CardStackTransforms(
            container = this,
            config = CardStackTransforms.Config(
                indexGhost = INDEX_GHOST,
                indexBack = INDEX_BACK,
                indexMiddle = INDEX_MIDDLE,
                indexFront = INDEX_FRONT,
                scaleGhost = SCALE_GHOST,
                scaleBack = SCALE_BACK,
                scaleMiddle = SCALE_MIDDLE,
                scaleFront = SCALE_FRONT,
                alphaBack = ALPHA_BACK,
                alphaMiddle = ALPHA_MIDDLE,
                alphaFront = ALPHA_FRONT,
                alphaDefault = ALPHA_DEFAULT,
                alphaHidden = ALPHA_HIDDEN,
                stackTransformDurationMs = STACK_TRANSFORM_DURATION_MS,
                pivotYTop = PIVOT_Y_TOP,
                pivotXCenterDivisor = PIVOT_X_CENTER_DIVISOR,
            ),
        )
    }

    private val animator: CardStackAnimator by lazy {
        CardStackAnimator(
            container = this,
            slots = slots,
            deck = deck,
            config = CardStackAnimator.Config(
                maxVisibleCards = MAX_VISIBLE_CARDS,
                minCardsForMiddle = MIN_CARDS_FOR_MIDDLE,
                minCardsForBack = MIN_CARDS_FOR_BACK,
                baseOffset12Dp = BASE_OFFSET_12_DP,
                baseOffset23Dp = BASE_OFFSET_23_DP,
                collapsedOffset12Dp = COLLAPSED_OFFSET_12_DP,
                collapsedOffset23Dp = COLLAPSED_OFFSET_23_DP,
                scaleGhost = SCALE_GHOST,
                alphaBack = ALPHA_BACK,
                alphaMiddle = ALPHA_MIDDLE,
                alphaFront = ALPHA_FRONT,
                animation3DurationMs = ANIMATION_3_DURATION_MS,
                cardChangeDropDurationMs = CARD_CHANGE_DROP_DURATION_MS,
                cardChangeRiseDurationMs = CARD_CHANGE_RISE_DURATION_MS,
                compressAnimationDurationMs = COMPRESS_ANIMATION_DURATION_MS,
                overshootTension = OVERSHOOT_TENSION,
                cubicBezierX1 = CUBIC_BEZIER_X1,
                cubicBezierY1 = CUBIC_BEZIER_Y1,
                cubicBezierX2 = CUBIC_BEZIER_X2,
                cubicBezierY2 = CUBIC_BEZIER_Y2,
                cardDropExtraDp = CARD_DROP_EXTRA_DP,
            ),
            getVisibleCardCount = { visibleCardCount },
            getCurrentOffset12Px = { currentOffset12Px },
            getCurrentOffset23Px = { currentOffset23Px },
            setCurrentOffsetsPx = { offset12Px, offset23Px ->
                currentOffset12Px = offset12Px
                currentOffset23Px = offset23Px
            },
            applyStackTransforms = { shouldAnimate ->
                transforms.apply(
                    shouldAnimate = shouldAnimate,
                    isFocusAnimationRunning = animator.isFocusAnimationRunning,
                    currentOffset12Px = currentOffset12Px,
                    currentOffset23Px = currentOffset23Px,
                )
            },
            setImageUrls = { urls, animateLayout -> setImageUrls(urls, animateLayout) },
            rotateRolesAfterScroll = { rotateRolesAfterScroll() },
            dpToPx = { dp -> dpToPx(dp) },
            dpToPxF = { dp -> dpToPxF(dp) },
        )
    }

    init {
        applyZOrder()
        slots.configureInitialRoles()

        // Initially ничего не показываем до прихода данных.
        updateVisibleCardsForCount(MIN_VISIBLE_CARDS, animateLayout = false)
    }

    fun setImageUrls(urls: List<String>, animateLayout: Boolean = true) {
        deck.setUrls(urls)

        val limited = deck.visibleUrls(MAX_VISIBLE_CARDS)

        visibleCardCount = limited.size.coerceIn(MIN_VISIBLE_CARDS, MAX_VISIBLE_CARDS)
        updateVisibleCardsForCount(visibleCardCount, animateLayout = animateLayout)

        deck.bindVisibleUrlsToSlots(slots, maxVisibleCards = MAX_VISIBLE_CARDS)
    }

    fun animateCardChange(newUrls: List<String>) {
        animator.animateCardChange(newUrls)
    }

    fun animateCompress() {
        animator.animateCompress()
    }

    fun animateFocusChange() {
        animator.animateFocusChange()
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
                val offset = transforms.topOffsetForIndex(i, currentOffset12Px, currentOffset23Px)
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

        transforms.apply(
            shouldAnimate = shouldAnimate,
            isFocusAnimationRunning = animator.isFocusAnimationRunning,
            currentOffset12Px = currentOffset12Px,
            currentOffset23Px = currentOffset23Px,
        )
    }

    private fun updateVisibleCardsForCount(count: Int, animateLayout: Boolean) {
        val clamped = count.coerceIn(MIN_VISIBLE_CARDS, MAX_VISIBLE_CARDS)
        visibleCardCount = clamped

        val showBack = clamped >= MIN_CARDS_FOR_BACK
        val showMiddle = clamped >= MIN_CARDS_FOR_MIDDLE
        val showFront = clamped >= MIN_CARDS_FOR_FRONT

        val ghostRoot = slots.ghostRoot
        val backRoot = slots.backRoot
        val middleRoot = slots.middleRoot
        val frontRoot = slots.frontRoot

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

    private fun applyZOrder() {
        val zStep = dpToPxF(Z_ORDER_STEP_DP)
        slots.applyZOrder(zStepPx = zStep)
    }

    private fun rotateRolesAfterScroll() {
        slots.rotateAfterScroll()
        applyZOrder()

        deck.rotateLeft()
        slots.configureInitialRoles()
        updateVisibleCardsForCount(visibleCardCount, animateLayout = false)
        deck.prepareGhostForIncomingBack(slots)
        transforms.apply(
            shouldAnimate = false,
            isFocusAnimationRunning = animator.isFocusAnimationRunning,
            currentOffset12Px = currentOffset12Px,
            currentOffset23Px = currentOffset23Px,
        )
        requestLayout()
        invalidate()
    }
}
