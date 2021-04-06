package eu.kanade.tachiyomi.ui.category.addtolibrary

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.databinding.SetCategoriesSheetBinding
import eu.kanade.tachiyomi.ui.category.ManageCategoryDialog
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Date
import java.util.Locale
import kotlin.math.max

class SetCategoriesSheet(
    private val activity: Activity,
    private val manga: Manga,
    var categories: MutableList<Category>,
    var preselected: Array<Int>,
    val addingToLibrary: Boolean,
    val onMangaAdded: (() -> Unit) = { }
) : BottomSheetDialog
(activity, R.style.BottomSheetDialogTheme) {

    private var sheetBehavior: BottomSheetBehavior<*>

    private val fastAdapter: FastAdapter<AddCategoryItem>
    private val itemAdapter = ItemAdapter<AddCategoryItem>()
    private val selectExtension: SelectExtension<AddCategoryItem>
    private val db: DatabaseHelper by injectLazy()

    private val binding = SetCategoriesSheetBinding.inflate(activity.layoutInflater)
    init {
        // Use activity theme for this layout
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)
        setEdgeToEdge(activity, binding.root)

        binding.toolbarTitle.text = context.getString(
            if (addingToLibrary) {
                R.string.add_x_to
            } else {
                R.string.move_x_to
            },
            manga.mangaType(context)
        )

        setOnShowListener {
            updateBottomButtons()
        }
        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    updateBottomButtons()
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    updateBottomButtons()
                }
            }
        )

        binding.categoryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    sheetBehavior.isDraggable = !recyclerView.canScrollVertically(-1)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.canScrollVertically(-1)) {
                    sheetBehavior.isDraggable = false
                }
            }
        })

        binding.titleLayout.viewTreeObserver.addOnGlobalLayoutListener {
            binding.categoryRecyclerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                val fullHeight = activity.window.decorView.height
                val insets = activity.window.decorView.rootWindowInsets
                matchConstraintMaxHeight =
                    fullHeight - (insets?.systemWindowInsetTop ?: 0) -
                    binding.titleLayout.height - binding.buttonLayout.height - 75.dpToPx
            }
        }

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoryRecyclerView.adapter = fastAdapter
        itemAdapter.set(categories.map(::AddCategoryItem))
        itemAdapter.adapterItems.forEach { item ->
            item.isSelected = preselected.any { it == item.category.id }
        }

        selectExtension = fastAdapter.getSelectExtension()
        selectExtension.apply {
            isSelectable = true
            multiSelect = true
            setCategoriesButtons()
            selectionListener = object : ISelectionListener<AddCategoryItem> {
                override fun onSelectionChanged(item: AddCategoryItem, selected: Boolean) {
                    setCategoriesButtons()
                }
            }
        }
    }

    fun setCategoriesButtons() {
        binding.addToCategoriesButton.text = context.getString(
            if (addingToLibrary) {
                R.string.add_to_
            } else {
                R.string.move_to_
            },
            when (selectExtension.selections.size) {
                0 -> context.getString(R.string.default_category).lowercase(Locale.ROOT)
                1 -> selectExtension.selectedItems.firstOrNull()?.category?.name ?: ""
                else -> context.resources.getQuantityString(
                    R.plurals.category_plural,
                    selectExtension.selections.size,
                    selectExtension.selections.size
                )
            }
        )
        binding.categoryRecyclerView.scrollToPosition(
            max(0, itemAdapter.adapterItems.indexOf(selectExtension.selectedItems.firstOrNull()))
        )
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true
        updateBottomButtons()
    }

    fun updateBottomButtons() {
        val bottomSheet = binding.root.parent as View
        val bottomSheetVisibleHeight = -bottomSheet.top + (activity.window.decorView.height - bottomSheet.height)

        binding.buttonLayout.translationY = bottomSheetVisibleHeight.toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = context.obtainStyledAttributes(attrsArray)
        val headerHeight = array.getDimensionPixelSize(0, 0)
        binding.buttonLayout.updatePaddingRelative(
            bottom = activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        )

        binding.buttonLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = headerHeight + binding.buttonLayout.paddingBottom
        }
        array.recycle()

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.newCategoryButton.setOnClickListener {
            ManageCategoryDialog(null) {
                categories = db.getCategories().executeAsBlocking()
                itemAdapter.set(categories.map(::AddCategoryItem))
                itemAdapter.adapterItems.forEach { item ->
                    item.isSelected = it == item.category.id
                }
                setCategoriesButtons()
            }.show(activity)
        }

        binding.addToCategoriesButton.setOnClickListener {
            addMangaToCategories()
            dismiss()
        }
    }

    private fun addMangaToCategories() {
        if (!manga.favorite) {
            manga.favorite = !manga.favorite

            manga.date_added = Date().time

            db.insertManga(manga).executeAsBlocking()
        }

        val selectedCategories = selectExtension.selectedItems.map(AddCategoryItem::category)
        val mc = selectedCategories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
        onMangaAdded()
    }
}
