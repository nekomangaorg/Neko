package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

class ReaderPageFetcher(private val page: ReaderPage, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult {
        var streamFn = page.stream
        if (streamFn == null) {
            val loadJob =
                CoroutineScope(Dispatchers.IO).launch { page.chapter.pageLoader?.loadPage(page) }
            try {
                page.statusFlow.first { it == Page.State.READY || it == Page.State.ERROR }
            } finally {
                loadJob.cancel()
            }
            streamFn = page.stream
        }

        val actualStream = streamFn ?: error("Page stream not available for page ${page.index}")
        val source = actualStream().source().buffer()
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ReaderPage> {
        override fun create(
            data: ReaderPage,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ReaderPageFetcher(data, options)
        }
    }
}

class ReaderPageSplitFetcher(private val split: ReaderPageSplit, private val options: Options) :
    Fetcher {

    override suspend fun fetch(): FetchResult {
        val bytes = split.cachedBytes
        val source =
            if (bytes != null) {
                ByteArrayInputStream(bytes).source().buffer()
            } else {
                var streamFn = split.page.stream
                if (streamFn == null) {
                    val loadJob =
                        CoroutineScope(Dispatchers.IO).launch {
                            split.page.chapter.pageLoader?.loadPage(split.page)
                        }
                    try {
                        split.page.statusFlow.first {
                            it == Page.State.READY || it == Page.State.ERROR
                        }
                    } finally {
                        loadJob.cancel()
                    }
                    streamFn = split.page.stream
                }
                val actualStream =
                    streamFn ?: error("Page stream not available for page ${split.page.index}")
                actualStream().source().buffer()
            }
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ReaderPageSplit> {
        override fun create(
            data: ReaderPageSplit,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ReaderPageSplitFetcher(data, options)
        }
    }
}

class ReaderPageKeyer : Keyer<ReaderPage> {
    override fun key(data: ReaderPage, options: Options): String {
        return "reader_page_${data.chapter.chapter.id}_${data.index}"
    }
}

class ReaderPageSplitKeyer : Keyer<ReaderPageSplit> {
    override fun key(data: ReaderPageSplit, options: Options): String {
        return "reader_split_${data.page.chapter.chapter.id}_${data.page.index}_${data.topOffset}"
    }
}
