package ru.kolxz2.chertholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.kolxz2.chertholder.databinding.ItemRecyclerBinding

class RecyclerAdapter(
    private val items: List<RecyclerItem>
) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(
        private val binding: ItemRecyclerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecyclerItem) {
            binding.imageView.setImageResource(item.imageResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecyclerBinding.inflate(inflater, parent, false)
        return RecyclerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

