package ru.kolxz2.chertholder

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import ru.kolxz2.chertholder.databinding.ActivityMainBinding
import ru.kolxz2.chertholder.protaatipe.CardholderLayout

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

        val replacementUrls = listOf(
            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-lezhit-na-spine-eti-udivitelnye-kotiki.jpg"),
//            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-v-trave-eti-udivitelnye-kotiki.jpg"),
            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-v-luchah-eti-udivitelnye-kotiki.jpg")
        )

        val imageUrls = listOf(
//            CardholderLayout.CardSource.UrlSource("https://storage-api.petstory.ru/resize/0x0x100/07/e2/bb/07e2bb0a343f4874979064b4e4066d96.jpeg"),
//            CardholderLayout.CardSource.UrlSource("https://storage-api.petstory.ru/resize/0x0x100/14/09/a3/1409a33b47794d7eb21d05b6120856c8.jpeg"),
            CardholderLayout.CardSource.UrlSource("https://storage-api.petstory.ru/resize/0x0x100/64/fa/44/64fa440130a54c849ab1742035ae0a39.jpeg")
        )

        var isShowingReplacementUrls = false
        binding.shimmerStubInclude.root.visibility = View.VISIBLE

        val adapter = RecyclerAdapter(imageUrls, binding)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnAnim1.setOnClickListener {
            val nextUrls = if (isShowingReplacementUrls) imageUrls else replacementUrls
            binding.shimmerStubInclude.root.visibility = View.VISIBLE
            adapter.setCardsState(nextUrls)
            isShowingReplacementUrls = !isShowingReplacementUrls
        }

        binding.btnAnim2.setOnClickListener {
            adapter.setCardsState(isCollapsed = isShowingReplacementUrls)
            isShowingReplacementUrls = !isShowingReplacementUrls
        }
    }
}