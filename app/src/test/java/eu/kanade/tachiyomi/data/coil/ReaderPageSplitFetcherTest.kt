package eu.kanade.tachiyomi.data.coil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageSplitFetcherTest {

    private val heapSizes = listOf(192L, 256L, 512L, 576L, 1024L, 2048L).map { it * MIB }

    @Test
    fun `full decode of a tall page fits in the fallback bitmap cache`() {
        val pages =
            listOf(720 to 8_000, 800 to 16_000, 1080 to 20_000, 1440 to 40_000, 4000 to 60_000)
        for (maxMemory in heapSizes) {
            val cacheMax = fallbackBitmapCacheMaxBytes(maxMemory)
            for ((width, height) in pages) {
                val sampleSize = fallbackDecodeSampleSize(width, height, maxMemory)
                val bytes = roundedUp(width, sampleSize) * roundedUp(height, sampleSize) * 4
                assertTrue(
                    "${width}x$height at sample size $sampleSize is $bytes bytes, " +
                        "cache max $cacheMax at a ${maxMemory / MIB} MiB heap",
                    bytes <= cacheMax,
                )
            }
        }
    }

    @Test
    fun `page under the decode budget keeps full resolution`() {
        // 86.4 MB and 64.8 MB as ARGB_8888. A budget of maxMemory / 8 would decode both at
        // sample size 2.
        assertEquals(1, fallbackDecodeSampleSize(1080, 20_000, 576L * MIB))
        assertEquals(1, fallbackDecodeSampleSize(1080, 15_000, 256L * MIB))
    }

    @Test
    fun `decode budget is a quarter of a small heap`() {
        assertEquals(32 * MIB, fallbackBitmapCacheMaxBytes(128L * MIB).toLong())
        assertEquals(48 * MIB, fallbackBitmapCacheMaxBytes(192L * MIB).toLong())
    }

    @Test
    fun `decode byte count rounds sampled dimensions up`() {
        // 2305x65535 at sample size 4 is at most 577x16384.
        assertEquals(577L * 16_384 * 4, fallbackDecodeBytes(2305, 65_535, 4))
    }

    @Test
    fun `sample size allows for sampled dimensions rounding up`() {
        // BitmapFactory decoded a 2305x65535 GIF at sample size 2 to 1153x32768, 151126016 bytes,
        // over the 150994944 byte budget of a 576 MiB heap. Rounded down it would fit.
        assertEquals(4, fallbackDecodeSampleSize(2305, 65_535, 576L * MIB))
    }

    @Test
    fun `sample size math does not overflow at Int max dimensions`() {
        assertEquals(64, fallbackDecodeSampleSize(Int.MAX_VALUE, 1, 576L * MIB))
        assertEquals(524_288, fallbackDecodeSampleSize(Int.MAX_VALUE, Int.MAX_VALUE, 576L * MIB))
    }

    @Test
    fun `native decode that succeeds is not retried`() {
        val events = mutableListOf<String>()

        val result =
            decodeOrRetrySmaller(
                sampleSize = 2,
                beforeRetry = { events += "retry" },
                decode = { sampleSize ->
                    "bitmap at $sampleSize".also { events += "decode $sampleSize" }
                },
            )

        assertEquals("bitmap at 2", result)
        assertEquals(listOf("decode 2"), events)
    }

    @Test
    fun `native decode that returns null is retried at double sample size`() {
        val events = mutableListOf<String>()

        val result =
            decodeOrRetrySmaller(
                sampleSize = 2,
                beforeRetry = { events += "retry" },
                decode = { sampleSize ->
                    events += "decode $sampleSize"
                    if (sampleSize == 4) "bitmap at 4" else null
                },
            )

        assertEquals("bitmap at 4", result)
        assertEquals(listOf("decode 2", "retry", "decode 4"), events)
    }

    @Test
    fun `native decode that returns null twice gives null`() {
        val events = mutableListOf<String>()

        val result =
            decodeOrRetrySmaller<String>(
                sampleSize = 1,
                beforeRetry = { events += "retry" },
                decode = { sampleSize ->
                    events += "decode $sampleSize"
                    null
                },
            )

        assertNull(result)
        assertEquals(listOf("decode 1", "retry", "decode 2"), events)
    }

    private fun roundedUp(size: Int, sampleSize: Int): Long =
        (size.toLong() + sampleSize - 1) / sampleSize

    private companion object {
        const val MIB = 1024L * 1024
    }
}
