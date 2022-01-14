package eu.kanade.tachiyomi.data.models

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SManga

data class DisplaySManga(val sManga: SManga, val displayText: String = "")

data class DisplayManga(val manga: Manga, val displayText: String = "")
