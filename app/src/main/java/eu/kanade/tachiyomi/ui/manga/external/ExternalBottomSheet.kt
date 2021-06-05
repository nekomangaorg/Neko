package eu.kanade.tachiyomi.ui.manga.external

import android.os.Bundle
import android.view.LayoutInflater
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import eu.kanade.tachiyomi.databinding.ExternalBottomSheetBinding
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog

class ExternalBottomSheet(private val controller: MangaDetailsController) : E2EBottomSheetDialog<ExternalBottomSheetBinding>(controller.activity!!) {

    override fun createBinding(inflater: LayoutInflater) = ExternalBottomSheetBinding.inflate(inflater)

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
        val itemAdapter = ItemAdapter<ExternalItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)
        binding.externalRecycler.adapter = fastAdapter
        binding.externalRecycler.updatePaddingRelative(
            bottom = controller.activity!!.window.decorView.rootWindowInsets.systemWindowInsetBottom
        )
        itemAdapter.add(controller.presenter.externalLinksList)

        fastAdapter.onClickListener = { _, _, item, _ ->
            controller.openInWebView(item.externalLink.getUrl())
            true
        }
    }
}
