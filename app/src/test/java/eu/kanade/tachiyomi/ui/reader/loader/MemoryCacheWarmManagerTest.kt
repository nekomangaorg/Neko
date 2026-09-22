package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import coil3.ImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryCacheWarmManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val imageLoader = mockk<ImageLoader>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var warmManager: MemoryCacheWarmManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        warmManager =
            MemoryCacheWarmManager(
                context = context,
                imageLoaderProvider = { imageLoader },
                maxTextureSizeProvider = { 4096 },
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `warmMemoryCache enqueues ImageRequest into ImageLoader and tracks Disposable`() {
        val disposable = mockk<Disposable>(relaxed = true)
        every { disposable.isDisposed } returns false
        every { imageLoader.enqueue(any()) } returns disposable

        warmManager.warmMemoryCache(key = "page_1", data = "http://example.com/image.jpg")

        verify(exactly = 1) { imageLoader.enqueue(any<ImageRequest>()) }
        assertTrue(warmManager.isWarming("page_1"))
        assertEquals(1, warmManager.activeCount())
    }

    @Test
    fun `cancel disposes active disposable and removes it from tracking`() {
        val disposable = mockk<Disposable>(relaxed = true)
        every { disposable.isDisposed } returns false
        every { imageLoader.enqueue(any()) } returns disposable

        warmManager.warmMemoryCache(key = "page_1", data = "http://example.com/image.jpg")
        warmManager.cancel("page_1")

        verify(exactly = 1) { disposable.dispose() }
        assertFalse(warmManager.isWarming("page_1"))
        assertEquals(0, warmManager.activeCount())
    }

    @Test
    fun `cancelAllExcept disposes disposables not in retained set`() {
        val disp1 = mockk<Disposable>(relaxed = true)
        val disp2 = mockk<Disposable>(relaxed = true)
        val disp3 = mockk<Disposable>(relaxed = true)
        every { disp1.isDisposed } returns false
        every { disp2.isDisposed } returns false
        every { disp3.isDisposed } returns false

        every { imageLoader.enqueue(any()) } returns disp1 andThen disp2 andThen disp3

        warmManager.warmMemoryCache(key = "page_1", data = "item1")
        warmManager.warmMemoryCache(key = "page_2", data = "item2")
        warmManager.warmMemoryCache(key = "page_3", data = "item3")

        assertEquals(3, warmManager.activeCount())

        warmManager.cancelAllExcept(setOf("page_2"))

        verify(exactly = 1) { disp1.dispose() }
        verify(exactly = 0) { disp2.dispose() }
        verify(exactly = 1) { disp3.dispose() }

        assertFalse(warmManager.isWarming("page_1"))
        assertTrue(warmManager.isWarming("page_2"))
        assertFalse(warmManager.isWarming("page_3"))
        assertEquals(1, warmManager.activeCount())
    }

    @Test
    fun `release disposes all active disposables and clears map`() {
        val disp1 = mockk<Disposable>(relaxed = true)
        val disp2 = mockk<Disposable>(relaxed = true)
        every { disp1.isDisposed } returns false
        every { disp2.isDisposed } returns false

        every { imageLoader.enqueue(any()) } returns disp1 andThen disp2

        warmManager.warmMemoryCache(key = "page_1", data = "item1")
        warmManager.warmMemoryCache(key = "page_2", data = "item2")

        warmManager.release()

        verify(exactly = 1) { disp1.dispose() }
        verify(exactly = 1) { disp2.dispose() }
        assertEquals(0, warmManager.activeCount())
    }

    @Test
    fun `warmMemoryCache triggers onError when ImageLoader is unavailable`() {
        val unconfiguredManager =
            MemoryCacheWarmManager(
                context = context,
                imageLoaderProvider = { null },
                maxTextureSizeProvider = { 4096 },
            )

        var errorCalled = false
        var capturedError: Throwable? = null

        unconfiguredManager.warmMemoryCache(
            key = "page_1",
            data = "item1",
            onError = {
                errorCalled = true
                capturedError = it
            },
        )

        assertTrue(errorCalled)
        assertTrue(capturedError is IllegalStateException)
        assertEquals(0, unconfiguredManager.activeCount())
    }
}
