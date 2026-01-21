package ru.kolxz2.chertholder

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(
    private val imageUrls: List<String> = emptyList()
) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(
        val stack: CardholderLayout
    ) : RecyclerView.ViewHolder(stack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        // One RecyclerView item: the custom ViewGroup itself.
        val view = CardholderLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return RecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.stack.setImageUrls(imageUrls)
    }

    override fun getItemCount(): Int = 1
}

