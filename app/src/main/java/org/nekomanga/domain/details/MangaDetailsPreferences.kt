package org.nekomanga.domain.details

import eu.kanade.tachiyomi.data.database.models.Manga
import tachiyomi.core.preference.PreferenceStore

class MangaDetailsPreferences(private val preferenceStore: PreferenceStore) {
    fun filterChapterByRead() =
        this.preferenceStore.getInt("default_chapter_filter_by_read", Manga.SHOW_ALL)

    fun filterChapterByDownloaded() =
        this.preferenceStore.getInt("default_chapter_filter_by_downloaded", Manga.SHOW_ALL)

    fun filterChapterByBookmarked() =
        this.preferenceStore.getInt("default_chapter_filter_by_bookmarked", Manga.SHOW_ALL)

    fun sortChapterOrder() =
        this.preferenceStore.getInt(
            "default_chapter_sort_by_source_or_number",
            Manga.CHAPTER_SORTING_SOURCE,
        )

    fun hideChapterTitlesByDefault() = this.preferenceStore.getBoolean("hide_chapter_titles")

    fun filterChapterByUnavailable() =
        this.preferenceStore.getInt("hide_unavailable_chapters", Manga.SHOW_ALL)

    fun chaptersDescAsDefault() = this.preferenceStore.getBoolean("chapters_desc_as_default", true)

    fun autoThemeByCover() = this.preferenceStore.getBoolean("theme_manga_details", true)

    fun forcePortrait() = this.preferenceStore.getBoolean("force_portrait", false)

    fun coverRatios() = this.preferenceStore.getStringSet("cover_ratio")

    fun coverColors() = this.preferenceStore.getStringSet("cover_colors")

    fun coverVibrantColors() = this.preferenceStore.getStringSet("cover_vibrant_colors")

    fun wrapAltTitles() = this.preferenceStore.getBoolean("wrap_alt_titles_manga_details")

    fun hideButtonText() = this.preferenceStore.getBoolean("hide_manga_detail_button_text")

    fun extraLargeBackdrop() = this.preferenceStore.getBoolean("extra_large_backdrop")
}
