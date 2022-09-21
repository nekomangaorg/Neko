package eu.kanade.tachiyomi.ui.category.addtolibrary

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.databinding.SetCategoriesSheetBinding
import eu.kanade.tachiyomi.ui.category.ManageCategoryDialog
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import java.util.Date
import java.util.Locale
import kotlin.math.max
import uy.kohesive.injekt.injectLazy

class SetCategoriesSheet(
    private val activity: Activity,
    private val listManga: List<Manga>,
    var categories: MutableList<Category>,
    var preselected: Array<TriStateCheckBox.State>,
    private val addingToLibrary: Boolean,
    val onMangaAdded: (() -> Unit) = { },
) : E2EBottomSheetDialog<SetCategoriesSheetBinding>(activity) {

    constructor(
        activity: Activity,
        manga: Manga,
        categories: MutableList<Category>,
        preselected: Array<Int>,
        addingToLibrary: Boolean,
        onMangaAdded: () -> Unit,
    ) : this(
        activity,
        listOf(manga),
        categories,
        categories.map {
            if (it.id in preselected) {
                TriStateCheckBox.State.CHECKED
            } else {
                TriStateCheckBox.State.UNCHECKED
            }
        }.toTypedArray(),
        addingToLibrary,
        onMangaAdded,
    )

    private val fastAdapter: FastAdapter<AddCategoryItem>
    private val itemAdapter = ItemAdapter<AddCategoryItem>()

    private val db: DatabaseHelper by injectLazy()
    override var recyclerView: RecyclerView? = binding.categoryRecyclerView

    private val preCheckedCategories = categories.mapIndexedNotNull { index, category ->
        category.takeIf { preselected[index] == TriStateCheckBox.State.CHECKED }
    }
    private val preIndeterminateCategories = categories.mapIndexedNotNull { index, category ->
        category.takeIf { preselected[index] == TriStateCheckBox.State.IGNORE }
    }
    private val selectedCategories = preIndeterminateCategories + preCheckedCategories

    private val selectedItems: Set<AddCategoryItem>
        get() = itemAdapter.adapterItems.filter { it.isSelected }.toSet()

    private val checkedItems: Set<AddCategoryItem>
        get() = itemAdapter.adapterItems.filter { it.state == TriStateCheckBox.State.CHECKED }
            .toSet()

    private val indeterminateItems: Set<AddCategoryItem>
        get() = itemAdapter.adapterItems.filter { it.state == TriStateCheckBox.State.IGNORE }
            .toSet()

    private val uncheckedItems: Set<AddCategoryItem>
        get() = itemAdapter.adapterItems.filter { !it.isSelected }.toSet()

    override fun createBinding(inflater: LayoutInflater) =
        SetCategoriesSheetBinding.inflate(inflater)

    init {
        binding.toolbarTitle.text = context.getString(
            if (addingToLibrary) R.string.add_x_to else R.string.move_x_to,
            if (listManga.size == 1) {
                listManga.first().seriesType(context)
            } else {
                context.getString(R.string.selection).lowercase(Locale.ROOT)
            },
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
            },
        )

        binding.titleLayout.checkHeightThen {
            binding.categoryRecyclerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                val fullHeight = activity.window.decorView.height
                val insets = activity.window.decorView.rootWindowInsetsCompat
                matchConstraintMaxHeight =
                    fullHeight - (insets?.getInsets(systemBars())?.top ?: 0) -
                    binding.titleLayout.height - binding.buttonLayout.height - 45.dpToPx
            }
        }

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoryRecyclerView.adapter = fastAdapter
        itemAdapter.set(
            categories.mapIndexed { index, category ->
                AddCategoryItem(category).apply {
                    skipInversed = preselected[index] != TriStateCheckBox.State.IGNORE
                    state = preselected[index]
                }
            },
        )
        setCategoriesButtons()
        fastAdapter.onClickListener = onClickListener@{ view, _, item, _ ->
            val checkBox = view as? TriStateCheckBox ?: return@onClickListener true
            checkBox.goToNextStep()
            item.state = checkBox.state
            setCategoriesButtons()
            true
        }
    }

    private fun setCategoriesButtons() {
        val addingMore = checkedItems.isNotEmpty() &&
            selectedCategories.isNotEmpty() &&
            selectedItems.map { it.category }
                .containsAll(selectedCategories) &&
            checkedItems.size > preCheckedCategories.size
        val nothingChanged = itemAdapter.adapterItems.map { it.state }
            .toTypedArray()
            .contentEquals(preselected)
        val removing = selectedItems.isNotEmpty() && (
            // Check that selected items has the previous delta items
            (
                selectedCategories.containsAll(indeterminateItems.map { it.category }) &&
                    preIndeterminateCategories.size > indeterminateItems.size
                ) ||
                // or check that checked items has the previous checked items
                (
                    preCheckedCategories.containsAll(checkedItems.map { it.category }) &&
                        preCheckedCategories.size > checkedItems.size
                    )
            ) &&
            // Additional checks in case a delta item is now fully checked
            preCheckedCategories.size >= checkedItems.size &&
            preIndeterminateCategories.size >= indeterminateItems.size

        val items = when {
            addingToLibrary -> checkedItems.map { it.category }
            addingMore -> checkedItems.map { it.category }.subtract(preCheckedCategories)
            removing -> selectedCategories.subtract(selectedItems.map { it.category })
            nothingChanged -> selectedItems.map { it.category }
            else -> checkedItems.map { it.category }
        }
        binding.addToCategoriesButton.text = context.getString(
            when {
                addingToLibrary || (addingMore && !nothingChanged) -> R.string.add_to_
                removing -> R.string.remove_from_
                nothingChanged -> R.string.keep_in_
                else -> R.string.move_to_
            },
            when (items.size) {
                0 -> context.getString(R.string.default_category).lowercase(Locale.ROOT)
                1 -> items.firstOrNull()?.name ?: ""
                else -> context.resources.getQuantityString(
                    R.plurals.category_plural,
                    items.size,
                    items.size,
                )
            },
        )
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true
        updateBottomButtons()
        binding.root.post {
            binding.categoryRecyclerView.scrollToPosition(
                max(0, itemAdapter.adapterItems.indexOf(selectedItems.firstOrNull())),
            )
        }
    }

    fun updateBottomButtons() {
        val bottomSheet = binding.root.parent as View
        val bottomSheetVisibleHeight =
            -bottomSheet.top + (activity.window.decorView.height - bottomSheet.height)

        binding.buttonLayout.translationY = bottomSheetVisibleHeight.toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attrsArray = intArrayOf(R.attr.mainActionBarSize)
        val array = context.obtainStyledAttributes(attrsArray)
        val headerHeight = array.getDimensionPixelSize(0, 0)
        binding.buttonLayout.updatePaddingRelative(
            bottom = activity.window.decorView.rootWindowInsetsCompat
                ?.getInsets(systemBars())?.bottom ?: 0,
        )

        binding.buttonLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = headerHeight + binding.buttonLayout.paddingBottom
        }
        array.recycle()

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.newCategoryButton.setOnClickListener {
            ManageCategoryDialog(null) {
                categories = db.getCategories().executeAsBlocking()
                val map = itemAdapter.adapterItems.map { it.category.id to it.state }.toMap()
                itemAdapter.set(
                    categories.mapIndexed { index, category ->
                        AddCategoryItem(category).apply {
                            skipInversed =
                                preselected.getOrElse(index) { TriStateCheckBox.State.UNCHECKED } != TriStateCheckBox.State.IGNORE
                            state = map[category.id] ?: TriStateCheckBox.State.CHECKED
                        }
                    },
                )
                setCategoriesButtons()
            }.show(activity)
        }

        binding.addToCategoriesButton.setOnClickListener {
            addMangaToCategories()
            dismiss()
        }
    }

    private fun addMangaToCategories() {
        if (listManga.size == 1 && !listManga.first().favorite) {
            val manga = listManga.first()
            manga.favorite = !manga.favorite

            manga.date_added = Date().time

            db.insertManga(manga).executeAsBlocking()
        }

        val addCategories = checkedItems.map(AddCategoryItem::category)
        val removeCategories = uncheckedItems.map(AddCategoryItem::category)
        val mangaCategories = listManga.map { manga ->
            val categories = db.getCategoriesForManga(manga).executeAsBlocking()
                .subtract(removeCategories).plus(addCategories).distinct()
            categories.map { MangaCategory.create(manga, it) }
        }.flatten()
        db.setMangaCategories(mangaCategories, listManga)
        onMangaAdded()
    }
}
