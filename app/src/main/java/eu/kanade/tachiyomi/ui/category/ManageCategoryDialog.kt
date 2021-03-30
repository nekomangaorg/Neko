package eu.kanade.tachiyomi.ui.category

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.MangaCategoryDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.injectLazy

class ManageCategoryDialog(bundle: Bundle? = null) :
    DialogController(bundle) {

    constructor(libraryController: LibraryController, category: Category) : this() {
        this.libraryController = libraryController
        this.category = category
    }

    private var libraryController: LibraryController? = null
    private var category: Category? = null

    private val preferences by injectLazy<PreferencesHelper>()
    private val db by injectLazy<DatabaseHelper>()
    var binding: MangaCategoryDialogBinding? = null

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = MangaCategoryDialogBinding.inflate(activity!!.layoutInflater)
        val dialog = MaterialDialog(activity!!).apply {
            title(R.string.manage_category)
            customView(view = binding!!.root)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.save) { onPositiveButtonClick() }
        }
        onViewCreated(dialog.view)
        return dialog
    }

    private fun onPositiveButtonClick() {
        val binding = binding ?: return
        val category = category ?: return
        if (category.id ?: 0 <= 0) return
        val text = binding.title.text.toString()
        val categoryExists = categoryExists(text)
        if (text.isNotBlank() && !categoryExists && !text.equals(category.name, true)) {
            category.name = text
            db.insertCategory(category).executeAsBlocking()
            libraryController?.presenter?.getLibrary()
        } else if (categoryExists) {
            activity?.toast(R.string.category_with_name_exists)
        }
        if (!updatePref(preferences.downloadNewCategories(), binding.downloadNew)) {
            preferences.downloadNew().set(false)
        } else {
            preferences.downloadNew().set(true)
        }
        if (preferences.libraryUpdateInterval().getOrDefault() > 0 &&
            !updatePref(preferences.libraryUpdateCategories(), binding.downloadNew)
        ) {
            preferences.libraryUpdateInterval().set(0)
            LibraryUpdateJob.setupTask(0)
        }
    }

    /**
     * Returns true if a category with the given name already exists.
     */
    private fun categoryExists(name: String): Boolean {
        return db.getCategories().executeAsBlocking().any {
            it.name.equals(name, true) && category?.id != it.id
        }
    }

    fun onViewCreated(view: View) {
        val binding = binding ?: return
        val category = category ?: return
        if (category.id ?: 0 <= 0) {
            binding.title.gone()
            binding.downloadNew.gone()
            binding.includeGlobal.gone()
            return
        }
        binding.editCategories.setOnClickListener {
            router.popCurrentController()
            router.pushController(CategoryController().withFadeTransaction())
        }
        binding.title.hint = category.name
        binding.title.append(category.name)
        val downloadNew = preferences.downloadNew().get()
        setCheckbox(
            binding.downloadNew,
            preferences.downloadNewCategories(),
            true
        )
        if (downloadNew && preferences.downloadNewCategories().get().isEmpty()) {
            binding.downloadNew.gone()
        } else if (!downloadNew) {
            binding.downloadNew.visible()
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
    private fun updatePref(categories: Preference<Set<String>>, box: CompoundButton): Boolean {
        val categoryId = category?.id ?: return true
        val updateCategories = categories.get().toMutableSet()
        if (box.isChecked) {
            updateCategories.add(categoryId.toString())
        } else {
            updateCategories.remove(categoryId.toString())
        }
        categories.set(updateCategories)
        return updateCategories.isNotEmpty()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        binding = null
    }

    private fun setCheckbox(
        box: CompoundButton,
        categories: Preference<Set<String>>,
        shouldShow: Boolean
    ) {
        val updateCategories = categories.get()
        box.visibleIf(updateCategories.isNotEmpty() && shouldShow)
        if (updateCategories.isNotEmpty() && shouldShow) box.isChecked =
            updateCategories.any { category?.id == it.toIntOrNull() }
    }
}
