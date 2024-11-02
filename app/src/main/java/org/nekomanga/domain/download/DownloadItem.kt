package org.nekomanga.domain.download

import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.manga.SimpleManga

data class DownloadItem(val mangaItem: SimpleManga, val chapterItem: ChapterItem)
