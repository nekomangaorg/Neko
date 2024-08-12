package org.nekomanga.domain.track.store

import android.content.Context
import androidx.core.content.edit
import org.nekomanga.logging.TimberKt

class DelayedTrackingStore(context: Context) {
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun add(trackId: Long, lastChapterRead: Float) {
        val previousLastChapterRead = preferences.getFloat(trackId.toString(), 0f)
        if (lastChapterRead > previousLastChapterRead) {
            TimberKt.d { "Queuing track item: $trackId, last chapter read: $lastChapterRead" }
            preferences.edit { putFloat(trackId.toString(), lastChapterRead) }
        }
    }

    fun remove(trackId: Long) {
        preferences.edit { remove(trackId.toString()) }
    }

    fun getItems(): List<DelayedTrackingItem> {
        return preferences.all.mapNotNull {
            DelayedTrackingItem(
                trackId = it.key.toLong(), lastChapterRead = it.value.toString().toFloat())
        }
    }

    data class DelayedTrackingItem(
        val trackId: Long,
        val lastChapterRead: Float,
    )
}
