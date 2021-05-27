package eu.kanade.tachiyomi.ui.manga.external

import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController

class ExternalBottomSheet(private val controller: MangaDetailsController) : BottomSheetDialog
    (controller.activity!!, R.style.BottomSheetDialogTheme) {

    /*val activity = controller.activity!!

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

    */
    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     *//*
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
    }*/
}
