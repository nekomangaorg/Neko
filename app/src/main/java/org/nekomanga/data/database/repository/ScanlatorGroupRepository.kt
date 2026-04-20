package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl

interface ScanlatorGroupRepository {
    suspend fun getScanlatorGroupByName(name: String): ScanlatorGroupImpl?

    suspend fun getScanlatorGroupsByNames(names: List<String>): List<ScanlatorGroupImpl>

    suspend fun insertScanlatorGroup(group: ScanlatorGroupImpl): Long

    suspend fun insertScanlatorGroups(groups: List<ScanlatorGroupImpl>)

    suspend fun deleteScanlatorGroup(name: String)
}
