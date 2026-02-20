package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.merged.InvalidHttpSource
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.source.online.merged.mangaball.MangaBall
import eu.kanade.tachiyomi.source.online.merged.projectsuki.ProjectSuki
import eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi
import eu.kanade.tachiyomi.source.online.merged.toonily.Toonily
import eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral
import eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDex
import eu.kanade.tachiyomi.source.online.utils.MdLang
import java.security.MessageDigest
import org.nekomanga.constants.Constants

/** Currently hardcoded to always return the same English [MangaDex] instance */
open class SourceManager {

    val mangaDex: MangaDex = MangaDex()

    val mangaBall: MangaBall by lazy { MangaBall() }

    val invalidMergeSource: InvalidHttpSource by lazy { InvalidHttpSource() }

    val komga: Komga by lazy { Komga() }

    val suwayomi: Suwayomi by lazy { Suwayomi() }

    val toonily: Toonily by lazy { Toonily() }

    val weebCentral: WeebCentral by lazy { WeebCentral() }

    val weebDex: WeebDex by lazy { WeebDex() }

    val projectSuki: ProjectSuki by lazy { ProjectSuki() }

    open fun get(sourceKey: Long): Source? {
        return mangaDex
    }

    fun isMangadex(sourceKey: Long): Boolean {
        return possibleIds.contains(sourceKey)
    }

    companion object {

        val mergeSourceNames =
            listOf<String>(
                Komga.name,
                Constants.LOCAL_SOURCE,
                Suwayomi.name,
                Toonily.name,
                WeebCentral.name,
                MangaBall.name,
                WeebDex.name,
                ProjectSuki.name,
            )

        val possibleIds = MdLang.entries.map { getId(it.lang) }

        fun getId(lang: String): Long {
            val key = "mangadex/$lang/1"
            val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and
                Long.MAX_VALUE
        }
    }
}
