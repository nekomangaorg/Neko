package eu.kanade.tachiyomi.data.coil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias Decode = SharedWork<FullDecode<String>>.Run.() -> FullDecode<String>

class SharedFullDecodesTest {

    private val decodes = SharedFullDecodes<String>()
    private val callers = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runs = AtomicInteger()
    private val key = "1_0"

    @After
    fun tearDown() {
        callers.cancel()
    }

    private fun decoded(image: String): Decode = {
        runs.incrementAndGet()
        FullDecode.Decoded(image)
    }

    private fun failed(permanent: Boolean): Decode = {
        runs.incrementAndGet()
        FullDecode.Failed(permanent)
    }

    @Test
    fun `a decoded page is returned`() = runBlocking {
        assertEquals("page", decodes.await(key, retry = false, decode = decoded("page")))
    }

    @Test
    fun `a page whose decode failed for good is not decoded again`() = runBlocking {
        assertNull(decodes.await(key, retry = false, decode = failed(permanent = true)))

        assertNull(decodes.await(key, retry = false, decode = decoded("page")))
        assertEquals(1, runs.get())
    }

    @Test
    fun `a failed page does not stop other pages from decoding`() = runBlocking {
        decodes.await(key, retry = false, decode = failed(permanent = true))

        assertEquals(
            "other page",
            decodes.await("1_1", retry = false, decode = decoded("other page")),
        )
    }

    @Test
    fun `a page whose decode can succeed later is decoded again`() = runBlocking {
        assertNull(decodes.await(key, retry = false, decode = failed(permanent = false)))

        assertEquals("page", decodes.await(key, retry = false, decode = decoded("page")))
        assertEquals(2, runs.get())
    }

    @Test
    fun `a retry decodes a page whose decode failed for good`() = runBlocking {
        decodes.await(key, retry = false, decode = failed(permanent = true))

        assertEquals("page", decodes.await(key, retry = true, decode = decoded("page")))
        assertEquals(2, runs.get())
    }

    @Test
    fun `a retry that fails again marks the page again`() = runBlocking {
        decodes.await(key, retry = false, decode = failed(permanent = true))
        decodes.await(key, retry = true, decode = failed(permanent = true))

        assertNull(decodes.await(key, retry = false, decode = decoded("page")))
        assertEquals(2, runs.get())
    }

    @Test
    fun `clear forgets pages whose decode failed`() = runBlocking {
        decodes.await(key, retry = false, decode = failed(permanent = true))

        decodes.clear()

        assertEquals("page", decodes.await(key, retry = false, decode = decoded("page")))
    }

    @Test
    fun `a decode that fails after clear does not mark the page`() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val caller =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                decodes.await(key, retry = false) {
                    started.countDown()
                    release.await(10, TimeUnit.SECONDS)
                    FullDecode.Failed(permanent = true)
                }
            }
        assertTrue("decode did not start", started.await(5, TimeUnit.SECONDS))

        decodes.clear()
        release.countDown()

        assertNull(withTimeout(5_000) { caller.await() })
        assertEquals("page", decodes.await(key, retry = false, decode = decoded("page")))
    }
}
