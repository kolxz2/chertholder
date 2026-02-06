package ru.kolxz2.chertholder.protaatipe

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.kolxz2.chertholder.R
import ru.kolxz2.chertholder.databinding.MainPageFocusAccountCardBinding

internal class CardholderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var cardStateFactory: CardStateFactory = DefaultCardStateFactory()

    private var cardSources: List<CardSource> = emptyList()

    private var defaultFallbackUrl: String? = null

    private var isCollapsed: Boolean = false

    private val children: List<MainPageFocusAccountCardBinding> = buildList {
        val inflater = LayoutInflater.from(context)
        repeat(TOTAL_CARD_COUNT) {
            add(
                MainPageFocusAccountCardBinding.inflate(
                    inflater,
                    this@CardholderLayout,
                    true
                )
            )
        }
    }

    private var animator: Animator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidthSpec =
                MeasureSpec.makeMeasureSpec(child.measuredWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(
                (child.measuredHeight * DEFAULT_CARD_RATIO).toInt(),
                MeasureSpec.EXACTLY
            )
            child.measure(childWidthSpec, childHeightSpec)
            child.pivotX = child.measuredWidth / 2f
            child.pivotY = 0f
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

    fun setCardsState(
        cardSources: List<CardSource> = this.cardSources,
        isCollapsed: Boolean = this.isCollapsed,
        isAnimate: Boolean = true,
        defaultUrl: String? = this.defaultFallbackUrl,
    ) {
        this.defaultFallbackUrl = defaultUrl
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
                applyWithoutAnimation(actualCards, collapsed)
                null
            }

            actualPreviousCards != actualCards -> {
                createCardsChangeAnimator(
                    previousCards = actualPreviousCards,
                    newCards = actualCards,
                    isCollapsed = collapsed,
                    hasPreviousCards = previousCard.isNotEmpty(),
                )
            }

            previousCollapsed != collapsed -> {
                createCollapseAnimator(actualCards, collapsed)
            }

            else -> null
        }
        animator?.duration = DEFAULT_CARD_DURATION
        animator?.start()
    }

    private fun animateTo(
        start: (MainPageFocusAccountCardBinding, Int) -> CardState = { child, _ -> child.getCardState() },
        end: (MainPageFocusAccountCardBinding, Int) -> CardState,
    ): ValueAnimator {
        val transitions = children.mapIndexed { index, child ->
            Triple(
                child,
                start(child, index),
                end(child, index)
            )
        }
        return ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                for ((child, start, end) in transitions) {
                    child.applyCardState(start, end, it.animatedValue as Float)
                }
            }
        }
    }

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

    private fun MainPageFocusAccountCardBinding.applyCardState(cardState: CardState) {
        root.translationX = cardState.translationX
        root.translationY = cardState.translationY
        root.translationZ = cardState.translationZ
        root.scaleX = cardState.scaleX
        root.scaleY = cardState.scaleY
        cardImage.alpha = cardState.alpha
    }

    private fun MainPageFocusAccountCardBinding.applyCardState(
        start: CardState,
        end: CardState,
        fraction: Float,
    ) {
        root.translationX = start.translationX + (end.translationX - start.translationX) * fraction
        root.translationY = start.translationY + (end.translationY - start.translationY) * fraction
        root.translationZ = start.translationZ + (end.translationZ - start.translationZ) * fraction
        root.scaleX = start.scaleX + (end.scaleX - start.scaleX) * fraction
        root.scaleY = start.scaleY + (end.scaleY - start.scaleY) * fraction
        cardImage.alpha = start.alpha + (end.alpha - start.alpha) * fraction
    }

    private fun applyWithoutAnimation(
        cards: List<CardSource>,
        collapsed: Boolean,
    ) {
        children.forEachIndexed { index, child ->
            val initial = cardStateFactory.getVisibleCardState(
                holder = this,
                index = index,
                count = cards.size,
                isCollapsed = collapsed,
            )
            child.applyCardState(initial)
            child.load(cards.getOrNull(index))
        }
    }

    private fun createCardsChangeAnimator(
        previousCards: List<CardSource>,
        newCards: List<CardSource>,
        isCollapsed: Boolean,
        hasPreviousCards: Boolean,
    ): Animator {
        val hideAnimator = animateTo { _, index ->
            cardStateFactory.getHiddenCardState(
                holder = this,
                index = index,
                count = previousCards.size,
                isCollapsed = isCollapsed,
            )
        }

        val switchAnimator = animateTo(
            start = { _, index ->
                cardStateFactory.getHiddenCardState(
                    holder = this,
                    index = index,
                    count = previousCards.size,
                    isCollapsed = isCollapsed,
                )
            },
            end = { _, index ->
                cardStateFactory.getHiddenCardState(
                    holder = this,
                    index = index,
                    count = newCards.size,
                    isCollapsed = isCollapsed,
                )
            },
        )

        val showAnimator = animateTo(
            start = { _, index ->
                cardStateFactory.getHiddenCardState(
                    holder = this,
                    index = index,
                    count = newCards.size,
                    isCollapsed = isCollapsed,
                )
            },
            end = { _, index ->
                cardStateFactory.getVisibleCardState(
                    holder = this,
                    index = index,
                    count = newCards.size,
                    isCollapsed = isCollapsed,
                )
            },
        ).apply {
            doOnStart { children.load(newCards) }
        }

        return if (!hasPreviousCards) {
            showAnimator
        } else {
            AnimatorSet().apply { playSequentially(hideAnimator, switchAnimator, showAnimator) }
        }
    }

    private fun createCollapseAnimator(
        cards: List<CardSource>,
        collapsed: Boolean,
    ): Animator {
        return animateTo { _, index ->
            cardStateFactory.getVisibleCardState(
                holder = this,
                index = index,
                count = cards.size,
                isCollapsed = collapsed,
            )
        }.apply {
            interpolator =
                AnimationUtils.loadInterpolator(context, R.anim.card_compress_interpolator)
        }
    }

    private fun MainPageFocusAccountCardBinding.load(cardSource: CardSource?) {
        val placeholder =
            ContextCompat.getDrawable(cardImage.context, R.drawable.placeholder_card_image)
        when (cardSource) {
            is CardSource.UrlSource -> {
                val fallbackUrl = defaultFallbackUrl
                Glide.with(cardImage)
                    .load(cardSource.url)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(placeholder)
                    .let { request ->
                        if (fallbackUrl != null) {
                            request.error(
                                Glide.with(cardImage)
                                    .load(fallbackUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .placeholder(placeholder),
                            )
                        } else request
                    }
                    .into(cardImage)
            }

            is CardSource.DrawableSource -> Glide
                .with(cardImage)
                .load(cardSource.icon)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(placeholder)
                .into(cardImage)

            else -> Glide
                .with(cardImage)
                .clear(cardImage)
                .also { cardImage.setImageDrawable(null) }
        }
        val overlayUrl = when (cardSource) {
            is CardSource.UrlSource -> cardSource.overlayUrl
            is CardSource.DrawableSource -> cardSource.overlayUrl
            else -> null
        }
        if (overlayUrl != null) {
            cardOverlay.visibility = View.VISIBLE
            Glide.with(cardOverlay)
                .load(overlayUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(cardOverlay)
        } else {
            cardOverlay.visibility = View.GONE
            Glide.with(cardOverlay).clear(cardOverlay)
        }
    }

    private fun List<MainPageFocusAccountCardBinding>.load(cards: List<CardSource>) {
        forEachIndexed { index, child -> child.load(cards.getOrNull(index)) }
    }

    sealed interface CardSource {

        data class UrlSource(val url: String, val overlayUrl: String? = null) : CardSource

        data class DrawableSource(val icon: Drawable, val overlayUrl: String? = null) : CardSource
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

        fun getVisibleCardState(
            holder: CardholderLayout,
            index: Int,
            count: Int,
            isCollapsed: Boolean,
        ): CardState

        fun getHiddenCardState(
            holder: CardholderLayout,
            index: Int,
            count: Int,
            isCollapsed: Boolean,
        ): CardState
    }

    private companion object {
        private const val TOTAL_CARD_COUNT = 3
        private const val DEFAULT_CARD_RATIO = 0.875f
        private const val DEFAULT_CARD_DURATION = 300L
    }
}