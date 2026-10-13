package eu.kanade.tachiyomi.data.coil

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class SharedRegionDecodersTest {

    private class FakeDecoder(val bytes: ByteArray)

    private val created = AtomicInteger()
    private val recycled = mutableListOf<FakeDecoder>()
    private val decoders =
        SharedRegionDecoders<FakeDecoder>(recycle = { synchronized(recycled) { recycled += it } })

    private fun create(bytes: ByteArray): () -> FakeDecoder? = {
        created.incrementAndGet()
        FakeDecoder(bytes)
    }

    @Test
    fun `slices of the same file share one decoder`() {
        val bytes = ByteArray(8)

        decoders.use(bytes, create(bytes)) { first ->
            decoders.use(bytes, create(bytes)) { second -> assertSame(first, second) }
            assertEquals(emptyList<FakeDecoder>(), recycled)
        }

        assertEquals(1, created.get())
        assertEquals(1, recycled.size)
    }

    @Test
    fun `slices decoding at the same time on different threads share one decoder`() {
        val bytes = ByteArray(8)
        val users = 8
        // Every user waits inside its block until all of them are in, so they overlap.
        val allIn = CyclicBarrier(users)
        val seen = mutableSetOf<FakeDecoder?>()

        (1..users)
            .map {
                thread {
                    decoders.use(bytes, create(bytes)) { decoder ->
                        synchronized(seen) { seen += decoder }
                        allIn.await(5, TimeUnit.SECONDS)
                    }
                }
            }
            .forEach { it.join() }

        assertEquals(1, created.get())
        assertEquals(1, seen.size)
        assertEquals(seen.toList(), recycled)
    }

    @Test
    fun `a decoder is recycled after its last slice and the next slice creates a new one`() {
        val bytes = ByteArray(8)
        var first: FakeDecoder? = null
        var second: FakeDecoder? = null

        decoders.use(bytes, create(bytes)) { first = it }
        decoders.use(bytes, create(bytes)) { second = it }

        assertEquals(2, created.get())
        assertNotSame(first, second)
        assertEquals(listOf(first, second), recycled)
    }

    @Test
    fun `a file read again gets a decoder made from the new bytes`() {
        val oldBytes = ByteArray(8) { 1 }
        val newBytes = ByteArray(8) { 2 }

        decoders.use(oldBytes, create(oldBytes)) { old ->
            decoders.use(newBytes, create(newBytes)) { new -> assertSame(newBytes, new?.bytes) }
            assertSame(oldBytes, old?.bytes)
        }

        assertEquals(2, created.get())
    }

    @Test
    fun `a failed create gives every overlapping slice no decoder and is not retried`() {
        val bytes = ByteArray(8)
        val failed: () -> FakeDecoder? = {
            created.incrementAndGet()
            null
        }

        decoders.use(bytes, failed) { first ->
            assertNull(first)
            decoders.use(bytes, failed) { second -> assertNull(second) }
        }

        assertEquals(1, created.get())
        assertEquals(emptyList<FakeDecoder>(), recycled)
    }

    @Test
    fun `a create that throws does not keep the file's entry`() {
        val bytes = ByteArray(8)

        assertThrows(IllegalStateException::class.java) {
            decoders.use(bytes, { error("no decoder") }) {}
        }
        decoders.use(bytes, create(bytes)) {}

        assertEquals(1, created.get())
        assertEquals(1, recycled.size)
    }

    @Test
    fun `a block that throws recycles the decoder after the last user`() {
        val bytes = ByteArray(8)

        decoders.use(bytes, create(bytes)) {
            assertThrows(IllegalStateException::class.java) {
                decoders.use(bytes, create(bytes)) { error("decoding failure") }
            }
            assertEquals(emptyList<FakeDecoder>(), recycled)
        }

        assertEquals(1, created.get())
        assertEquals(1, recycled.size)
    }
}
