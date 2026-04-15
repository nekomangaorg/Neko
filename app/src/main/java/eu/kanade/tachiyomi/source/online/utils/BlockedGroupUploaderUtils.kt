package eu.kanade.tachiyomi.source.online.utils

import com.github.michaelbull.result.getOrElse
import eu.kanade.tachiyomi.source.online.MangaDex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.repository.ScanlatorRepositoryImpl
import org.nekomanga.domain.site.MangaDexPreferences

suspend fun getBlockedScanlatorGroupUUIDs(
    mangaDexPreferences: MangaDexPreferences,
    scanlatorRepository: ScanlatorRepositoryImpl,
    mangaDex: MangaDex,
): List<String> {
    val blockedGroupNames = mangaDexPreferences.blockedGroups().get().toList()
    return coroutineScope {
        val chunks = blockedGroupNames.chunked(900)
        val existingGroups = chunks.flatMap { chunk ->
            scanlatorRepository.getScanlatorGroupsByNames(chunk)
        }
        val existingGroupNames = existingGroups.map { it.name }.toSet()
        val missingGroupNames = blockedGroupNames.filterNot { it in existingGroupNames }

        val fetchedGroups =
            missingGroupNames
                .map { name -> async { mangaDex.getScanlatorGroup(group = name) } }
                .awaitAll()
                .mapNotNull { result -> result.getOrElse { null }?.toScanlatorGroupImpl() }

        if (fetchedGroups.isNotEmpty()) {
            scanlatorRepository.insertScanlatorGroups(fetchedGroups.map { it.toEntity() })
        }
        val fetchedGroupEntities = fetchedGroups.map { it.toEntity() }
        (existingGroups + fetchedGroupEntities).map { it.uuid }
    }
}

suspend fun getBlockedUploaderUUIDs(
    mangaDexPreferences: MangaDexPreferences,
    scanlatorRepository: ScanlatorRepositoryImpl,
    mangaDex: MangaDex,
): List<String> {
    val blockedUploaderNames = mangaDexPreferences.blockedUploaders().get().toList()
    return coroutineScope {
        val chunks = blockedUploaderNames.chunked(900)
        val existingUploaders = chunks.flatMap { chunk ->
            scanlatorRepository.getUploadersByNamesSync(chunk)
        }
        val existingUploaderNames = existingUploaders.map { it.username }.toSet()
        val missingUploaderNames = blockedUploaderNames.filterNot { it in existingUploaderNames }

        val fetchedUploaders =
            missingUploaderNames
                .map { name -> async { mangaDex.getUploader(uploader = name) } }
                .awaitAll()
                .mapNotNull { result -> result.getOrElse { null }?.toUploaderImpl() }

        if (fetchedUploaders.isNotEmpty()) {
            scanlatorRepository.insertUploaders(fetchedUploaders.map { it.toEntity() })
        }
        val fetchedUploaderEntities = fetchedUploaders.map { it.toEntity() }
        (existingUploaders + fetchedUploaderEntities).map { it.uuid }
    }
}
