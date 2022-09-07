package eu.kanade.tachiyomi.ui.follows

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FollowsRepository {
    private val mangaDex: MangaDex by lazy { Injekt.get<SourceManager>().getMangadex() }
    private val db: DatabaseHelper = Injekt.get()

    suspend fun fetchFollows(): Result<ImmutableMap<Int, ImmutableList<DisplayManga>>, ResultError> {
        val result = mangaDex.fetchAllFollows()
        result.getError()?.let {
            return Err(it)
        }

        return Ok(
            result.get()!!.entries.map { entry ->
                entry.key to entry.value.map { it.toDisplayManga(db, mangaDex.id) }.toImmutableList()
            }.toMap().toImmutableMap(),
        )
    }
}
