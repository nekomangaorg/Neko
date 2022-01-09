package eu.kanade.tachiyomi.data.models

import eu.kanade.tachiyomi.data.database.models.Manga

data class DisplayManga(val manga: Manga, val displayText: String? = null)
