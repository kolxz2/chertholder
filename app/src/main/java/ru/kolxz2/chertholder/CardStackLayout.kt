package ru.kolxz2.chertholder

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.kolxz2.chertholder.databinding.ItemRecyclerBinding

class CardStackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // Spacing requirements:
    // - between front(1) and middle(2): 10dp
    // - between middle(2) and back(3): 7dp
    //
    // We layout from back to front:
    // back(3): 0
    // middle(2): 7dp
    // front(1): 7dp + 10dp = 17dp
    private val offset12Px = dpToPx(10f)
    private val offset23Px = dpToPx(7f)
    private val frontTopOffsetPx = offset23Px + offset12Px

    /**
     * Children order is important:
     * - index 0: back card (peeks the most)
     * - index 1: middle card
     * - index 2: front card (drawn last, on top)
     */
    private val bindings: List<ItemRecyclerBinding> = buildList(3) {
        val inflater = LayoutInflater.from(context)
        repeat(3) {
            add(ItemRecyclerBinding.inflate(inflater, this@CardStackLayout, true))
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
        bindings.getOrNull(0)?.cardView?.translationZ = 0f
        bindings.getOrNull(1)?.cardView?.translationZ = zStep
        bindings.getOrNull(2)?.cardView?.translationZ = 2f * zStep

        // Default: show all 3 cards.
        setCardCount(visibleCardCount)
    }

    /**
     * Set how many cards to show (1..3).
     * 1 -> only front card
     * 2 -> middle + front
     * 3 -> back + middle + front
     */
    fun setCardCount(count: Int) {
        val clamped = count.coerceIn(1, 3)
        visibleCardCount = clamped

        // child index: 0=back, 1=middle, 2=front
        val showBack = visibleCardCount >= 3
        val showMiddle = visibleCardCount >= 2
        val showFront = true

        bindings.getOrNull(0)?.root?.visibility = if (showBack) View.VISIBLE else View.GONE
        bindings.getOrNull(1)?.root?.visibility = if (showMiddle) View.VISIBLE else View.GONE
        bindings.getOrNull(2)?.root?.visibility = if (showFront) View.VISIBLE else View.GONE

        requestLayout()
        invalidate()
    }

    fun getCardCount(): Int = visibleCardCount

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

            val offset = topOffsetForIndex(i)
            minOffset = minOf(minOffset, offset)
            maxOffset = maxOf(maxOffset, offset)
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
        var minOffset = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            minOffset = minOf(minOffset, topOffsetForIndex(i))
        }
        if (minOffset == Int.MAX_VALUE) minOffset = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val lp = child.layoutParams as MarginLayoutParams

            // Shift so that the top-most visible card starts at 0.
            val topOffset = topOffsetForIndex(i) - minOffset

            val left = paddingLeft + lp.leftMargin
            val top = paddingTop + topOffset + lp.topMargin
            val right = left + child.measuredWidth
            val bottom = top + child.measuredHeight

            child.layout(left, top, right, bottom)
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

    private fun topOffsetForIndex(childIndex: Int): Int {
        return when (childIndex) {
            0 -> 0
            1 -> offset23Px
            else -> frontTopOffsetPx
        }
    }
}

