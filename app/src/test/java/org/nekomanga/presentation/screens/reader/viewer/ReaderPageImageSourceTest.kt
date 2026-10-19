package org.nekomanga.presentation.screens.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Size
import eu.kanade.tachiyomi.data.coil.maxBitmapBytesMemoryCacheKey
import eu.kanade.tachiyomi.util.system.GLUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import me.saket.telephoto.zoomable.ZoomableImageSource.PainterDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderPageImageSourceTest {

    private val imageLoader = mockk<ImageLoader>()
    private val request =
        ImageRequest.Builder(mockk<Context>(relaxed = true)).data("page_0").crossfade(true).build()
    private val bitmap =
        mockk<Bitmap> {
            every { width } returns 800
            every { height } returns 12000
        }

    @Test
    fun `a page in the memory cache is drawn without a second load`() = runTest {
        coEvery { imageLoader.execute(any()) } returns
            SuccessResult(
                image = bitmap.asImage(),
                request = request,
                dataSource = DataSource.MEMORY_CACHE,
            )

        val resolved = loadReaderPageImage(imageLoader, request)

        coVerify(exactly = 1) { imageLoader.execute(any()) }
        coVerify(exactly = 1) {
            imageLoader.execute(match { it.memoryCachePolicy == CachePolicy.ENABLED })
        }
        assertNotNull((resolved.delegate as PainterDelegate).painter)
        assertEquals(200.milliseconds, resolved.crossfadeDuration)
    }

    @Test
    fun `a decoded page is loaded once`() = runTest {
        coEvery { imageLoader.execute(any()) } returns
            SuccessResult(
                image = bitmap.asImage(),
                request = request,
                dataSource = DataSource.MEMORY,
            )

        val resolved = loadReaderPageImage(imageLoader, request)

        coVerify(exactly = 1) { imageLoader.execute(request) }
        assertNotNull((resolved.delegate as PainterDelegate).painter)
    }

    @Test
    fun `a failed page has no painter and no crossfade`() = runTest {
        coEvery { imageLoader.execute(any()) } returns
            ErrorResult(image = null, request = request, throwable = IllegalStateException())

        val resolved = loadReaderPageImage(imageLoader, request)

        coVerify(exactly = 1) { imageLoader.execute(request) }
        assertNull((resolved.delegate as PainterDelegate).painter)
        assertEquals(Duration.ZERO, resolved.crossfadeDuration)
    }

    @Test
    fun `page size limits keep the original target size so small pages are not scaled up`() =
        runTest {
            val limited =
                ImageRequest.Builder(mockk<Context>(relaxed = true))
                    .data("page_0")
                    .readerPageSizeLimits(maxTextureSize = 16383)
                    .build()

            assertEquals(Size.ORIGINAL, limited.sizeResolver.size())
        }

    @Test
    fun `page size limits cap pages at the texture size and the canvas byte limit`() {
        val limited =
            ImageRequest.Builder(mockk<Context>(relaxed = true))
                .data("page_0")
                .readerPageSizeLimits(maxTextureSize = 16383)
                .build()

        assertEquals(Size(16383, 16383), limited.maxBitmapSize)
        assertEquals(
            GLUtil.MAX_CANVAS_BITMAP_BYTES.toString(),
            limited.memoryCacheKeyExtras[maxBitmapBytesMemoryCacheKey],
        )
    }
}
