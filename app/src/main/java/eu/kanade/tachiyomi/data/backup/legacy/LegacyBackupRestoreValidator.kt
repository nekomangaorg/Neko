package eu.kanade.tachiyomi.data.backup.legacy

import android.content.Context
import android.net.Uri
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupRestoreValidator
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup

class LegacyBackupRestoreValidator : AbstractBackupRestoreValidator() {
    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if version or manga cannot be found.
     * @return List of missing sources or missing trackers.
     */
    override fun validate(context: Context, uri: Uri): Results {
        val reader = JsonReader(context.contentResolver.openInputStream(uri)!!.bufferedReader())
        val json = JsonParser.parseReader(reader).asJsonObject

        val version = json.get(Backup.VERSION)
        val mangaListJson = json.get(Backup.MANGAS)
        if (version == null || mangaListJson == null) {
            throw Exception(context.getString(R.string.file_is_missing_data))
        }

        val mangaList = mangaListJson.asJsonArray
        if (mangaList.size() == 0) {
            throw Exception(context.getString(R.string.backup_has_no_manga))
        }

        val sources = getSourceMapping(json)
        val missingSources = sources
            .filter { sourceManager.get(it.key) == null }
            .values
            .sorted()

        val trackers = mangaList
            .filter { it.asJsonObject.has("track") }
            .flatMap { it.asJsonObject["track"].asJsonArray }
            .map { it.asJsonObject["s"].asInt }
            .distinct()
        val missingTrackers = trackers
            .mapNotNull { trackManager.getService(it) }
            .filter { !it.isLogged }
            .map { context.getString(it.nameRes()) }
            .sorted()

        return Results(missingSources, missingTrackers)
    }

    companion object {
        fun getSourceMapping(json: JsonObject): Map<Long, String> {
            val extensionsMapping = json.get(Backup.EXTENSIONS) ?: return emptyMap()

            return extensionsMapping.asJsonArray
                .map {
                    val items = it.asString.split(":")
                    items[0].toLong() to items[1]
                }
                .toMap()
        }
    }
}
