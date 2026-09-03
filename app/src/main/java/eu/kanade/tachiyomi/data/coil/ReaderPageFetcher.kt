package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import java.io.ByteArrayInputStream
import okio.buffer
import okio.source

class ReaderPageFetcher(private val page: ReaderPage, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val streamFn = page.stream ?: error("Page stream not available for page ${page.index}")
        val source = streamFn().source().buffer()
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
                val streamFn =
                    split.page.stream
                        ?: error("Page stream not available for page ${split.page.index}")
                streamFn().source().buffer()
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
