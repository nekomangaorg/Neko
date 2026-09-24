package eu.kanade.tachiyomi.data.coil

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedWorkTest {

    private val sharedWork = SharedWork<String>()
    private val callers = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = LinkedBlockingQueue<String>()
    private val releases = mutableListOf<CountDownLatch>()
    private val key = "1_0"

    /** Stands in for a full page decode, which blocks its thread and ignores cancellation. */
    private inner class BlockingWork(
        private val result: String,
        private val failure: Exception? = null,
    ) {
        private val release = CountDownLatch(1).also { releases += it }

        fun run(): String {
            started.put(result)
            release.await(10, TimeUnit.SECONDS)
            failure?.let { throw it }
            return result
        }

        fun finish() = release.countDown()
    }

    @After
    fun tearDown() {
        releases.forEach { it.countDown() }
        callers.cancel()
    }

    @Test
    fun `a waiting caller gets the result after the caller that started the work is cancelled`() =
        runBlocking {
            val work = BlockingWork("decoded")
            val starter =
                callers.launch(start = CoroutineStart.UNDISPATCHED) {
                    sharedWork.await(key) { work.run() }
                }
            assertEquals("decoded", started.poll(5, TimeUnit.SECONDS))
            val waiter =
                callers.async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching { sharedWork.await(key) { "second run" } }
                }

            starter.cancel()
            work.finish()

            val result = withTimeout(5_000) { waiter.await() }
            assertEquals("decoded", result.getOrElse { it.toString() })
        }

    @Test
    fun `no second run starts while work whose caller was cancelled is still running`() =
        runBlocking {
            val work = BlockingWork("decoded")
            val starter =
                callers.launch(start = CoroutineStart.UNDISPATCHED) {
                    sharedWork.await(key) { work.run() }
                }
            assertEquals("decoded", started.poll(5, TimeUnit.SECONDS))
            starter.cancel()

            val secondRun = BlockingWork("second run")
            val waiter =
                callers.async(start = CoroutineStart.UNDISPATCHED) {
                    sharedWork.await(key) { secondRun.run() }
                }

            assertNull(
                "run started while the first was still running",
                started.poll(500, TimeUnit.MILLISECONDS),
            )
            work.finish()
            assertEquals("decoded", withTimeout(5_000) { waiter.await() })
        }

    @Test
    fun `work that ends after clear does not remove the entry of newer work`() = runBlocking {
        val older = BlockingWork("older")
        val olderCaller =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                sharedWork.await(key) { older.run() }
            }
        assertEquals("older", started.poll(5, TimeUnit.SECONDS))
        sharedWork.clear()

        val newer = BlockingWork("newer")
        val newerCaller =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                sharedWork.await(key) { newer.run() }
            }
        assertEquals("newer", started.poll(5, TimeUnit.SECONDS))

        older.finish()
        assertEquals("older", withTimeout(5_000) { olderCaller.await() })

        val thirdRun = BlockingWork("third run")
        val lateCaller =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                sharedWork.await(key) { thirdRun.run() }
            }
        assertNull(
            "run started while newer work was still running",
            started.poll(500, TimeUnit.MILLISECONDS),
        )
        newer.finish()
        assertEquals("newer", withTimeout(5_000) { lateCaller.await() })
        assertEquals("newer", withTimeout(5_000) { newerCaller.await() })
    }

    @Test
    fun `a failure reaches every waiting caller and the next call starts new work`() = runBlocking {
        val work = BlockingWork("failing", failure = IOException("unreadable page"))
        val starter =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { sharedWork.await(key) { work.run() } }
            }
        assertEquals("failing", started.poll(5, TimeUnit.SECONDS))
        val waiter =
            callers.async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { sharedWork.await(key) { "second run" } }
            }

        work.finish()

        val starterFailure = withTimeout(5_000) { starter.await() }.exceptionOrNull()
        val waiterFailure = withTimeout(5_000) { waiter.await() }.exceptionOrNull()
        assertTrue("starter got $starterFailure", starterFailure is IOException)
        assertTrue("waiter got $waiterFailure", waiterFailure is IOException)
        assertEquals("retry", withTimeout(5_000) { sharedWork.await(key) { "retry" } })
    }

    @Test
    fun `finished work is not reused by the next call`() = runBlocking {
        assertEquals("first", sharedWork.await(key) { "first" })
        assertEquals("second", sharedWork.await(key) { "second" })
    }
}
