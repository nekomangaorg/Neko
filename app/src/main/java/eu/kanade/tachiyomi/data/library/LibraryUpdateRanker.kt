package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.database.models.Manga
import kotlin.math.abs

/**
 * This class will provide various functions to Rank mangaList to efficiently schedule mangaList to update.
 */
object LibraryUpdateRanker {

    val rankingScheme = listOf(
        (this::lexicographicRanking)(),
        (this::latestFirstRanking)(),
        (this::nextFirstRanking)()
    )

    /**
     * Provides a total ordering over all the MangaList.
     *
     * Orders the manga based on the distance between the next expected update and now.
     * The comparator is reversed, placing the smallest (and thus closest to updating now) first.
     */
    fun nextFirstRanking(): Comparator<Manga> {
        val time = System.currentTimeMillis()
        return Comparator {
            mangaFirst: Manga,
            mangaSecond: Manga,
            ->
            compareValues(abs(mangaSecond.next_update - time), abs(mangaFirst.next_update - time))
        }.reversed()
    }

    /**
     * Provides a total ordering over all the MangaList.
     *
     * Assumption: An active [Manga] mActive is expected to have been last updated after an
     * inactive [Manga] mInactive.
     *
     * Using this insight, function returns a Comparator for which mActive appears before mInactive.
     * @return a Comparator that ranks manga based on relevance.
     */
    fun latestFirstRanking(): Comparator<Manga> {
        return Comparator {
            mangaFirst: Manga,
            mangaSecond: Manga,
            ->
            compareValues(mangaSecond.last_update, mangaFirst.last_update)
        }
    }

    /**
     * Provides a total ordering over all the MangaList.
     *
     * Order the manga lexicographically.
     * @return a Comparator that ranks manga lexicographically based on the title.
     */
    fun lexicographicRanking(): Comparator<Manga> {
        return Comparator {
            mangaFirst: Manga,
            mangaSecond: Manga,
            ->
            compareValues(mangaFirst.title, mangaSecond.title)
        }
    }
}
