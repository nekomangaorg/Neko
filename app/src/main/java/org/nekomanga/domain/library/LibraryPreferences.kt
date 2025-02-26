package org.nekomanga.domain.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import tachiyomi.core.preference.PreferenceStore

class LibraryPreferences(private val preferenceStore: PreferenceStore) {

    fun defaultCategory() = this.preferenceStore.getInt("default_category", -1)

    fun searchSuggestions() = this.preferenceStore.getString("library_search_suggestion")

    fun showSearchSuggestions() = this.preferenceStore.getBoolean("show_library_search_suggestions")

    fun lastSearchSuggestion() = this.preferenceStore.getLong("last_library_suggestion")

    fun updateInterval() = this.preferenceStore.getInt("pref_library_update_interval_key", 24)

    fun lastUpdateTimestamp() = this.preferenceStore.getLong("library_update_last_timestamp")

    fun lastUpdateAttemptTimestamp() =
        this.preferenceStore.getLong("library_update_last_attempt_timestamp")

    fun libraryUpdateIds() = this.preferenceStore.getString("library_update_ids")

    fun autoUpdateDeviceRestrictions() =
        this.preferenceStore.getStringSet("library_update_restriction")

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

    fun updatePrioritization() = this.preferenceStore.getInt("library_update_prioritization")

    fun layout() =
        this.preferenceStore.getInt(
            "pref_display_library_layout",
            LibraryItem.LAYOUT_COMFORTABLE_GRID,
        )

    fun sortingMode() = this.preferenceStore.getInt("library_sorting_mode")

    fun sortAscending() = this.preferenceStore.getBoolean("library_sorting_ascending", true)

    fun collapsedCategories() =
        this.preferenceStore.getStringSet("collapsed_categories", mutableSetOf())

    fun collapsedDynamicCategories() =
        this.preferenceStore.getStringSet("collapsed_dynamic_categories")

    fun collapsedDynamicAtBottom() = this.preferenceStore.getBoolean("collapsed_dynamic_at_bottom")

    fun unreadBadgeType() = this.preferenceStore.getInt("unread_badge_type", 2)

    fun showCategoriesHeaderCount() = this.preferenceStore.getBoolean("display_number_of_items")

    fun showAllCategories() = this.preferenceStore.getBoolean("show_all_categories", true)

    fun showAllCategoriesWhenSearchingSingleCategory() =
        this.preferenceStore.getBoolean("show_all_categories_when_searching_single_category")

    fun lastUsedCategory() = this.preferenceStore.getInt("last_used_category")

    fun hopperGravity() = this.preferenceStore.getInt("hopper_gravity", 1)

    fun filterOrder() =
        this.preferenceStore.getString("filter_order", FilterBottomSheet.Filters.DEFAULT_ORDER)

    fun hopperLongPressAction() = this.preferenceStore.getInt("hopper_long_press")

    fun hideHopper() = this.preferenceStore.getBoolean("hide_hopper")

    fun autoHideHopper() = this.preferenceStore.getBoolean("autohide_hopper", true)

    fun groupBy() = this.preferenceStore.getInt("group_library_by")

    fun showCategoryInTitle() = this.preferenceStore.getBoolean("category_in_title")

    fun updateFaster() = this.preferenceStore.getBoolean("faster_library_updates")

    fun updateCovers() = this.preferenceStore.getBoolean("refresh_covers_too", true)

    fun outlineOnCovers() = this.preferenceStore.getBoolean("outline_on_covers", true)

    fun gridSize() = this.preferenceStore.getFloat("grid_size_float", 1f)

    fun uniformGrid() = this.preferenceStore.getBoolean("uniform_grid", true)

    fun staggeredGrid() = this.preferenceStore.getBoolean("use_staggered_grid")

    fun hideStartReadingButton() = this.preferenceStore.getBoolean("hide_reading_button")

    fun showDownloadBadge() = this.preferenceStore.getBoolean("display_download_badge")

    fun filterDownloaded() = this.preferenceStore.getInt("pref_filter_downloaded_key")

    fun filterUnread() = this.preferenceStore.getInt("pref_filter_unread_key")

    fun filterCompleted() = this.preferenceStore.getInt("pref_filter_completed_key")

    fun filterBookmarked() = this.preferenceStore.getInt("pref_filter_bookmarked_key")

    fun filterTracked() = this.preferenceStore.getInt("pref_filter_tracked_key")

    fun filterMangaType() = this.preferenceStore.getInt("pref_filter_manga_type_key")

    fun filterMerged() = this.preferenceStore.getInt("pref_filter_merged_key")

    fun filterMissingChapters() = this.preferenceStore.getInt("pref_filter_missing_chapters_key")

    fun showEmptyCategoriesWhileFiltering() =
        this.preferenceStore.getBoolean("show_empty_categories_filtering")

    fun defaultMangaOrder() = this.preferenceStore.getString("default_manga_order")

    fun removeArticles() = this.preferenceStore.getBoolean("remove_articles")

    fun whatToUpdateOnRefresh() = this.preferenceStore.getInt("update_on_refresh", -1)

    companion object {

        const val MANGA_NOT_COMPLETED = "manga_status_not_completed"
        const val MANGA_HAS_UNREAD = "manga_fully_read"
        const val MANGA_NOT_STARTED = "manga_not_started"
        const val MANGA_TRACKING_COMPLETED = "manga_tracking_completed"
        const val MANGA_TRACKING_DROPPED = "manga_tracking_dropped"
        const val MANGA_TRACKING_ON_HOLD = "manga_tracking_on_hold"
        const val MANGA_TRACKING_PLAN_TO_READ = "manga_tracking_plan_to_read"

        // Device
        const val DEVICE_ONLY_ON_WIFI = "wifi"
        const val DEVICE_CHARGING = "ac"
        const val DEVICE_BATTERY_NOT_LOW = "battery_not_low"
    }
}
