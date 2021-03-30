package eu.kanade.tachiyomi.ui.source.browse

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.view.ActionMode
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.SourceFilterSheetBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import uy.kohesive.injekt.injectLazy

class SourceSearchSheet(activity: Activity) :
    BottomSheetDialog(activity, R.style.BottomSheetDialogTheme) {

    /**
     * Preferences helper.
     */
    private val preferences by injectLazy<PreferencesHelper>()

    private var sheetBehavior: BottomSheetBehavior<*>

    private var elevationAnimator: ValueAnimator? = null

    var filterChanged = true

    var isNotElevated = false

    val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter<IFlexible<*>>(null)
        .setDisplayHeadersAtStartUp(true)

    var onSearchClicked = {}

    var onResetClicked = {}

    private val binding = SourceFilterSheetBinding.inflate(activity.layoutInflater)
    init {
        setContentView(binding.root)
        binding.toolbarTitle.text = context.getString(R.string.search_filters)
        binding.searchBtn.setOnClickListener { dismiss() }
        binding.resetBtn.setOnClickListener { onResetClicked() }

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)
        sheetBehavior.skipCollapsed = true
        sheetBehavior.expand()
        setEdgeToEdge(
            activity,
            binding.root,
            50.dpToPx
        )

        binding.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.recycler.clipToPadding = false
        binding.recycler.adapter = adapter
        binding.recycler.setHasFixedSize(true)
        binding.recycler.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)

        // the spinner in the recycler can break the sheet's layout on change
        // this is to reset it back
        binding.sourceFilterSheet.post {
            (binding.sourceFilterSheet.parent as? View)?.fitsSystemWindows = false
            binding.sourceFilterSheet.viewTreeObserver.addOnDrawListener {
                (binding.sourceFilterSheet.parent as? View)?.fitsSystemWindows = false
            }
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {}

                override fun onStateChanged(p0: View, state: Int) {
                    if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        sheetBehavior.skipCollapsed = true
                    }
                }
            }
        )

        binding.recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val atTop = !binding.recycler.canScrollVertically(-1)
                    if (atTop != isNotElevated) {
                        elevationAnimator?.cancel()
                        isNotElevated = atTop
                        elevationAnimator?.cancel()
                        elevationAnimator = ObjectAnimator.ofFloat(
                            binding.titleLayout,
                            "elevation",
                            binding.titleLayout.elevation,
                            if (atTop) 0f else 10f.dpToPx
                        )
                        elevationAnimator?.duration = 100
                        elevationAnimator?.start()
                    }
                }
            }
        )
    }

    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int
    ): ActionMode? {
        (binding.sourceFilterSheet.parent as View).fitsSystemWindows = false
        return super.onWindowStartingActionMode(callback, type)
    }

    override fun dismiss() {
        super.dismiss()
        if (filterChanged) {
            onSearchClicked()
        }
    }

    fun setFilters(items: List<IFlexible<*>>) {
        adapter.updateDataSet(items)
    }
}
