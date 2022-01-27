package eu.kanade.tachiyomi.ui.manga.external

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import eu.kanade.tachiyomi.databinding.ExternalBottomSheetBinding
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog

class ExternalBottomSheet(private val controller: MangaDetailsController) :
    E2EBottomSheetDialog<ExternalBottomSheetBinding>(controller.activity!!) {

    override fun createBinding(inflater: LayoutInflater) =
        ExternalBottomSheetBinding.inflate(inflater)

    val activity = controller.activity!!
    val itemAdapter = ItemAdapter<ExternalItem>()
    val fastAdapter = FastAdapter.with(itemAdapter)

    init {
        val insets =
            activity.window.decorView.rootWindowInsetsCompat?.getInsets(WindowInsetsCompat.Type.systemBars())
        val height = insets?.bottom ?: 0
        sheetBehavior.peekHeight = 525.dpToPx + height
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true

        binding.externalRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.externalRecycler.adapter = fastAdapter

        itemAdapter.add(controller.presenter.externalLinksList)

        fastAdapter.onClickListener = { _, _, item, _ ->
            controller.openInWebView(item.externalLink.getUrl())
            true
        }
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.externalRecycler.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
    }
}
