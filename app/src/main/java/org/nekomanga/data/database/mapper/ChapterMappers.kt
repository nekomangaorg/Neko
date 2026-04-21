package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import org.nekomanga.data.database.entity.ChapterEntity

fun ChapterEntity.toChapter(): ChapterImpl {
    return ChapterImpl().apply {
        id = this@toChapter.id
        manga_id = this@toChapter.mangaId
        url = this@toChapter.url
        name = this@toChapter.name
        chapter_txt = this@toChapter.chapterTxt
        chapter_title = this@toChapter.chapterTitle
        vol = this@toChapter.vol
        scanlator = this@toChapter.scanlator
        uploader = this@toChapter.uploader
        // Neko specific / legacy Chapter properties
        read = this@toChapter.read
        bookmark = this@toChapter.bookmark
        last_page_read = this@toChapter.lastPageRead
        pages_left = this@toChapter.pagesLeft
        chapter_number = this@toChapter.chapterNumber
        source_order = this@toChapter.sourceOrder
        smart_order = this@toChapter.smartOrder
        date_fetch = this@toChapter.dateFetch
        date_upload = this@toChapter.dateUpload
        mangadex_chapter_id = this@toChapter.mangadexChapterId ?: ""
        language = this@toChapter.language

        // (Note: isUnavailable is an extension property handled by Chapter.canDeleteChapter
        // or set via the source, it is not a direct mutable field on ChapterImpl)
    }
}

fun Chapter.toEntity(): ChapterEntity {
    return ChapterEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        mangaId = this.manga_id ?: 0L, // Safely falls back if detached
        url = this.url,
        name = this.name,
        chapterTxt = this.chapter_txt ?: "",
        chapterTitle = this.chapter_title ?: "",
        vol = this.vol ?: "",
        scanlator = this.scanlator,
        uploader = this.uploader,
        isUnavailable = this.isUnavailable, // Pulled from SChapter interface
        read = this.read,
        bookmark = this.bookmark,
        lastPageRead = this.last_page_read,
        pagesLeft = this.pages_left,
        chapterNumber = this.chapter_number,
        sourceOrder = this.source_order,
        smartOrder = this.smart_order,
        dateFetch = this.date_fetch,
        dateUpload = this.date_upload,
        mangadexChapterId = this.mangadex_chapter_id,
        language = this.language,
    )
}
