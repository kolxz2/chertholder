package ru.kolxz2.chertholder

import android.view.View
import coil.load

/**
 * Holds the full deck of URLs and knows how to bind the top cards into the stack slots.
 */
internal class CardStackDeck {

    private companion object {
        private const val DECK_URL_NEXT_CANDIDATE_INDEX = 3
        private const val DECK_URL_FALLBACK_INDEX = 0
        private const val MIN_DECK_SIZE_FOR_ROTATION = 1
    }

    private val deckUrls: MutableList<String> = mutableListOf()

    fun setUrls(urls: List<String>) {
        deckUrls.clear()
        deckUrls.addAll(urls)
    }

    fun visibleUrls(maxVisibleCards: Int): List<String> = deckUrls.take(maxVisibleCards)

    fun rotateLeft() {
        if (deckUrls.size <= MIN_DECK_SIZE_FOR_ROTATION) return
        val first = deckUrls.removeAt(DECK_URL_FALLBACK_INDEX)
        deckUrls.add(first)
    }

    fun incomingBackUrl(): String? {
        if (deckUrls.isEmpty()) return null
        val candidate = deckUrls.getOrNull(DECK_URL_NEXT_CANDIDATE_INDEX)
        if (!candidate.isNullOrBlank()) return candidate
        val fallback = deckUrls.getOrNull(DECK_URL_FALLBACK_INDEX)
        return fallback?.takeIf { it.isNotBlank() }
    }

    fun bindVisibleUrlsToSlots(slots: CardStackSlots, maxVisibleCards: Int) {
        slots.forEachBinding { binding ->
            binding.cardImage.load(null)
            binding.cardImage.visibility = View.GONE
        }

        val limited = deckUrls.take(maxVisibleCards)

        val targetBindingIndices = intArrayOf(
            CardStackSlots.INDEX_FRONT,
            CardStackSlots.INDEX_MIDDLE,
            CardStackSlots.INDEX_BACK,
        )
        for (i in limited.indices) {
            val bindingIndex = targetBindingIndices.getOrNull(i) ?: continue
            val binding = slots.bindingAt(bindingIndex) ?: continue
            val url = limited[i]

            if (url.isBlank()) {
                binding.cardImage.visibility = View.GONE
            } else {
                binding.cardImage.visibility = View.VISIBLE
                binding.cardImage.load(url)
            }
        }

        prepareGhostForIncomingBack(slots)
    }

    fun prepareGhostForIncomingBack(slots: CardStackSlots) {
        val ghost = slots.ghostBinding ?: return
        val url = incomingBackUrl()

        if (url.isNullOrBlank()) {
            ghost.cardImage.load(null)
            ghost.cardImage.visibility = View.GONE
        } else {
            ghost.cardImage.visibility = View.VISIBLE
            ghost.cardImage.load(url)
        }

        slots.configureAsGhost(ghost)
    }
}

