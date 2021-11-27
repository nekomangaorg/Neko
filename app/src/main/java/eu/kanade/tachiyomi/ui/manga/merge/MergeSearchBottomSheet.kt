package eu.kanade.tachiyomi.ui.manga.merge

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MergeSearchBottomSheetBinding
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsDivider
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog

class MergeSearchBottomSheet(controller: MangaDetailsController) :
    E2EBottomSheetDialog<MergeSearchBottomSheetBinding>(controller.activity!!) {

    val activity = controller.activity!!
    private val presenter = controller.presenter
    private val mergeSearchItemAdapter = ItemAdapter<MergeSearchItem>()
    private val mergeSearchAdapter = FastAdapter.with(mergeSearchItemAdapter)
    override var recyclerView: RecyclerView? = binding.mergeRecycler

    override fun createBinding(inflater: LayoutInflater) =
        MergeSearchBottomSheetBinding.inflate(inflater)

    init {
        val insets =
            activity.window.decorView.rootWindowInsetsCompat?.getInsets(WindowInsetsCompat.Type.systemBars())
        val height = insets?.bottom ?: 0
        sheetBehavior.peekHeight = 800.dpToPx + height
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true

        binding.searchCloseButton.setOnClickListener {
            onBackPressed()
        }

        binding.mergeSearch.append(presenter.manga.title)
        binding.mergeSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val text = binding.mergeSearch.text?.toString() ?: ""
                if (text.isNotBlank()) {
                    startTransition()
                    val imm =
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(binding.mergeSearch.windowToken, 0)
                    binding.mergeSearch.clearFocus()
                    search(text)
                }
            }
            true
        }

        mergeSearchAdapter.onClickListener = { _, _, item, _ ->
            presenter.attachMergeManga(item.manga)
            presenter.refreshAll()
            this.dismiss()
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.textInputLayout.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)

        binding.mergeRecycler.adapter = mergeSearchAdapter
        binding.mergeRecycler.layoutManager = GridLayoutManager(binding.root.context, 3)

        binding.mergeRecycler.setHasFixedSize(false)
        binding.mergeRecycler.addItemDecoration(MangaDetailsDivider(activity, 16.dpToPx))
        binding.mergeRecycler.itemAnimator = null
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
        search(presenter.manga.title)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.mergeRecycler.adapter = null
    }

    private fun search(query: String) {
        binding.progress.visibility = View.VISIBLE
        binding.mergeRecycler.visibility = View.INVISIBLE
        mergeSearchItemAdapter.clear()
        presenter.mergeSearch(query)
    }

    fun onSearchResults(results: List<SManga>) {
        binding.progress.visibility = View.GONE
        if (results.isEmpty()) {
            noResults()
        } else {
            binding.searchEmptyView.visibility = View.GONE
            binding.mergeRecycler.visibility = View.VISIBLE
            mergeSearchItemAdapter.set(
                results.map {
                    MergeSearchItem(it)
                }
            )
        }
    }

    private fun noResults() {
        binding.mergeRecycler.visibility = View.INVISIBLE
        binding.searchEmptyView.showMedium(
            CommunityMaterial.Icon.cmd_compass_off,
            binding.root.context.getString(R.string.no_results_found)
        )
    }

    fun onSearchResultsError() {
        binding.progress.visibility = View.GONE
        noResults()
    }

    private fun startTransition(duration: Long = 100) {
        val transition = androidx.transition.TransitionSet()
            .addTransition(androidx.transition.ChangeBounds())
            .addTransition(androidx.transition.Fade())
        transition.duration = duration
        val mainView = binding.root.parent as ViewGroup
        TransitionManager.endTransitions(mainView)
        TransitionManager.beginDelayedTransition(mainView, transition)
    }
}
