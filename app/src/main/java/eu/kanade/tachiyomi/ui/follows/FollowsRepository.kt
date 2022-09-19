package eu.kanade.tachiyomi.ui.follows

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
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
        return mangaDex.fetchAllFollows()
            .andThen { sourceMap ->
                Ok(
                    sourceMap.entries.associate { entry ->
                        FollowStatus.fromInt(entry.key).stringRes to entry.value.map { it.toDisplayManga(db, mangaDex.id) }.toImmutableList()
                    }.toImmutableMap(),
                )
            }
    }
}
