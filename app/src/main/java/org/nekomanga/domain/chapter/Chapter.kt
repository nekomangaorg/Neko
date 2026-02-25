package org.nekomanga.domain.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants

data class SimpleChapter(
    val id: Long,
    val mangaId: Long,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Int,
    val dateFetch: Long,
    val sourceOrder: Int,
    val smartOrder: Int,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val chapterNumber: Float,
    val pagesLeft: Int,
    val volume: String,
    val chapterText: String,
    val chapterTitle: String,
    val language: String,
    val mangaDexChapterId: String,
    val oldMangaDexChapterId: String?,
    val scanlator: String,
    val uploader: String,
    val isUnavailable: Boolean,
    val lastRead: Long = 0L,
) {
    val isRecognizedNumber = chapterNumber >= 0f

    fun isMergedChapter() = MergeType.containsMergeSourceName(this.scanlator)

    fun isLocalSource() = this.scanlator == Constants.LOCAL_SOURCE && this.isUnavailable

    fun canDeleteChapter() = !this.isLocalSource() && !this.bookmark && !this.isUnavailable

    fun isMergedChapterOfType(mergeType: MergeType) =
        MergeType.getMergeTypeName(mergeType) == this.scanlator

    fun getHttpSource(sourceManager: SourceManager): HttpSource {
        val mergeType = MergeType.getMergeTypeFromName(this.scanlator)
        return when (mergeType == null) {
            true -> sourceManager.mangaDex
            false -> MergeType.getSource(mergeType, sourceManager)
        }
    }

    fun scanlatorList(): List<String> {
        return ChapterUtil.getScanlators(this.scanlator)
    }

    fun commentUrl(threadId: String): String {
        return MdConstants.forumUrl + threadId
    }

    fun toSChapter(): SChapter {
        return SChapter.create().also {
            it.url = url
            it.name = name
            it.date_upload = dateUpload
            it.chapter_number = chapterNumber
            it.scanlator = scanlator
            it.vol = volume
            it.chapter_txt = chapterText
            it.chapter_title = chapterTitle
            it.mangadex_chapter_id = mangaDexChapterId
            it.old_mangadex_id = oldMangaDexChapterId
            it.language = language
            it.uploader = uploader
        }
    }

    fun copyFromSChapter(sChapter: SChapter): SimpleChapter {
        return this.copy(
            name = sChapter.name,
            url = sChapter.url,
            dateUpload = sChapter.date_upload,
            chapterNumber = sChapter.chapter_number,
            scanlator = sChapter.scanlator ?: "",
            uploader = sChapter.uploader ?: "",
            isUnavailable = sChapter.isUnavailable,
            volume = sChapter.vol,
        )
    }

    companion object {
        fun create() =
            SimpleChapter(
                id = -1,
                mangaId = -1,
                read = false,
                bookmark = false,
                lastPageRead = 0,
                dateFetch = 0,
                sourceOrder = 0,
                smartOrder = 0,
                url = "",
                name = "",
                dateUpload = -1,
                chapterNumber = -1f,
                pagesLeft = 0,
                chapterText = "",
                chapterTitle = "",
                volume = "",
                scanlator = "",
                uploader = "",
                mangaDexChapterId = "",
                oldMangaDexChapterId = null,
                language = "",
                isUnavailable = false,
            )
    }

    fun toDbChapter(): Chapter =
        Chapter.create().also {
            it.id = id
            it.manga_id = mangaId
            it.url = url
            it.name = name
            it.scanlator = scanlator
            it.uploader = uploader
            it.read = read
            it.bookmark = bookmark
            it.last_page_read = lastPageRead
            it.pages_left = pagesLeft
            it.date_fetch = dateFetch
            it.date_upload = dateUpload
            it.chapter_number = chapterNumber
            it.source_order = sourceOrder
            it.smart_order = smartOrder
            it.language = language
            it.isUnavailable = isUnavailable
            it.vol = volume
            it.chapter_title = chapterTitle
            it.chapter_txt = chapterText
            it.mangadex_chapter_id = mangaDexChapterId
            it.old_mangadex_id = oldMangaDexChapterId
        }

    fun toChapterItem(
        downloadState: Download.State = Download.State.NOT_DOWNLOADED,
        downloadProgress: Int = 0,
    ): ChapterItem =
        ChapterItem(
            chapter = this,
            downloadState = downloadState,
            downloadProgress = downloadProgress,
        )
}

fun Chapter.toSimpleChapter(lastRead: Long = 0L): SimpleChapter? {
    if (id == null || manga_id == null) return null
    return SimpleChapter(
        id = id!!,
        mangaId = manga_id!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read,
        pagesLeft = pages_left,
        dateFetch = date_fetch,
        sourceOrder = source_order,
        smartOrder = smart_order,
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = chapter_number,
        scanlator = scanlator ?: "",
        uploader = uploader ?: "",
        volume = vol,
        chapterTitle = chapter_title,
        chapterText = chapter_txt,
        mangaDexChapterId = mangadex_chapter_id,
        oldMangaDexChapterId = old_mangadex_id,
        language = language ?: "",
        lastRead = lastRead,
        isUnavailable = isUnavailable,
    )
}

fun SimpleChapter.toChapterItem(): ChapterItem {
    return ChapterItem(chapter = this)
}

data class ChapterItem(
    val chapter: SimpleChapter,
    val downloadState: Download.State = Download.State.NOT_DOWNLOADED,
    val downloadProgress: Int = -1,
) {
    val isDownloaded = downloadState == Download.State.DOWNLOADED

    val isNotDownloaded = downloadState == Download.State.NOT_DOWNLOADED
}
