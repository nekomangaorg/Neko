package eu.kanade.tachiyomi.ui.reader.loader

sealed interface PreloadPageStatus {
    data object Idle : PreloadPageStatus

    data object DiskQueued : PreloadPageStatus

    data object DiskDownloading : PreloadPageStatus

    data object DiskReady : PreloadPageStatus

    data object MemoryDecoding : PreloadPageStatus

    data object MemoryReady : PreloadPageStatus

    data class Error(val cause: Throwable, val retryCount: Int = 0) : PreloadPageStatus
}

data class ReaderPreloadState(
    val activeIndex: Int = 0,
    val windowRange: IntRange = IntRange.EMPTY,
    val memoryRange: IntRange = IntRange.EMPTY,
    val pageStatuses: Map<String, PreloadPageStatus> = emptyMap(),
    val isIdle: Boolean = true,
)
