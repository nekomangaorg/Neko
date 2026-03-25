package eu.kanade.tachiyomi.ui.manga

sealed class TrackingUpdate {
    object Success : TrackingUpdate()

    data class Error(val message: String, val exception: Throwable) : TrackingUpdate()
}
