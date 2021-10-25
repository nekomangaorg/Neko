package eu.kanade.tachiyomi.util

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.category.addtolibrary.SetCategoriesSheet
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import java.util.Date

fun Manga.shouldDownloadNewChapters(db: DatabaseHelper, prefs: PreferencesHelper): Boolean {
    if (!favorite) return false

    // Boolean to determine if user wants to automatically download new chapters.
    val downloadNew = prefs.downloadNew().get()
    if (!downloadNew) return false

    val categoriesToDownload = prefs.downloadNewCategories().get().map(String::toInt)
    if (categoriesToDownload.isEmpty()) return true

    // Get all categories, else default category (0)
    val categoriesForManga =
        db.getCategoriesForManga(this).executeAsBlocking()
            .mapNotNull { it.id }
            .takeUnless { it.isEmpty() } ?: listOf(0)

    val categoriesToExclude = prefs.downloadNewCategoriesExclude().get().map(String::toInt)
    if (categoriesForManga.intersect(categoriesToExclude).isNotEmpty()) return false

    return categoriesForManga.intersect(categoriesToDownload).isNotEmpty()
}

fun Manga.moveCategories(
    db: DatabaseHelper,
    activity: Activity,
    onMangaMoved: () -> Unit,
) {
    val categories = db.getCategories().executeAsBlocking()
    val categoriesForManga = db.getCategoriesForManga(this).executeAsBlocking()
    val ids = categoriesForManga.mapNotNull { it.id }.toTypedArray()
    SetCategoriesSheet(
        activity,
        this,
        categories.toMutableList(),
        ids,
        false
    ) {
        onMangaMoved()
    }.show()
}

fun List<Manga>.moveCategories(
    db: DatabaseHelper,
    activity: Activity,
    onMangaMoved: () -> Unit,
) {
    if (this.isEmpty()) return
    val categories = db.getCategories().executeAsBlocking()
    val commonCategories = map { db.getCategoriesForManga(it).executeAsBlocking() }
        .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2).toMutableList() }
        .toTypedArray()
    val mangaCategories = map { db.getCategoriesForManga(it).executeAsBlocking() }
    val common = mangaCategories.reduce { set1, set2 -> set1.intersect(set2).toMutableList() }
    val mixedCategories = mangaCategories.flatten().distinct().subtract(common).toMutableList()
    SetCategoriesSheet(
        activity,
        this,
        categories.toMutableList(),
        categories.map {
            when (it) {
                in commonCategories -> TriStateCheckBox.State.CHECKED
                in mixedCategories -> TriStateCheckBox.State.INDETERMINATE
                else -> TriStateCheckBox.State.UNCHECKED
            }
        }.toTypedArray(),
        false
    ) {
        onMangaMoved()
    }.show()
}

fun Manga.addOrRemoveToFavorites(
    db: DatabaseHelper,
    preferences: PreferencesHelper,
    view: View,
    activity: Activity,
    onMangaAdded: () -> Unit,
    onMangaMoved: () -> Unit,
    onMangaDeleted: () -> Unit,
): Snackbar? {
    if (!favorite) {
        val categories = db.getCategories().executeAsBlocking()
        val defaultCategoryId = preferences.defaultCategory()
        val defaultCategory = categories.find { it.id == defaultCategoryId }
        when {
            defaultCategory != null -> {
                favorite = true
                date_added = Date().time
                db.insertManga(this).executeAsBlocking()
                val mc = MangaCategory.create(this, defaultCategory)
                db.setMangaCategories(listOf(mc), listOf(this))
                onMangaMoved()
                return view.snack(activity.getString(R.string.added_to_, defaultCategory.name)) {
                    setAction(R.string.change) {
                        moveCategories(db, activity, onMangaMoved)
                    }
                }
            }
            defaultCategoryId == 0 || categories.isEmpty() -> { // 'Default' or no category
                favorite = true
                date_added = Date().time
                db.insertManga(this).executeAsBlocking()
                db.setMangaCategories(emptyList(), listOf(this))
                onMangaMoved()
                return if (categories.isNotEmpty()) {
                    view.snack(
                        activity.getString(
                            R.string.added_to_,
                            activity.getString(R.string.default_value)
                        )
                    ) {
                        setAction(R.string.change) {
                            moveCategories(db, activity, onMangaMoved)
                        }
                    }
                } else {
                    view.snack(R.string.added_to_library)
                }
            }
            else -> {
                val categoriesForManga = db.getCategoriesForManga(this).executeAsBlocking()
                val ids = categoriesForManga.mapNotNull { it.id }.toTypedArray()

                SetCategoriesSheet(
                    activity,
                    this,
                    categories.toMutableList(),
                    ids,
                    true
                ) {
                    onMangaAdded()
                }.show()
            }
        }
    } else {
        val lastAddedDate = date_added
        favorite = false
        date_added = 0
        db.insertManga(this).executeAsBlocking()
        onMangaMoved()
        return view.snack(
            view.context.getString(R.string.removed_from_library),
            Snackbar.LENGTH_INDEFINITE
        ) {
            setAction(R.string.undo) {
                favorite = true
                date_added = lastAddedDate
                db.insertManga(this@addOrRemoveToFavorites).executeAsBlocking()
                onMangaMoved()
            }
            addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!favorite) {
                            onMangaDeleted()
                        }
                    }
                }
            )
        }
    }
    return null
}

fun Manga.addNewScanlatorsToFilter(
    db: DatabaseHelper,
    existingChapters: List<Chapter>,
    newChapters: List<Chapter>,
): Set<String> {
    val existingScanlators =
        existingChapters.flatMap { it.scanlatorList() }.distinct()
            .toSet()
    val newScanlators =
        newChapters.flatMap { it.scanlatorList() }.distinct()
            .toSet()

    //Add new scanlators to the existing filter just so they aren't hidden
    val result = newScanlators.subtract(existingScanlators)
    if (result.isNotEmpty()) {
        if (this.filtered_scanlators != null) {
            val scanlators =
                ChapterUtil.getScanlators(this.filtered_scanlators!!).toSet() + newScanlators
            this.filtered_scanlators = ChapterUtil.getScanlatorString(scanlators)
        } else {
            this.filtered_scanlators =
                ChapterUtil.getScanlatorString(existingScanlators + newScanlators)
        }
        db.insertManga(this).executeAsBlocking()
    }
    return result
}

/**
 * Returns a manga from the database for the given manga from network. It creates a new entry
 * if the manga is not yet in the database.
 *
 * @param sManga the manga from the source.
 * @return a manga from the database.
 */
fun SManga.toLocalManga(db: DatabaseHelper, sourceId: Long): Manga {
    var localManga = db.getManga(this.url, sourceId).executeAsBlocking()
    if (localManga == null) {
        val newManga = Manga.create(this.url, this.title, sourceId)
        newManga.copyFrom(this)
        val result = db.insertManga(newManga).executeAsBlocking()
        newManga.id = result.insertedId()
        localManga = newManga
    } else if (localManga.title.isBlank()) {
        localManga.title = this.title
        db.insertManga(localManga).executeAsBlocking()
    }
    return localManga
}
