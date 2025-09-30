package eu.kanade.tachiyomi.data.preference

/** This class stores the keys for the preferences in the application. */
object PreferenceKeys {

    const val theme = "pref_theme_key"

    const val nightMode = "night_mode"
    const val lightTheme = "light_theme"
    const val darkTheme = "dark_theme"

    const val startingTab = "starting_tab"

    const val backToStart = "back_to_start"

    const val deniedA11FilePermission = "denied_a11_file_permission"

    const val showNavigationOverlayNewUser = "reader_navigation_overlay_new_user"

    const val showNavigationOverlayNewUserWebtoon = "reader_navigation_overlay_new_user_webtoon"

    const val catalogueAsList = "pref_display_catalogue_as_list"

    const val catalogueShowLibrary = "pref_display_catalogue_already_in_library"
    const val catalogueDisplayMode = "catalogue_display_mode"

    const val removeAfterReadSlots = "remove_after_read_slots"

    const val deleteRemovedChapters = "delete_removed_chapters"

    const val dateFormat = "app_date_format"
    const val showDLsInRecents = "show_dls_in_recents"
    const val showRemHistoryInRecents = "show_rem_history_in_recents"
    const val showReadInAllRecents = "show_read_in_all_recents"
    const val showTitleFirstInRecents = "show_title_first_in_recents"

    const val groupChaptersHistory = "group_chapters_history"

    const val historyChapterGrouping = "history_chapter_grouping"

    const val showUpdatedTime = "show_updated_time"
    const val groupChaptersUpdates = "group_chapters_updates"

    const val sideNavIconAlignment = "pref_side_nav_icon_alignment"

    const val showSeriesInShortcuts = "show_series_shortcuts"
    const val openChapterInShortcuts = "open_chapter_shortcuts"

    const val sideNavMode = "side_nav_mode"

    const val shouldAutoUpdate = "should_auto_update"

    fun trackUsername(syncId: Int) = "pref_mangasync_username_$syncId"

    fun trackPassword(syncId: Int) = "pref_mangasync_password_$syncId"

    fun trackToken(syncId: Int) = "track_token_$syncId"

    fun sourceUsername(sourceId: Long) = "pref_source_username_$sourceId"

    fun sourcePassword(sourceId: Long) = "pref_source_password_$sourceId"

    fun sourceUrl(sourceId: Long) = "pref_source_url_$sourceId"

    const val autoAddTracker = "auto_add_tracker"

    const val thumbnailQuality = "mangadex_thumbnail_quality"
}
