package eu.kanade.tachiyomi.ui.manga.external

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import kotlinx.android.synthetic.main.external_bottom_sheet.*

class ExternalBottomSheet(private val controller: MangaDetailsController) : BottomSheetDialog
    (controller.activity!!, R.style.BottomSheetDialogTheme) {

    val activity = controller.activity!!

    private var sheetBehavior: BottomSheetBehavior<*>

    val presenter = controller.presenter

    init {
        // Use activity theme for this layout
        val view = activity.layoutInflater.inflate(R.layout.external_bottom_sheet, null)
        setContentView(view)

        sheetBehavior = BottomSheetBehavior.from(view.parent as ViewGroup)
        setEdgeToEdge(activity, view)
        val height = activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        sheetBehavior.peekHeight = 380.dpToPx + height

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {}

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.skipCollapsed = true
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemAdapter = ItemAdapter<ExternalItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)
        external_recycler.adapter = fastAdapter
        external_recycler.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        itemAdapter.add(presenter.externalLinksList)

        fastAdapter.onClickListener = { _, _, item, _ ->
            controller.openInWebView(item.externalLink.getUrl())
            true
        }
    }
}
