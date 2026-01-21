package ru.kolxz2.chertholder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.kolxz2.chertholder.databinding.MainPageFocusAccountCardBinding

/**
 * Owns 4 card bindings (ghost/back/middle/front) and their ordering in the parent ViewGroup.
 *
 * Important invariant: the child index in parent equals slot index (INDEX_* constants).
 */
internal class CardStackSlots(
    context: Context,
    private val parent: ViewGroup,
) {
    internal companion object {
        internal const val INDEX_GHOST = 0
        internal const val INDEX_BACK = 1
        internal const val INDEX_MIDDLE = 2
        internal const val INDEX_FRONT = 3

        internal const val TOTAL_CARD_COUNT = 4
    }

    private val bindings: MutableList<MainPageFocusAccountCardBinding> = buildList(TOTAL_CARD_COUNT) {
        val inflater = LayoutInflater.from(context)
        repeat(TOTAL_CARD_COUNT) {
            add(MainPageFocusAccountCardBinding.inflate(inflater, parent, true))
        }
    }.toMutableList()

    fun bindingAt(index: Int): MainPageFocusAccountCardBinding? = bindings.getOrNull(index)

    val ghostBinding: MainPageFocusAccountCardBinding?
        get() = bindingAt(INDEX_GHOST)
    val backBinding: MainPageFocusAccountCardBinding?
        get() = bindingAt(INDEX_BACK)
    val middleBinding: MainPageFocusAccountCardBinding?
        get() = bindingAt(INDEX_MIDDLE)
    val frontBinding: MainPageFocusAccountCardBinding?
        get() = bindingAt(INDEX_FRONT)

    val ghostRoot: View?
        get() = ghostBinding?.root
    val backRoot: View?
        get() = backBinding?.root
    val middleRoot: View?
        get() = middleBinding?.root
    val frontRoot: View?
        get() = frontBinding?.root

    fun forEachBinding(block: (MainPageFocusAccountCardBinding) -> Unit) {
        for (b in bindings) block(b)
    }

    fun applyZOrder(zStepPx: Float) {
        bindings.getOrNull(INDEX_GHOST)?.root?.translationZ = INDEX_GHOST * zStepPx
        bindings.getOrNull(INDEX_BACK)?.root?.translationZ = INDEX_BACK * zStepPx
        bindings.getOrNull(INDEX_MIDDLE)?.root?.translationZ = INDEX_MIDDLE * zStepPx
        bindings.getOrNull(INDEX_FRONT)?.root?.translationZ = INDEX_FRONT * zStepPx
    }

    fun configureInitialRoles() {
        ghostBinding?.let(::configureAsGhost)
        backBinding?.let(::configureAsCard)
        middleBinding?.let(::configureAsCard)
        frontBinding?.let(::configureAsCard)
    }

    fun configureAsGhost(binding: MainPageFocusAccountCardBinding) {
        binding.root.isClickable = false
        binding.root.isFocusable = false
        binding.root.isFocusableInTouchMode = false
        binding.root.isEnabled = false
        binding.root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
    }

    fun configureAsCard(binding: MainPageFocusAccountCardBinding) {
        binding.root.isEnabled = true
        binding.root.isClickable = false
        binding.root.isFocusable = false
        binding.root.isFocusableInTouchMode = false
        binding.root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    }

    /**
     * Rotates bindings and reorders children in the parent:
     * oldFront -> newGhost, oldGhost -> newBack, oldBack -> newMiddle, oldMiddle -> newFront.
     */
    fun rotateAfterScroll() {
        val oldGhost = bindings.getOrNull(INDEX_GHOST)
        val oldBack = bindings.getOrNull(INDEX_BACK)
        val oldMiddle = bindings.getOrNull(INDEX_MIDDLE)
        val oldFront = bindings.getOrNull(INDEX_FRONT)

        if (oldGhost == null || oldBack == null || oldMiddle == null || oldFront == null) return

        bindings.clear()
        bindings.add(oldFront)
        bindings.add(oldGhost)
        bindings.add(oldBack)
        bindings.add(oldMiddle)

        parent.removeAllViews()
        parent.addView(oldFront.root)
        parent.addView(oldGhost.root)
        parent.addView(oldBack.root)
        parent.addView(oldMiddle.root)
    }
}

