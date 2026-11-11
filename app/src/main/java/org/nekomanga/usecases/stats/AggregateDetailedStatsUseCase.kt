package org.nekomanga.usecases.stats

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedDistributionUiModel
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatDistribution
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatGroup
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatManga

class AggregateDetailedStatsUseCase(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend operator fun invoke(
        mangaList: List<DetailedStatManga>,
        categories: List<String>,
        defaultCategoryName: String,
        typeStringResolver: (MangaType) -> String,
        statusStringResolver: (MangaStatus) -> String,
    ): DetailedDistributionUiModel =
        withContext(defaultDispatcher) {
            if (mangaList.isEmpty()) {
                return@withContext DetailedDistributionUiModel()
            }

            // Category distribution using inverted index in O(M + C) instead of O(M * C)
            val categoryMangaMap = mutableMapOf<String, MutableList<DetailedStatManga>>()
            categories.forEach { categoryMangaMap[it] = mutableListOf() }
            if (defaultCategoryName !in categoryMangaMap) {
                categoryMangaMap[defaultCategoryName] = mutableListOf()
            }

            for (manga in mangaList) {
                for (cat in manga.categories) {
                    categoryMangaMap.getOrPut(cat) { mutableListOf() }.add(manga)
                }
            }

            val categoryGroups =
                categoryMangaMap.entries
                    .filter { it.key != defaultCategoryName || it.value.isNotEmpty() }
                    .map { (catName, mangaInCat) ->
                        DetailedStatGroup(
                            key = catName,
                            count = mangaInCat.size,
                            readChapters = mangaInCat.sumOf { it.readChapters },
                            totalChapters = mangaInCat.sumOf { it.totalChapters },
                            readDuration = mangaInCat.sumOf { it.readDuration },
                        )
                    }

            val categoryDistribution =
                DetailedStatDistribution(
                    totalCount = categoryGroups.sumOf { it.count },
                    totalChapters = categoryGroups.sumOf { it.totalChapters },
                    totalReadDuration = categoryGroups.sumOf { it.readDuration },
                    groups = categoryGroups,
                )

            // Type distribution
            val typeGroups =
                mangaList
                    .groupBy { typeStringResolver(it.type) }
                    .map { (typeName, mangaInType) ->
                        DetailedStatGroup(
                            key = typeName,
                            count = mangaInType.size,
                            readChapters = mangaInType.sumOf { it.readChapters },
                            totalChapters = mangaInType.sumOf { it.totalChapters },
                            readDuration = mangaInType.sumOf { it.readDuration },
                        )
                    }

            val typeDistribution =
                DetailedStatDistribution(
                    totalCount = typeGroups.sumOf { it.count },
                    totalChapters = typeGroups.sumOf { it.totalChapters },
                    totalReadDuration = typeGroups.sumOf { it.readDuration },
                    groups = typeGroups,
                )

            // Status distribution
            val statusGroups =
                mangaList
                    .groupBy { statusStringResolver(it.status) }
                    .map { (statusName, mangaInStatus) ->
                        DetailedStatGroup(
                            key = statusName,
                            count = mangaInStatus.size,
                            readChapters = mangaInStatus.sumOf { it.readChapters },
                            totalChapters = mangaInStatus.sumOf { it.totalChapters },
                            readDuration = mangaInStatus.sumOf { it.readDuration },
                        )
                    }

            val statusDistribution =
                DetailedStatDistribution(
                    totalCount = statusGroups.sumOf { it.count },
                    totalChapters = statusGroups.sumOf { it.totalChapters },
                    totalReadDuration = statusGroups.sumOf { it.readDuration },
                    groups = statusGroups,
                )

            // Content Rating distribution
            val contentRatingGroups =
                mangaList
                    .groupBy { it.contentRating.prettyPrint() }
                    .map { (ratingName, mangaInRating) ->
                        DetailedStatGroup(
                            key = ratingName,
                            count = mangaInRating.size,
                            readChapters = mangaInRating.sumOf { it.readChapters },
                            totalChapters = mangaInRating.sumOf { it.totalChapters },
                            readDuration = mangaInRating.sumOf { it.readDuration },
                        )
                    }

            val contentRatingDistribution =
                DetailedStatDistribution(
                    totalCount = contentRatingGroups.sumOf { it.count },
                    totalChapters = contentRatingGroups.sumOf { it.totalChapters },
                    totalReadDuration = contentRatingGroups.sumOf { it.readDuration },
                    groups = contentRatingGroups,
                )

            // Tag distribution
            val tagToMangaMap = mutableMapOf<String, MutableList<DetailedStatManga>>()
            for (manga in mangaList) {
                for (tag in manga.tags) {
                    if (!tag.contains("content rating:", ignoreCase = true)) {
                        tagToMangaMap.getOrPut(tag) { mutableListOf() }.add(manga)
                    }
                }
            }

            val tagGroups =
                tagToMangaMap.entries.map { (tag, mangaInTag) ->
                    DetailedStatGroup(
                        key = tag,
                        count = mangaInTag.size,
                        readChapters = mangaInTag.sumOf { it.readChapters },
                        totalChapters = mangaInTag.sumOf { it.totalChapters },
                        readDuration = mangaInTag.sumOf { it.readDuration },
                    )
                }

            val tagDistribution =
                DetailedStatDistribution(
                    totalCount = tagGroups.sumOf { it.count },
                    totalChapters = tagGroups.sumOf { it.totalChapters },
                    totalReadDuration = tagGroups.sumOf { it.readDuration },
                    groups = tagGroups,
                )

            // Start Year distribution
            val startYearGroups =
                mangaList
                    .groupBy { it.startYear }
                    .map { (year, mangaInYear) ->
                        DetailedStatGroup(
                            key = year?.toString() ?: "",
                            count = mangaInYear.size,
                            readChapters = mangaInYear.sumOf { it.readChapters },
                            totalChapters = mangaInYear.sumOf { it.totalChapters },
                            readDuration = mangaInYear.sumOf { it.readDuration },
                        )
                    }
                    .sortedWith(
                        compareBy<DetailedStatGroup> { it.key.isEmpty() }.thenBy { it.key },
                    )

            val startYearDistribution =
                DetailedStatDistribution(
                    totalCount = startYearGroups.sumOf { it.count },
                    totalChapters = startYearGroups.sumOf { it.totalChapters },
                    totalReadDuration = startYearGroups.sumOf { it.readDuration },
                    groups = startYearGroups,
                )

            DetailedDistributionUiModel(
                categoryDistribution = categoryDistribution,
                typeDistribution = typeDistribution,
                statusDistribution = statusDistribution,
                contentRatingDistribution = contentRatingDistribution,
                tagDistribution = tagDistribution,
                startYearDistribution = startYearDistribution,
            )
        }
}
