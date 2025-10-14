package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.zip
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.uuid
import eu.kanade.tachiyomi.source.online.models.dto.AggregateVolume
import eu.kanade.tachiyomi.source.online.models.dto.ChapterDataDto
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.models.dto.ForumThreadDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.util.throws
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.nekomanga.constants.Constants.NO_GROUP
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaHandler {
    private val artworkHandler: ArtworkHandler by injectLazy()
    val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    val authService: MangaDexAuthorizedUserService by lazy {
        Injekt.get<NetworkServices>().authService
    }
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()
    private val apiMangaParser: ApiMangaParser by injectLazy()

    suspend fun fetchMangaAndChapterDetails(
        manga: SManga,
        fetchArtwork: Boolean,
    ): Result<MangaDetailChapterInformation, ResultError> {
        TimberKt.d { "fetch manga and chapter details" }

        return withContext(Dispatchers.IO) {
            val detailsManga =
                withContext(Dispatchers.Default) { fetchMangaDetails(manga.uuid(), fetchArtwork) }
            val chapterList =
                withContext(Dispatchers.Default) {
                    fetchChapterList(
                        manga.uuid(),
                        manga.last_chapter_number,
                        manga.last_volume_number,
                        mangaDexPreferences.includeUnavailableChapters().get(),
                    )
                }

            return@withContext zip(
                { detailsManga },
                { chapterList },
                { (manga, artwork), list -> MangaDetailChapterInformation(manga, artwork, list) },
            )
        }
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return withContext(Dispatchers.IO) {
            service
                .viewChapter(urlChapterId)
                .onFailure {
                    val type = "trying to get manga id from chapter id"
                    this.log(type)
                    this.throws(type)
                }
                .getOrThrow()
                .data
                .relationships
                .first { it.type == MdConstants.Types.manga }
                .id
        }
    }

    suspend fun fetchMangaDetails(
        mangaUUID: String,
        fetchArtwork: Boolean,
    ): Result<Pair<SManga, List<SourceArtwork>>, ResultError> {
        return withContext(Dispatchers.IO) {
            val artworks = artworkAsync(mangaUUID, fetchArtwork)
            val stats = statsAsync(mangaUUID)
            val manga = mangaAsync(mangaUUID)

            manga
                .await()
                .andThen { mangaDto ->
                    apiMangaParser.mangaDetailsParse(mangaDto.data, stats.await())
                }
                .andThen { sManga -> Ok(sManga to artworks.await()) }
        }
    }

    private fun CoroutineScope.simpleChaptersAsync(mangaUUID: String) = async {
        service
            .aggregateChapters(mangaUUID, MdUtil.getLangsToShow(mangaDexPreferences))
            .getOrResultError("trying to aggregate for $mangaUUID")
            .mapBoth(
                success = { aggregateDto ->
                    aggregateDto.volumes
                        .asMdMap<AggregateVolume>()
                        .values
                        .flatMap { it.chapters.values }
                        .map { it.chapter }
                },
                failure = { emptyList() },
            )
    }

    private fun CoroutineScope.mangaAsync(mangaUUID: String) = async {
        service.viewManga(mangaUUID).getOrResultError("Error getting Manga Detail")
    }

    private fun CoroutineScope.artworkAsync(mangaUUID: String, fetchArtwork: Boolean) = async {
        when (fetchArtwork) {
            true -> artworkHandler.getArtwork(mangaUUID).getOrElse { emptyList() }
            false -> emptyList()
        }
    }

    private fun CoroutineScope.statsAsync(mangaUUID: String) = async {
        service
            .mangaStatistics(mangaUUID)
            .getOrResultError("trying to get stats for $mangaUUID")
            .mapBoth(
                success = { statResult ->
                    val stats = statResult.statistics[mangaUUID]
                    when (stats == null) {
                        true -> Stats()
                        false -> {
                            val rating = stats.rating?.bayesian ?: 0.0
                            val strRating =
                                when (rating > 0) {
                                    true -> rating.toString()
                                    false -> null
                                }
                            Stats(
                                rating = strRating ?: "",
                                follows = stats.follows.toString(),
                                threadId = stats.comments?.threadId?.toString() ?: "",
                                repliesCount = stats.comments?.repliesCount?.toString() ?: "",
                            )
                        }
                    }
                },
                failure = { Stats() },
            )
    }

    suspend fun fetchChapterList(
        mangaUUID: String,
        lastChapterNumber: Int?,
        lastVolumeNumber: Int?,
        includeUnavailable: Boolean,
    ): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            val langs = MdUtil.getLangsToShow(mangaDexPreferences)

            fetchOffset(mangaUUID, langs, includeUnavailable, 0)
                .andThen { chapterListDto ->
                    Ok(
                        when (chapterListDto.total > chapterListDto.limit) {
                            true ->
                                chapterListDto.data +
                                    fetchRestOfChapters(
                                        mangaUUID,
                                        langs,
                                        includeUnavailable,
                                        chapterListDto.limit,
                                        chapterListDto.total,
                                    )

                            false -> chapterListDto.data
                        }
                    )
                }
                .andThen { results ->
                    val groupMap = getGroupMap(results)
                    val uploaderMap = getUploaderMap(results, groupMap)

                    apiMangaParser.chapterListParse(
                        lastChapterNumber,
                        lastVolumeNumber,
                        results,
                        groupMap,
                        uploaderMap,
                    )
                }
        }
    }

    private suspend fun fetchRestOfChapters(
        mangaUUID: String,
        langs: List<String>,
        includeUnavailable: Boolean,
        limit: Int,
        total: Int,
    ): List<ChapterDataDto> {
        return withContext(Dispatchers.IO) {
            val totalRequestNo = (total / limit)

            (1..totalRequestNo)
                .map { pos ->
                    async { fetchOffset(mangaUUID, langs, includeUnavailable, pos * limit) }
                }
                .awaitAll()
                .mapNotNull { it.getOrElse { null }?.data }
                .flatten()
        }
    }

    suspend fun fetchChapterCommentId(chapterUUID: String): Result<String?, ResultError> {
        return service
            .chapterStatistics(chapterUUID)
            .getOrResultError("Trying to get chapter comments")
            .andThen {
                Ok(
                    it.statistics[chapterUUID]?.comments?.threadId?.toString()
                        ?: return@andThen Err("No thread exists")
                )
            }
            .orElse {
                authService
                    .createForumThread(ForumThreadDto(chapterUUID, "chapter"))
                    .getOrResultError("Trying to create forum thread")
                    .andThen { Ok(it.data.id.toString()) }
            }
    }

    private suspend fun fetchOffset(
        mangaUUID: String,
        langs: List<String>,
        includeUnavailable: Boolean,
        offset: Int,
    ): Result<ChapterListDto, ResultError> {
        return service
            .viewChapters(mangaUUID, langs, includeUnavailable.toInt().toString(), offset)
            .getOrResultError("Trying to view chapters")
    }

    private fun getGroupMap(results: List<ChapterDataDto>): Map<String, String> {
        return results
            .flatMap { it.relationships }
            .mapNotNull { relationship ->
                val name = relationship.attributes?.name
                if (
                    relationship.type == MdConstants.Types.scanlator &&
                        !name.isNullOrBlank() &&
                        !NO_GROUP.equals(name, true)
                ) {
                    relationship.id to name
                } else {
                    null
                }
            }
            .toMap()
    }

    private suspend fun getUploaderMap(
        results: List<ChapterDataDto>,
        groups: Map<String, String>,
    ): Map<String, String> {
        return results
            .asSequence()
            .map { chapter -> chapter.relationships }
            .filter { relationships -> !relationships.any { groups.containsKey(it.id) } }
            .flatten()
            .filter { it.type == MdConstants.Types.uploader }
            .distinctBy { it.id }
            .associate {
                it.id to
                    service
                        .uploader(it.id)
                        .getOrResultError("Trying to get uploader username")
                        .mapBoth(
                            success = { user -> user.data.attributes.username },
                            failure = { "" },
                        )
            }
    }
}
