package eu.kanade.tachiyomi.data.backup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.Backup
import okio.buffer
import okio.gzip
import okio.source

object BackupUtil {
    /** Decode a potentially-gzipped backup. */
    @SuppressLint("Recycle")
    fun decodeBackup(context: Context, uri: Uri): Backup {
        val backupCreator = BackupCreator(context)

        return context.contentResolver.openInputStream(uri)!!.source().buffer().use {
            backupStringSource ->
            val peeked = backupStringSource.peek()
            peeked.require(2)
            val id1id2 = peeked.readShort()
            val backupString =
                if (id1id2.toInt() == 0x1f8b) { // 0x1f8b is gzip magic bytes
                        backupStringSource.gzip().buffer()
                    } else {
                        backupStringSource
                    }
                    .use { it.readByteArray() }

            backupCreator.parser.decodeFromByteArray(Backup.serializer(), backupString)
        }
    }
}
