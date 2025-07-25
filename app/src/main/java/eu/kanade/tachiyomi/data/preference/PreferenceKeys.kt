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

    const val autoUpdateTrack = "pref_auto_update_manga_sync_key"

    const val trackMarkedAsRead = "track_marked_as_read"

    const val catalogueAsList = "pref_display_catalogue_as_list"

    const val catalogueShowLibrary = "pref_display_catalogue_already_in_library"

    const val downloadOnlyOverWifi = "pref_download_only_over_wifi_key"

    const val folderPerManga = "create_folder_per_manga"

    const val removeAfterReadSlots = "remove_after_read_slots"

    const val deleteRemovedChapters = "delete_removed_chapters"

    const val removeAfterMarkedAsRead = "pref_remove_after_marked_as_read_key"

    const val blockedScanlators = "blocked_scanlators"

    const val langToShow = "mangadex_languages_new"

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

    const val saveChaptersAsCBZ = "save_chapter_as_cbz"

    fun trackUsername(syncId: Int) = "pref_mangasync_username_$syncId"

    fun trackPassword(syncId: Int) = "pref_mangasync_password_$syncId"

    fun trackToken(syncId: Int) = "track_token_$syncId"

    const val sessionToken = "mangadex_session_token"

    const val refreshToken = "mangadex_refresh_token"

    const val lastRefreshTokenTime = "mangadex_refresh_token_time"

    const val mangadexCodeVerifier = "mangadex_code_verifier"

    const val mangadexUserName = "mangadex_username"

    fun sourceUsername(sourceId: Long) = "pref_source_username_$sourceId"

    fun sourcePassword(sourceId: Long) = "pref_source_password_$sourceId"

    fun sourceUrl(sourceId: Long) = "pref_source_url_$sourceId"

    const val contentRating = "content_rating_options"

    const val autoTrackContentRating = "auto_track_content_rating_options"

    const val coverQuality = "coverQuality"

    const val dataSaver = "data_saver_bool"

    const val includeUnavailable = "include_unavailable"

    const val readingSync = "reading_sync_bool"

    const val showContentRatingFilter = "show_R18_filter"

    const val autoAddTracker = "auto_add_tracker"

    const val enablePort443Only = "use_port_443_only_for_image_server"

    const val addToLibraryAsPlannedToRead = "add_to_libray_as_planned_to_read"
    const val autoAddToMangadexLibrary = "auto_add_to_mangadex_library"

    const val mangadexSyncToLibraryIndexes = "pref_mangadex_sync_to_library_indexes"

    const val thumbnailQuality = "mangadex_thumbnail_quality"

    const val savedFilters = "browse_saved_filters"

    const val chapterScanlatorFilterOption = "chapter__scanlator_filter_option"
}
