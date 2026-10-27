package org.nekomanga.domain.feed

import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.manga.Artwork

/** Domain model representing a manga entry in the feed. */
data class FeedItem(
    val mangaId: Long,
    val mangaTitle: String,
    val date: Long,
    val artwork: Artwork,
    val chapters: List<ChapterItem>,
    val lastReadChapter: String = "",
)
