package ru.kolxz2.chertholder

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import ru.kolxz2.chertholder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageUrls = listOf(
            // Use direct image URLs.
            // First URL should appear on the front (top) card.
            "https://vet-centre.by/wp-content/uploads/2016/11/kot-s-myshyu-eti-udivitelnye-kotiki.jpg",
            "https://vet-centre.by/wp-content/uploads/2016/11/kot-lezhit-na-spine-eti-udivitelnye-kotiki.jpg",
            "https://vet-centre.by/wp-content/uploads/2016/11/kot-v-trave-eti-udivitelnye-kotiki.jpg",
            "https://vet-centre.by/wp-content/uploads/2016/11/kot-v-luchah-eti-udivitelnye-kotiki.jpg" // should be ignored (max 3)
        )

        val replacementUrls = listOf(
            "https://storage-api.petstory.ru/resize/0x0x100/07/e2/bb/07e2bb0a343f4874979064b4e4066d96.jpeg",
            "https://storage-api.petstory.ru/resize/0x0x100/14/09/a3/1409a33b47794d7eb21d05b6120856c8.jpeg",
            "https://storage-api.petstory.ru/resize/0x0x100/64/fa/44/64fa440130a54c849ab1742035ae0a39.jpeg"
        )

        // Track what data set is currently shown in the stack.
        // Starts with imageUrls because adapter binds that initially.
        var isShowingReplacementUrls = false

        val adapter = RecyclerAdapter(imageUrls)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnAnim1.setOnClickListener {
            val stack = (binding.recyclerView.findViewHolderForAdapterPosition(0) as? RecyclerAdapter.RecyclerViewHolder)?.stack
            val nextUrls = if (isShowingReplacementUrls) imageUrls else replacementUrls
            if (stack != null) {
                stack.startAnimation1(nextUrls)
                isShowingReplacementUrls = !isShowingReplacementUrls
            }
        }

        binding.btnAnim2.setOnClickListener {
            val stack = (binding.recyclerView.findViewHolderForAdapterPosition(0) as? RecyclerAdapter.RecyclerViewHolder)?.stack
            stack?.startAnimation2()
        }

        binding.btnAnim3.setOnClickListener {
            val stack = (binding.recyclerView.findViewHolderForAdapterPosition(0) as? RecyclerAdapter.RecyclerViewHolder)?.stack
            stack?.startAnimation3()
        }
    }
}