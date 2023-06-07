package tachiyomi.core.network

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
