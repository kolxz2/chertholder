package ru.kolxz2.chertholder.protaatipe

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.animation.doOnStart
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.kolxz2.chertholder.databinding.MainPageFocusAccountCardBinding

internal class CardholderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var cardStateFactory: CardStateFactory = DefaultCardStateFactory()

    private var cardSources: List<CardSource> = emptyList()

    private var isCollapsed: Boolean = false

    private val children: List<MainPageFocusAccountCardBinding> = buildList {
        val inflater = LayoutInflater.from(context)
        repeat(TOTAL_CARD_COUNT) { add(MainPageFocusAccountCardBinding.inflate(inflater, this@CardholderLayout, true)) }
    }

    private var animator: Animator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(child.measuredWidth, MeasureSpec.EXACTLY)
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec((child.measuredHeight * DEFAULT_CARD_RATIO).toInt(), MeasureSpec.EXACTLY)
            child.measure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyChange(
            cards = cardSources,
            collapsed = isCollapsed,
            animate = false,
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    fun setCards(cardSources: List<CardSource>, isCollapsed: Boolean, isAnimate: Boolean) {
        applyChange(
            cards = cardSources,
            collapsed = isCollapsed,
            previousCard = this.cardSources,
            previousCollapsed = this.isCollapsed,
            animate = isAnimate,
        )
        this.cardSources = cardSources
        this.isCollapsed = isCollapsed
    }

    private fun applyChange(
        cards: List<CardSource>,
        collapsed: Boolean,
        previousCard: List<CardSource> = cards,
        previousCollapsed: Boolean = collapsed,
        animate: Boolean,
    ) {
        val actualPreviousCards = previousCard.take(childCount)
        val actualCards = cards.take(childCount)
        animator?.cancel()
        animator = when {
            !animate -> {
                // Apply state immediately
                children.forEachIndexed { index, child ->
                    val initial = cardStateFactory.getVisibleCardState(this, index, actualCards.size, isCollapsed)
                    child.applyCardState(initial)
                    child.load(cardSources.getOrNull(index))
                }
                null
            }
            actualPreviousCards != actualCards -> {
                // Animate hidden state
                val hidden = animateTo { _, index ->
                    cardStateFactory.getHiddenCardState(holder = this, index = index, count = actualPreviousCards.size, isCollapsed = collapsed)
                }
                // Animate change state
                val change = animateTo({ _, index ->
                    cardStateFactory.getHiddenCardState(holder = this, index = index, count = actualPreviousCards.size, isCollapsed = collapsed)
                }, { _, index ->
                    cardStateFactory.getHiddenCardState(holder = this, index = index, count = actualCards.size, isCollapsed = collapsed)
                })
                // Animate visible state
                val visible = animateTo({ _, index ->
                    cardStateFactory.getHiddenCardState(holder = this, index = index, count = actualCards.size, isCollapsed = collapsed)
                }, { _, index ->
                    cardStateFactory.getVisibleCardState(holder = this, index = index, count = actualCards.size, isCollapsed = collapsed)
                }).apply {
                    doOnStart { children.load(actualCards) }
                }
                if (previousCard.isEmpty()) {
                    // Only visible animation
                    visible
                } else {
                    // Hidden -> Change -> Visible
                    AnimatorSet().apply { playSequentially(hidden, change, visible) }
                }
            }
            previousCollapsed != collapsed -> {
                // Animate collapse state
                animateTo { _, index ->
                    cardStateFactory.getVisibleCardState(holder = this, index = index, count = actualCards.size, isCollapsed = collapsed)
                }
            }
            else -> null
        }
        animator?.duration = DEFAULT_CARD_DURATION
        animator?.start()
    }

    // Return animator between start and end card state
    private fun animateTo(
        start: (MainPageFocusAccountCardBinding, Int) -> CardState = { child, _ -> child.getCardState() },
        end: (MainPageFocusAccountCardBinding, Int) -> CardState,
    ): Animator {
        val transitions = children.mapIndexed { index, child -> Triple(child, start(child, index), end(child, index)) }
        return ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                for ((child, start, end) in transitions) {
                    child.applyCardState(start, end, it.animatedValue as Float)
                }
            }
        }
    }

    // Return current state snapshot
    private fun MainPageFocusAccountCardBinding.getCardState(): CardState {
        return CardState(
            translationX = root.translationX,
            translationY = root.translationY,
            translationZ = root.translationZ,
            scaleX = root.scaleX,
            scaleY = root.scaleY,
            alpha = cardImage.alpha,
        )
    }

    // Apply new card state to card view
    private fun MainPageFocusAccountCardBinding.applyCardState(cardState: CardState) {
        root.translationX = cardState.translationX
        root.translationY = cardState.translationY
        root.translationZ = cardState.translationZ
        root.scaleX = cardState.scaleX
        root.scaleY = cardState.scaleY
        root.pivotX = width / 2f
        root.pivotY = 0f
        cardImage.alpha = cardState.alpha
    }

    // Apply new card state by fraction
    private fun MainPageFocusAccountCardBinding.applyCardState(start: CardState, end: CardState, fraction: Float) {
        root.translationX = start.translationX + (end.translationX - start.translationX) * fraction
        root.translationY = start.translationY + (end.translationY - start.translationY) * fraction
        root.translationZ = start.translationZ + (end.translationZ - start.translationZ) * fraction
        root.scaleX = start.scaleX + (end.scaleX - start.scaleX) * fraction
        root.scaleY = start.scaleY + (end.scaleY - start.scaleY) * fraction
        root.pivotX = start.alpha + (end.alpha - start.alpha) * fraction
    }

    private fun MainPageFocusAccountCardBinding.load(cardSource: CardSource?) {
        when (cardSource) {
            is CardSource.UrlSource -> Glide
                .with(cardImage)
                .load(cardSource.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(true)
                .into(cardImage)
            is CardSource.DrawableSource -> Glide
                .with(cardImage)
                .load(cardSource.icon)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(cardImage)
            else -> Glide
                .with(cardImage)
                .clear(cardImage)
                .also { cardImage.setImageDrawable(null) }
        }
    }

    private fun List<MainPageFocusAccountCardBinding>.load(cards: List<CardSource>) {
        forEachIndexed { index, child -> child.load(cards.getOrNull(index)) }
    }

    sealed interface CardSource {

        data class UrlSource(val url: String) : CardSource

        data class DrawableSource(val icon: Drawable) : CardSource
    }

    data class CardState(
        val translationX: Float = 0f,
        val translationY: Float = 0f,
        val translationZ: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val alpha: Float = 1f,
    )

    interface CardStateFactory {

        fun getVisibleCardState(holder: CardholderLayout, index: Int, count: Int, isCollapsed: Boolean): CardState

        fun getHiddenCardState(holder: CardholderLayout, index: Int, count: Int, isCollapsed: Boolean): CardState
    }

    private companion object {
        private const val TOTAL_CARD_COUNT = 3
        private const val DEFAULT_CARD_RATIO = 0.875f
        private const val DEFAULT_CARD_DURATION = 300L
    }
}