package eu.kanade.tachiyomi.ui.main

object DeepLinks {
    object Extras {
        const val MangaId = "mangaId"
        const val NotificationId = "notificationId"
        const val GroupId = "groupId"

        const val AppUpdateNotes = "notes"

        const val AppUpdateUrl = "DownloadUrl"

        const val AppUpdateReleaseUrl = "releaseUrl"

        const val AppUpdateVersion = "version"
    }

    object Intents {
        const val Search = "neko.Search"
        const val SearchQuery = "query"
        const val SearchFilter = "filter"
    }

    object Actions {
        const val Library = "eu.kanade.tachiyomi.SHOW_LIBRARY"
        const val RecentlyUpdated = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"
        const val RecentlyRead = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"
        const val Browse = "eu.kanade.tachiyomi.SHOW_BROWSE"
        const val Downloads = "eu.kanade.tachiyomi.SHOW_DOWNLOADS"
        const val Manga = "eu.kanade.tachiyomi.SHOW_MANGA"
        const val MangaBack = "eu.kanade.tachiyomi.SHOW_MANGA_BACK"
        const val UpdateNotes = "eu.kanade.tachiyomi.SHOW_UPDATE_NOTES"
        const val Source = "eu.kanade.tachiyomi.SHOW_SOURCE"
        const val ReaderSettings = "eu.kanade.tachiyomi.READER_SETTINGS"
    }
}
