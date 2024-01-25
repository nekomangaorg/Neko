package eu.kanade.tachiyomi.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupCreator
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import okio.buffer
import okio.gzip
import okio.source

object BackupUtil {
    /** Decode a potentially-gzipped backup. */
    @SuppressLint("Recycle")
    fun decodeBackup(context: Context, uri: Uri): Backup {
        val backupCreator = BackupCreator(context)

        val backupStringSource = context.contentResolver.openInputStream(uri)!!.source().buffer()

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

        return backupCreator.parser.decodeFromByteArray(BackupSerializer, backupString)
    }
}
