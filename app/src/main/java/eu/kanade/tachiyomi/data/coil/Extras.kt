package eu.kanade.tachiyomi.data.coil

import coil3.Extras
import coil3.request.ImageRequest

// Define the key
val DynamicCoverKey = Extras.Key(default = false)

// Extension for your Compose ImageRequest
fun ImageRequest.Builder.dynamicCover(enable: Boolean) = apply {
    extras[DynamicCoverKey] = enable
    // Ensure Coil's memory cache distinguishes between dynamic and default covers
    memoryCacheKeyExtra("dynamic_covers", enable.toString())
}
