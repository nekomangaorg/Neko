package eu.kanade.tachiyomi.ui.category

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.MangaCategoryDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.injectLazy

class ManageCategoryDialog(bundle: Bundle? = null) :
    DialogController(bundle) {

    constructor(category: Category?, updateLibrary: ((Int?) -> Unit)) : this() {
        this.updateLibrary = updateLibrary
        this.category = category
    }

    private var updateLibrary: ((Int?) -> Unit)? = null
    private var category: Category? = null

    private val preferences by injectLazy<PreferencesHelper>()
    private val db by injectLazy<DatabaseHelper>()
    lateinit var binding: MangaCategoryDialogBinding

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = dialog(activity!!)
        binding = MangaCategoryDialogBinding.bind(dialog.getCustomView())
        onViewCreated()
        return dialog
    }

    fun dialog(activity: Activity): MaterialDialog {
        return MaterialDialog(activity).apply {
            title(if (category == null) R.string.new_category else R.string.manage_category)
            customView(viewRes = R.layout.manga_category_dialog)
            negativeButton(android.R.string.cancel) { dismiss() }
            positiveButton(R.string.save) {
                if (onPositiveButtonClick()) {
                    dismiss()
                }
            }
            noAutoDismiss()
        }
    }

    fun show(activity: Activity) {
        val dialog = dialog(activity)
        binding = MangaCategoryDialogBinding.bind(dialog.getCustomView())
        onViewCreated()
        dialog.onShow {
            binding.title.requestFocus()
        }
        dialog.show()
    }

    private fun onPositiveButtonClick(): Boolean {
        val text = binding.title.text.toString()
        val categoryExists = categoryExists(text)
        val category = this.category ?: Category.create(text)
        if (category.id != 0) {
            if (text.isNotBlank() && !categoryExists &&
                !text.equals(this.category?.name ?: "", true)
            ) {
                category.name = text
                if (this.category == null) {
                    val categories = db.getCategories().executeAsBlocking()
                    category.order = (categories.maxOfOrNull { it.order } ?: 0) + 1
                    category.mangaSort = LibrarySort.Title.categoryValue
                    val dbCategory = db.insertCategory(category).executeAsBlocking()
                    category.id = dbCategory.insertedId()?.toInt()
                    this.category = category
                } else {
                    db.insertCategory(category).executeAsBlocking()
                }
            } else if (categoryExists) {
                binding.categoryTextLayout.error =
                    binding.categoryTextLayout.context.getString(R.string.category_with_name_exists)
                return false
            } else if (text.isBlank()) {
                binding.categoryTextLayout.error =
                    binding.categoryTextLayout.context.getString(R.string.category_cannot_be_blank)
                return false
            }
        }
        when (updatePref(preferences.downloadNewCategories(), binding.downloadNew)) {
            true -> preferences.downloadNew().set(true)
            false -> preferences.downloadNew().set(false)
        }
        if (preferences.libraryUpdateInterval().getOrDefault() > 0 &&
            updatePref(preferences.libraryUpdateCategories(), binding.includeGlobal) == false
        ) {
            preferences.libraryUpdateInterval().set(0)
            LibraryUpdateJob.setupTask(binding.root.context, 0)
        }
        updateLibrary?.invoke(category.id)
        return true
    }

    /**
     * Returns true if a category with the given name already exists.
     */
    private fun categoryExists(name: String): Boolean {
        return db.getCategories().executeAsBlocking().any {
            it.name.equals(name, true) && category?.id != it.id
        }
    }

    fun onViewCreated() {
        if (category?.id ?: 0 <= 0 && category != null) {
            binding.categoryTextLayout.isVisible = false
        }
        binding.editCategories.isVisible = category != null
        binding.editCategories.setOnClickListener {
            router.popCurrentController()
            router.pushController(CategoryController().withFadeTransaction())
        }
        binding.title.addTextChangedListener {
            binding.categoryTextLayout.error = null
        }
        binding.title.hint =
            category?.name ?: binding.editCategories.context.getString(R.string.category)
        binding.title.append(category?.name ?: "")
        val downloadNew = preferences.downloadNew().get()
        setCheckbox(
            binding.downloadNew,
            preferences.downloadNewCategories(),
            true
        )
        if (downloadNew && preferences.downloadNewCategories().get().isEmpty()) {
            binding.downloadNew.isVisible = false
        } else if (!downloadNew) {
            binding.downloadNew.isVisible = true
        }
        binding.downloadNew.isChecked =
            preferences.downloadNew().get() && binding.downloadNew.isChecked
        setCheckbox(
            binding.includeGlobal,
            preferences.libraryUpdateCategories(),
            preferences.libraryUpdateInterval().getOrDefault() > 0
        )
    }

    /** Update a pref based on checkbox, and return if the pref is not empty */
    private fun updatePref(categories: Preference<Set<String>>, box: CompoundButton): Boolean? {
        val categoryId = category?.id ?: return null
        if (!box.isVisible) return null
        val updateCategories = categories.get().toMutableSet()
        if (box.isChecked) {
            updateCategories.add(categoryId.toString())
        } else {
            updateCategories.remove(categoryId.toString())
        }
        categories.set(updateCategories)
        return updateCategories.isNotEmpty()
    }

    private fun setCheckbox(
        box: CompoundButton,
        categories: Preference<Set<String>>,
        shouldShow: Boolean,
    ) {
        val updateCategories = categories.get()
        box.isVisible = updateCategories.isNotEmpty() && shouldShow
        if (updateCategories.isNotEmpty() && shouldShow) box.isChecked =
            updateCategories.any { category?.id == it.toIntOrNull() }
    }
}
