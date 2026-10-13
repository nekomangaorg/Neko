package eu.kanade.tachiyomi.data.coil

import java.util.IdentityHashMap

/**
 * Region decoders shared by the slices of a page that decode while another slice of the same file
 * is decoding. BitmapRegionDecoder copies the whole file into native memory when it is created, so
 * one decoder per slice made 31 copies of an 87 MB page at once and the system killed the app. A
 * shared BitmapRegionDecoder is safe because it runs one decode at a time under its own lock
 * (mNativeLock in AOSP). The cost is that the slices of one page decode one after another. The 32
 * slices of a 750x129357 PNG took about 22 s on an emulator, and memory peaked around 900 MB, where
 * one decoder per slice got the app killed at 1.1 to 2.1 GB.
 *
 * Decoders are keyed by the byte array they were made from, so bytes read again after a retry get a
 * new decoder. The last slice to finish with a decoder recycles it.
 */
internal class SharedRegionDecoders<D : Any>(private val recycle: (D) -> Unit) {

    private class Entry<D : Any>(create: () -> D?) {
        // Lazy does not keep an exception, so a create that throws runs again for the next slice.
        val decoder = lazy(create)
        var users = 0
    }

    private val entries = IdentityHashMap<ByteArray, Entry<D>>()

    /**
     * Runs [block] with the decoder for [bytes], made by [create] unless another slice is using
     * one. [block] gets null when [create] gave no decoder.
     */
    fun <R> use(bytes: ByteArray, create: () -> D?, block: (D?) -> R): R {
        val entry =
            synchronized(entries) { entries.getOrPut(bytes) { Entry(create) }.also { it.users++ } }
        try {
            return block(entry.decoder.value)
        } finally {
            val last =
                synchronized(entries) {
                    entry.users--
                    (entry.users == 0).also {
                        if (it && entries[bytes] === entry) entries.remove(bytes)
                    }
                }
            if (last && entry.decoder.isInitialized()) {
                entry.decoder.value?.let(recycle)
            }
        }
    }
}
