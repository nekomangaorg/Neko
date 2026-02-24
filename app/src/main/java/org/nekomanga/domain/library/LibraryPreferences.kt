package org.nekomanga.domain.library

import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.library.LibraryGroup
import eu.kanade.tachiyomi.ui.library.filter.FilterBookmarked
import eu.kanade.tachiyomi.ui.library.filter.FilterCompleted
import eu.kanade.tachiyomi.ui.library.filter.FilterDownloaded
import eu.kanade.tachiyomi.ui.library.filter.FilterMangaType
import eu.kanade.tachiyomi.ui.library.filter.FilterMerged
import eu.kanade.tachiyomi.ui.library.filter.FilterMissingChapters
import eu.kanade.tachiyomi.ui.library.filter.FilterTracked
import eu.kanade.tachiyomi.ui.library.filter.FilterUnavailable
import eu.kanade.tachiyomi.ui.library.filter.FilterUnread
import tachiyomi.core.preference.PreferenceStore

class LibraryPreferences(private val preferenceStore: PreferenceStore) {

    fun defaultCategory() = this.preferenceStore.getInt("default_category", -1)

    fun enableLocalChapters() = this.preferenceStore.getBoolean("enable_local_chapters", true)

    fun searchSuggestions() = this.preferenceStore.getString("library_search_suggestion")

    fun showSearchSuggestions() = this.preferenceStore.getBoolean("show_library_search_suggestions")

    fun lastSearchSuggestion() = this.preferenceStore.getLong("last_library_suggestion")

    fun updateInterval() = this.preferenceStore.getInt("pref_library_update_interval_key", 24)

    fun lastUpdateTimestamp() = this.preferenceStore.getLong("library_update_last_timestamp")

    fun lastUpdateAttemptTimestamp() =
        this.preferenceStore.getLong("library_update_last_attempt_timestamp")

    fun lastUpdateDuration() = this.preferenceStore.getString("library_update_duration")

    fun libraryUpdateIds() = this.preferenceStore.getString("library_update_ids")

    fun autoUpdateDeviceRestrictions() =
        preferenceStore.getStringSet(
            "library_update_restriction",
            setOf(DEVICE_NETWORK_NOT_METERED),
        )

    fun autoUpdateMangaRestrictions() =
        this.preferenceStore.getStringSet(
            "library_update_manga_restrictions",
            setOf(
                MANGA_HAS_UNREAD,
                MANGA_NOT_STARTED,
                MANGA_NOT_COMPLETED,
                MANGA_TRACKING_PLAN_TO_READ,
                MANGA_TRACKING_DROPPED,
                MANGA_TRACKING_ON_HOLD,
                MANGA_TRACKING_COMPLETED,
            ),
        )

    fun whichCategoriesToUpdate() = this.preferenceStore.getStringSet("library_update_categories")

    fun whichCategoriesToExclude() =
        this.preferenceStore.getStringSet("library_update_categories_exclude")

    fun layout() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_display_library_layout",
            defaultValue = LibraryDisplayMode.ComfortableGrid,
            serializer = { it.toInt() },
            deserializer = { i -> LibraryDisplayMode.fromInt(i) },
        )

    fun sortingMode() = this.preferenceStore.getInt("library_sorting_mode")

    fun sortAscending() = this.preferenceStore.getBoolean("library_sorting_ascending", true)

    fun collapsedCategories() =
        this.preferenceStore.getStringSet("collapsed_categories", mutableSetOf())

    fun collapsedDynamicCategories() =
        this.preferenceStore.getStringSet("collapsed_dynamic_categories")

    fun collapsedDynamicAtBottom() = this.preferenceStore.getBoolean("collapsed_dynamic_at_bottom")

    fun unreadBadgeType() = this.preferenceStore.getInt("unread_badge_type", 2)

    fun showUnreadBadge() = this.preferenceStore.getBoolean("display_unread_badge", true)

    fun libraryHorizontalCategories() =
        this.preferenceStore.getBoolean("library_horizontal_categories", false)

    fun showLibraryButtonBar() = this.preferenceStore.getBoolean("show_library_button_bar", true)

    fun showCategoriesHeaderCount() = this.preferenceStore.getBoolean("display_number_of_items")

    fun showAllCategories() = this.preferenceStore.getBoolean("show_all_categories", true)

    fun showAllCategoriesWhenSearchingSingleCategory() =
        this.preferenceStore.getBoolean("show_all_categories_when_searching_single_category")

    fun lastUsedCategory() = this.preferenceStore.getInt("last_used_category")

    fun hopperGravity() = this.preferenceStore.getInt("hopper_gravity", 1)

    fun hopperLongPressAction() = this.preferenceStore.getInt("hopper_long_press")

    fun hideHopper() = this.preferenceStore.getBoolean("hide_hopper")

    fun autoHideHopper() = this.preferenceStore.getBoolean("autohide_hopper", true)

    fun groupBy() =
        this.preferenceStore.getObjectFromInt(
            key = "group_library_by",
            defaultValue = LibraryGroup.ByCategory,
            serializer = LibraryGroup::type,
            deserializer = LibraryGroup::fromInt,
        )

    fun skipMangaMetadataDuringUpdate() = this.preferenceStore.getBoolean("faster_library_updates")

    fun prioritizeLibraryUpdates() = this.preferenceStore.getBoolean("prioritize_library_updates")

    fun updateCovers() = this.preferenceStore.getBoolean("refresh_covers_too", true)

    fun outlineOnCovers() = this.preferenceStore.getBoolean("outline_on_covers", true)

    fun gridSize() = this.preferenceStore.getFloat("grid_size_float", 1f)

    fun uniformGrid() = this.preferenceStore.getBoolean("uniform_grid", true)

    fun staggeredGrid() = this.preferenceStore.getBoolean("use_staggered_grid")

    fun showStartReadingButton() = this.preferenceStore.getBoolean("start_reading_button", true)

    fun showDownloadBadge() = this.preferenceStore.getBoolean("display_download_badge")

    fun filterDownloaded() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_filter_downloaded_key",
            defaultValue = FilterDownloaded.Inactive,
            serializer = FilterDownloaded::toInt,
            deserializer = FilterDownloaded::fromInt,
        )

    fun filterUnread() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_filter_unread_key",
            defaultValue = FilterUnread.Inactive,
            serializer = FilterUnread::toInt,
            deserializer = FilterUnread::fromInt,
        )

    fun filterCompleted() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_filter_completed_key",
            defaultValue = FilterCompleted.Inactive,
            serializer = FilterCompleted::toInt,
            deserializer = FilterCompleted::fromInt,
        )

    fun filterBookmarked() =
        this.preferenceStore.getObjectFromInt(
            key = "pref_filter_bookmarked_key",
            defaultValue = FilterBookmarked.Inactive,
            serializer = FilterBookmarked::toInt,
            deserializer = FilterBookmarked::fromInt,
        )

    fun filterUnavailable() =
        this.preferenceStore.getObjectFromInt(
            "pref_filter_unavailable_key",
            defaultValue = FilterUnavailable.Inactive,
            serializer = FilterUnavailable::toInt,
            deserializer = FilterUnavailable::fromInt,
        )

    fun filterTracked() =
        this.preferenceStore.getObjectFromInt(
            "pref_filter_tracked_key",
            defaultValue = FilterTracked.Inactive,
            serializer = FilterTracked::toInt,
            deserializer = FilterTracked::fromInt,
        )

    fun filterMangaType() =
        this.preferenceStore.getObjectFromInt(
            "pref_filter_manga_type_key",
            defaultValue = FilterMangaType.Inactive,
            serializer = FilterMangaType::toInt,
            deserializer = FilterMangaType::fromInt,
        )

    fun filterMerged() =
        this.preferenceStore.getObjectFromInt(
            "pref_filter_merged_key",
            defaultValue = FilterMerged.Inactive,
            serializer = FilterMerged::toInt,
            deserializer = FilterMerged::fromInt,
        )

    fun filterMissingChapters() =
        this.preferenceStore.getObjectFromInt(
            "pref_filter_missing_chapters_key",
            defaultValue = FilterMissingChapters.Inactive,
            serializer = FilterMissingChapters::toInt,
            deserializer = FilterMissingChapters::fromInt,
        )

    fun removeArticles() = this.preferenceStore.getBoolean("remove_articles")

    fun chapterScanlatorFilterOption() =
        this.preferenceStore.getInt("chapter_scanlator_filter_option", 1)

    companion object {

        const val MANGA_NOT_COMPLETED = "manga_status_not_completed"
        const val MANGA_HAS_UNREAD = "manga_fully_read"
        const val MANGA_NOT_STARTED = "manga_not_started"
        const val MANGA_TRACKING_COMPLETED = "manga_tracking_completed"
        const val MANGA_TRACKING_DROPPED = "manga_tracking_dropped"
        const val MANGA_TRACKING_ON_HOLD = "manga_tracking_on_hold"
        const val MANGA_TRACKING_PLAN_TO_READ = "manga_tracking_plan_to_read"

        // Device
        const val DEVICE_NETWORK_NOT_METERED = "network_not_metered"
        const val DEVICE_CHARGING = "ac"
    }
}
