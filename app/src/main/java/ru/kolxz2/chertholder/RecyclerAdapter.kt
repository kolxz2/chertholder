package ru.kolxz2.chertholder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.kolxz2.chertholder.databinding.ActivityMainBinding
import ru.kolxz2.chertholder.protaatipe.CardholderLayout


internal class RecyclerAdapter(
    private var imageUrls: List<CardholderLayout.CardSource>,
    private val binding: ActivityMainBinding,
) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>() {

    private var boundHolder: RecyclerViewHolder? = null

    class RecyclerViewHolder(
        val cardholderLayout: CardholderLayout,
    ) : RecyclerView.ViewHolder(cardholderLayout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = CardholderLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return RecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        boundHolder = holder
        holder.cardholderLayout.onAllImagesLoaded = {
            binding.shimmerStubInclude.root.visibility = View.GONE
        }
        holder.cardholderLayout.setCardsState(imageUrls)
    }

    override fun onViewRecycled(holder: RecyclerViewHolder) {
        super.onViewRecycled(holder)
        if (boundHolder == holder) {
            boundHolder = null
        }
    }

    override fun getItemCount(): Int = 1

    fun setCardsState(
        cardSources: List<CardholderLayout.CardSource> = imageUrls,
        isCollapsed: Boolean? = null,
    ) {
        imageUrls = cardSources
        boundHolder?.cardholderLayout?.let { layout ->
            if (isCollapsed != null) {
                layout.setCardsState(cardSources, isCollapsed)
            } else {
                layout.setCardsState(cardSources)
            }
        }
    }
}

