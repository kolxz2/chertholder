package ru.kolxz2.chertholder

import android.os.Bundle
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

//        val imageUrls = listOf(
//            // Use direct image URLs.
//            // First URL should appear on the front (top) card.
//            "https://vet-centre.by/wp-content/uploads/2016/11/kot-s-myshyu-eti-udivitelnye-kotiki.jpg",
//            "https://vet-centre.by/wp-content/uploads/2016/11/kot-lezhit-na-spine-eti-udivitelnye-kotiki.jpg",
//            "https://vet-centre.by/wp-content/uploads/2016/11/kot-v-trave-eti-udivitelnye-kotiki.jpg",
//            "https://vet-centre.by/wp-content/uploads/2016/11/kot-v-luchah-eti-udivitelnye-kotiki.jpg"
//        )

//        val replacementUrls = listOf(
//            "https://storage-api.petstory.ru/resize/0x0x100/07/e2/bb/07e2bb0a343f4874979064b4e4066d96.jpeg",
//            "https://storage-api.petstory.ru/resize/0x0x100/14/09/a3/1409a33b47794d7eb21d05b6120856c8.jpeg",
//            "https://storage-api.petstory.ru/resize/0x0x100/64/fa/44/64fa440130a54c849ab1742035ae0a39.jpeg",
//            "https://www.marimedia.ru/media/upload/0aab62b4cbed784459680008078099fc.jpg"
//        )
        val replacementUrls = listOf(
            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-lezhit-na-spine-eti-udivitelnye-kotiki.jpg","https://s694vla.storage.yandex.net/rdisk/a1a0299978778848ca7a88fe121c0ab166f4ef674d8dd72f4aed17fad32eafb0/6985e30a/4RNXykgZPLS6_4q17tic3M22cS1v1vuR8LB6foiy1MQi-qlCSNySbmKW4No4BoGcvOkCGdijNjIkhhgVlVQM0Q==?uid=0&filename=Group%2018620%202.png&disposition=inline&hash=&limit=0&content_type=image%2Fpng&owner_uid=0&fsize=22521&hid=e971fe012c269e44df8fd906b8f15294&media_type=image&tknv=v3&etag=725e860d545a56902cb2c2bbbe4d9ada&ts=64a2733975680&s=d382c5210ba47b1ab6b211c28339ce443fcaf3bf271f800f3af8fe02eca66e11&pb=U2FsdGVkX18bAbwXp8igsFAyll2ypGFO53uL2eaOFUR7fstxUBQwZKdaAuJARnB_-wehRmiiDzLkYYYS1cPPlCwT9JzVpkfOwMgkpc2CWHk"),
            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-v-trave-eti-udivitelnye-kotiki.jpg", "https://s694vla.storage.yandex.net/rdisk/a1a0299978778848ca7a88fe121c0ab166f4ef674d8dd72f4aed17fad32eafb0/6985e30a/4RNXykgZPLS6_4q17tic3M22cS1v1vuR8LB6foiy1MQi-qlCSNySbmKW4No4BoGcvOkCGdijNjIkhhgVlVQM0Q==?uid=0&filename=Group%2018620%202.png&disposition=inline&hash=&limit=0&content_type=image%2Fpng&owner_uid=0&fsize=22521&hid=e971fe012c269e44df8fd906b8f15294&media_type=image&tknv=v3&etag=725e860d545a56902cb2c2bbbe4d9ada&ts=64a2733975680&s=d382c5210ba47b1ab6b211c28339ce443fcaf3bf271f800f3af8fe02eca66e11&pb=U2FsdGVkX18bAbwXp8igsFAyll2ypGFO53uL2eaOFUR7fstxUBQwZKdaAuJARnB_-wehRmiiDzLkYYYS1cPPlCwT9JzVpkfOwMgkpc2CWHk"),
            CardholderLayout.CardSource.UrlSource("https://vet-centre.by/wp-content/uploads/2016/11/kot-v-luchah-eti-udivitelnye-kotiki.jpg", "https://s694vla.storage.yandex.net/rdisk/a1a0299978778848ca7a88fe121c0ab166f4ef674d8dd72f4aed17fad32eafb0/6985e30a/4RNXykgZPLS6_4q17tic3M22cS1v1vuR8LB6foiy1MQi-qlCSNySbmKW4No4BoGcvOkCGdijNjIkhhgVlVQM0Q==?uid=0&filename=Group%2018620%202.png&disposition=inline&hash=&limit=0&content_type=image%2Fpng&owner_uid=0&fsize=22521&hid=e971fe012c269e44df8fd906b8f15294&media_type=image&tknv=v3&etag=725e860d545a56902cb2c2bbbe4d9ada&ts=64a2733975680&s=d382c5210ba47b1ab6b211c28339ce443fcaf3bf271f800f3af8fe02eca66e11&pb=U2FsdGVkX18bAbwXp8igsFAyll2ypGFO53uL2eaOFUR7fstxUBQwZKdaAuJARnB_-wehRmiiDzLkYYYS1cPPlCwT9JzVpkfOwMgkpc2CWHk")
        )

        val imageUrls = listOf(
            CardholderLayout.CardSource.UrlSource("https://storage-api.petstory.ru/resize/0x0x100/07/e2/bb/07e2bb0a343f4874979064b4e4066d96.jpeg"),
            CardholderLayout.CardSource.UrlSource("https://storage-api.petstorpeg"),
            CardholderLayout.CardSource.UrlSource("https://storage-api.petstory.ru/resize/0x0x100/64/fa/44/64fa440130a54c849ab1742035ae0a39.jpeg")
        )

        // Track what data set is currently shown in the stack.
        // Starts with imageUrls because adapter binds that initially.
        var isShowingReplacementUrls = false

        binding.cardStackLayout.setCardsState(imageUrls, defaultUrl = "https://bipbap.ru/wp-content/uploads/2017/04/72fqw2qq3kxh.jpg")
//
//        val adapter = RecyclerAdapter(imageUrls)
//        binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        binding.recyclerView.adapter = adapter

        binding.btnAnim1.setOnClickListener {
            val stack = binding.cardStackLayout
            val nextUrls = if (isShowingReplacementUrls) imageUrls else replacementUrls
            if (stack != null) {
                stack.setCardsState(nextUrls)
                isShowingReplacementUrls = !isShowingReplacementUrls
            }
        }

        binding.btnAnim2.setOnClickListener {
            val stack = binding.cardStackLayout
            stack.setCardsState(isCollapsed = isShowingReplacementUrls)
            isShowingReplacementUrls = !isShowingReplacementUrls

        }
//
//        binding.btnAnim3.setOnClickListener {
//            val stack = binding.cardStackLayout
//            stack?.animateFocusChange()
//        }
    }
}