package eu.kanade.tachiyomi.util

import android.app.Activity
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.category.addtolibrary.SetCategoriesSheet
import eu.kanade.tachiyomi.ui.source.browse.HomePageManga
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.SimpleManga
import org.nekomanga.domain.manga.SourceManga

fun Manga.shouldDownloadNewChapters(db: DatabaseHelper, prefs: PreferencesHelper): Boolean {
    if (!favorite) return false

    // Boolean to determine if user wants to automatically download new chapters.
    val downloadNewChapters = prefs.downloadNewChapters().get()
    if (!downloadNewChapters) return false

    val includedCategories = prefs.downloadNewChaptersInCategories().get().map(String::toInt)
    val excludedCategories = prefs.excludeCategoriesInDownloadNew().get().map(String::toInt)
    if (includedCategories.isEmpty() && excludedCategories.isEmpty()) return true

    // Get all categories, else default category (0)
    val categoriesForManga =
        db.getCategoriesForManga(this)
            .executeAsBlocking()
            .mapNotNull { it.id }
            .takeUnless { it.isEmpty() } ?: listOf(0)

    if (categoriesForManga.any { it in excludedCategories }) return false

    // Included category not selected
    if (includedCategories.isEmpty()) return true

    return categoriesForManga.any { it in includedCategories }
}

fun List<Manga>.moveCategories(db: DatabaseHelper, activity: Activity, onMangaMoved: () -> Unit) {
    if (this.isEmpty()) return
    val categories = db.getCategories().executeAsBlocking()
    val commonCategories =
        map { db.getCategoriesForManga(it).executeAsBlocking() }
            .reduce { set1: Iterable<Category>, set2 ->
                set1.intersect(set2.toSet()).toMutableList()
            }
            .toTypedArray()
    val mangaCategories = map { db.getCategoriesForManga(it).executeAsBlocking() }
    val common =
        mangaCategories.reduce { set1, set2 -> set1.intersect(set2.toSet()).toMutableList() }
    val mixedCategories =
        mangaCategories.flatten().distinct().subtract(common.toSet()).toMutableList()
    SetCategoriesSheet(
            activity,
            this,
            categories.toMutableList(),
            categories
                .map {
                    when (it) {
                        in commonCategories -> TriStateCheckBox.State.CHECKED
                        in mixedCategories -> TriStateCheckBox.State.IGNORE
                        else -> TriStateCheckBox.State.UNCHECKED
                    }
                }
                .toTypedArray(),
            false,
        ) {
            onMangaMoved()
        }
        .show()
}

/** Takes a SourceManga and converts to a display manga */
fun SourceManga.toDisplayManga(db: DatabaseHelper, sourceId: Long): DisplayManga {
    var localManga = db.getManga(this.url, sourceId).executeAsBlocking()
    if (localManga == null) {
        val newManga = Manga.create(this.url, this.title, sourceId)
        newManga.apply { this.thumbnail_url = currentThumbnail }
        val result = db.insertManga(newManga).executeAsBlocking()
        newManga.id = result.insertedId()
        localManga = newManga
    } else if (localManga.title.isBlank()) {
        localManga.title = this.title
        db.insertManga(localManga).executeAsBlocking()
    }
    return localManga.toDisplayManga(this.displayText, this.displayTextRes)
}

fun Manga.toDisplayManga(
    displayText: String = "",
    @StringRes displayTextRes: Int? = null,
): DisplayManga {
    return DisplayManga(
        mangaId = this.id!!,
        url = this.url,
        title = (this as? MangaImpl)?.title ?: this.title,
        inLibrary = this.favorite,
        displayText = displayText.replace("_", " ").capitalizeWords(),
        displayTextRes = displayTextRes,
        currentArtwork =
            Artwork(
                url = this.user_cover ?: "",
                inLibrary = this.favorite,
                mangaId = this.id!!,
                originalArtwork = this.thumbnail_url ?: MdConstants.noCoverUrl,
            ),
    )
}

fun Manga.toSimpleManga(): SimpleManga {
    return SimpleManga(id = this.id!!, title = (this as? MangaImpl)?.title ?: this.title)
}

fun SManga.getSlug(): String {
    val title =
        this.title
            .trim()
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .replace("-+$".toRegex(), "")

    val wordList = title.split('-')
    val slug = mutableListOf<String>()

    for (i in wordList) {
        if ((slug.joinToString("-", "", "-") + i).count() < 100) {
            slug.add(i)
        } else {
            break
        }
    }

    return slug.joinToString("-")
}

/** resync homepage manga with db manga */
fun List<HomePageManga>.resync(db: DatabaseHelper): ImmutableList<HomePageManga> {
    return this.map { homePageManga ->
            homePageManga.copy(
                displayManga = homePageManga.displayManga.resync(db).toImmutableList()
            )
        }
        .toImmutableList()
}

fun List<DisplayManga>.resync(db: DatabaseHelper): List<DisplayManga> {
    return this.mapNotNull { displayManga ->
        val dbManga = db.getManga(displayManga.mangaId).executeAsBlocking()
        when (dbManga == null) {
            true -> null
            else ->
                displayManga.copy(
                    inLibrary = dbManga.favorite,
                    currentArtwork =
                        displayManga.currentArtwork.copy(
                            url = dbManga.user_cover ?: "",
                            originalArtwork = dbManga.thumbnail_url ?: MdConstants.noCoverUrl,
                        ),
                )
        }
    }
}

fun List<DisplayManga>.unique(): List<DisplayManga> {
    return this.distinctBy { it.url }
}

/** Updates the visibility of HomePageManga display manga */
fun List<HomePageManga>.updateVisibility(prefs: PreferencesHelper): ImmutableList<HomePageManga> {
    return this.map { homePageManga ->
            homePageManga.copy(
                displayManga = homePageManga.displayManga.updateVisibility(prefs).toImmutableList()
            )
        }
        .toImmutableList()
}

/**
 * Marks display manga as visible when show library entries is enabled, otherwise hides library
 * entries
 */
fun List<DisplayManga>.updateVisibility(prefs: PreferencesHelper): List<DisplayManga> {
    return this.map { displayManga ->
        when (prefs.browseDisplayMode().get() % 3) {
            2 -> displayManga.copy(isVisible = displayManga.inLibrary)
            1 -> displayManga.copy(isVisible = !displayManga.inLibrary)
            else -> displayManga.copy(isVisible = true)
        }
    }
}

/** Filters out library manga if enabled */
fun List<DisplayManga>.filterVisibility(prefs: PreferencesHelper): List<DisplayManga> {
    return when (prefs.browseDisplayMode().get() % 3) {
        2 -> this.filter { it.inLibrary }
        1 -> this.filter { !it.inLibrary }
        else -> this
    }
}
