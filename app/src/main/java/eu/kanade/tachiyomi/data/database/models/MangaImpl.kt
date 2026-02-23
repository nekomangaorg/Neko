package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.injectLazy

open class MangaImpl : Manga {

    override var id: Long? = null

    override var source: Long = -1

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var next_update: Long = 0

    override var initialized: Boolean = false

    override var viewer_flags: Int = -1

    override var chapter_flags: Int = 0

    override var date_added: Long = 0

    override var follow_status: FollowStatus? = null

    override var lang_flag: String? = null

    override var anilist_id: String? = null

    override var kitsu_id: String? = null

    override var my_anime_list_id: String? = null

    override var manga_updates_id: String? = null

    override var anime_planet_id: String? = null

    override var other_urls: String? = null

    override var filtered_scanlators: String? = null

    override var filtered_language: String? = null

    override var missing_chapters: String? = null

    override var rating: String? = null

    override var users: String? = null

    override var merge_manga_url: String? = null

    override var merge_manga_image_url: String? = null

    override var last_volume_number: Int? = null

    override var last_chapter_number: Int? = null

    override var alt_titles: String? = null

    override var user_cover: String? = null

    override var user_title: String? = null

    override var dynamic_cover: String? = null

    override var replies_count: String? = null

    override var thread_id: String? = null

    override fun copyFrom(other: SManga) {
        if (other is MangaImpl && other.title.isNotBlank() && other.title != title) {
            val oldTitle = title
            title = other.title

            if (user_title.isNullOrBlank() && oldTitle != title) {
                val downloadManager: DownloadManager by injectLazy()
                val storageManager: StorageManager by injectLazy()
                val provider = DownloadProvider(downloadManager.context)
                provider.renameMangaFolder(oldTitle, title)
                storageManager.renamePagesAndCoverDirectory(oldTitle, title)
                downloadManager.updateDownloadCacheForManga(this)
            }
        }
        super.copyFrom(other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val manga = other as Manga

        return url == manga.url
    }

    override fun hashCode(): Int {
        return if (::url.isInitialized) {
            url.hashCode()
        } else {
            (id ?: 0L).hashCode()
        }
    }
}
