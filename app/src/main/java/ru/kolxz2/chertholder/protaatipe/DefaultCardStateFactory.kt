package ru.kolxz2.chertholder.protaatipe

import android.content.Context
import android.content.res.Resources
import androidx.annotation.Px
import kotlin.math.pow

internal class DefaultCardStateFactory : CardholderLayout.CardStateFactory {

    override fun getVisibleCardState(
        holder: CardholderLayout,
        index: Int,
        count: Int,
        isCollapsed: Boolean,
    ): CardholderLayout.CardState {
        return if (isCollapsed) {
            getCollapsedCardState(holder, index, count)
        } else {
            getExpandedCardState(holder, index, count)
        }
    }

    override fun getHiddenCardState(
        holder: CardholderLayout,
        index: Int,
        count: Int,
        isCollapsed: Boolean,
    ): CardholderLayout.CardState {
        return getVisibleCardState(holder, index, count, isCollapsed).toOutOfBounds(holder)
    }

    private fun getExpandedCardState(
        holder: CardholderLayout,
        index: Int,
        count: Int,
    ): CardholderLayout.CardState = CardholderLayout.CardState(
        translationY = getDefaultTranslationY(holder, index, count),
        translationZ = getDefaultTranslationZ(holder, index, count),
        scaleX = getDefaultScale(index),
        scaleY = getDefaultScale(index),
        alpha = getDefaultAlpha(index, count),
    )

    private fun getCollapsedCardState(
        holder: CardholderLayout,
        index: Int,
        count: Int,
    ): CardholderLayout.CardState = CardholderLayout.CardState(
        translationY = getCollapsedTranslationY(holder, index, count),
        translationZ = getDefaultTranslationZ(holder, index, count),
        scaleX = getDefaultScale(index),
        scaleY = getDefaultScale(index),
        alpha = getDefaultAlpha(index, count),
    )

    private fun CardholderLayout.CardState.toOutOfBounds(holder: CardholderLayout): CardholderLayout.CardState =
        copy(translationY = translationY + holder.height)

    private fun getCollapsedTranslationY(holder: CardholderLayout, index: Int, count: Int): Float {
        return when (count) {
            CARD_COUNT_1 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_1_COLLAPSED_TRANSLATION_Y_1
            )

            CARD_COUNT_2 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_2_COLLAPSED_TRANSLATION_Y_1
            )

            CARD_COUNT_2 if index == CARD_INDEX_2 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_2_COLLAPSED_TRANSLATION_Y_2
            )

            CARD_COUNT_3 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_1
            )

            CARD_COUNT_3 if index == CARD_INDEX_2 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_2
            )

            CARD_COUNT_3 if index == CARD_INDEX_3 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_3
            )

            else -> 0f
        }
    }

    private fun getDefaultTranslationY(holder: CardholderLayout, index: Int, count: Int): Float {
        return when (count) {
            CARD_COUNT_1 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_1_DEFAULT_TRANSLATION_Y_1
            )

            CARD_COUNT_2 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_2_DEFAULT_TRANSLATION_Y_1
            )

            CARD_COUNT_2 if index == CARD_INDEX_2 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_2_DEFAULT_TRANSLATION_Y_2
            )

            CARD_COUNT_3 if index == CARD_INDEX_1 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_MORE_2_DEFAULT_TRANSLATION_Y_1
            )

            CARD_COUNT_3 if index == CARD_INDEX_2 -> holder.context.dpToPxAsFloat(
                CARD_COUNT_MORE_2_DEFAULT_TRANSLATION_Y_2
            )

            else -> 0f
        }
    }

    private fun getDefaultTranslationZ(holder: CardholderLayout, index: Int, count: Int): Float {
        return holder.context.dpToPxAsFloat((count - index).toFloat())
    }

    private fun getDefaultAlpha(index: Int, count: Int): Float {
        return when {
            index >= count -> CARD_TRANSPARENT_ALPHA
            index == CARD_INDEX_1 -> CARD_1_DEFAULT_ALPHA
            index == CARD_INDEX_2 -> CARD_2_DEFAULT_ALPHA
            else -> CARD_3_DEFAULT_ALPHA
        }
    }

    @Px
    private fun Context.dpToPxAsFloat(dp: Float): Float = dpToPxAsFloat(dp, resources)

    @Px
    private fun dpToPxAsFloat(dp: Float, resources: Resources): Float =
        dp * resources.displayMetrics.density

    private fun getDefaultScale(index: Int): Float {
        return CARD_DEFAULT_SCALE.pow(index)
    }

    private companion object {
        const val CARD_COUNT_1_DEFAULT_TRANSLATION_Y_1 = 8f
        const val CARD_COUNT_2_DEFAULT_TRANSLATION_Y_1 = 14f
        const val CARD_COUNT_2_DEFAULT_TRANSLATION_Y_2 = 4f
        const val CARD_COUNT_MORE_2_DEFAULT_TRANSLATION_Y_1 = 18f
        const val CARD_COUNT_MORE_2_DEFAULT_TRANSLATION_Y_2 = 8f

        const val CARD_COUNT_1_COLLAPSED_TRANSLATION_Y_1 = 24f
        const val CARD_COUNT_2_COLLAPSED_TRANSLATION_Y_1 = 32f
        const val CARD_COUNT_2_COLLAPSED_TRANSLATION_Y_2 = 24f
        const val CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_1 = 38f
        const val CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_2 = 30f
        const val CARD_COUNT_MORE_2_COLLAPSED_TRANSLATION_Y_3 = 24f

        const val CARD_DEFAULT_SCALE = 0.8f

        const val CARD_1_DEFAULT_ALPHA = 1f
        const val CARD_2_DEFAULT_ALPHA = 0.85f
        const val CARD_3_DEFAULT_ALPHA = 0.7f
        const val CARD_TRANSPARENT_ALPHA = 0f

        const val CARD_INDEX_1 = 0
        const val CARD_INDEX_2 = 1
        const val CARD_INDEX_3 = 2

        const val CARD_COUNT_1 = 1
        const val CARD_COUNT_2 = 2
        const val CARD_COUNT_3 = 3
    }
}
