package org.nekomanga.presentation.components.dialog

import eu.kanade.tachiyomi.data.backup.BackupConst
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateBackupDialogTest {

    @Test
    fun defaultStateIncludesAllFlags() {
        val state = CreateBackupDialogState()
        val expected =
            BackupConst.BACKUP_CATEGORY or
                BackupConst.BACKUP_CHAPTER or
                BackupConst.BACKUP_TRACK or
                BackupConst.BACKUP_HISTORY or
                BackupConst.BACKUP_READ_MANGA

        assertEquals(expected, state.toBackupFlags())
    }

    @Test
    fun flagsReflectUncheckedOptions() {
        val state =
            CreateBackupDialogState(
                categories = false,
                chapters = true,
                tracking = false,
                history = true,
                allReadManga = false,
            )
        val expected = BackupConst.BACKUP_CHAPTER or BackupConst.BACKUP_HISTORY

        assertEquals(expected, state.toBackupFlags())
    }

    @Test
    fun flagsWithNothingCheckedReturnsZero() {
        val state =
            CreateBackupDialogState(
                categories = false,
                chapters = false,
                tracking = false,
                history = false,
                allReadManga = false,
            )

        assertEquals(0, state.toBackupFlags())
    }
}
