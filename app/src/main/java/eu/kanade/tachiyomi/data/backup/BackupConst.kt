package eu.kanade.tachiyomi.data.backup

import org.nekomanga.BuildConfig.APPLICATION_ID as ID

object BackupConst {

    private const val NAME = "BackupRestorer"
    const val EXTRA_URI = "$ID.$NAME.EXTRA_URI"
    const val EXTRA_FLAGS = "$ID.$NAME.EXTRA_FLAGS"
    const val EXTRA_MODE = "$ID.$NAME.EXTRA_MODE"
    const val EXTRA_TYPE = "$ID.$NAME.EXTRA_TYPE"

    const val BACKUP_TYPE_FULL = 1

    // Filter options
    internal const val BACKUP_CATEGORY = 0x1
    internal const val BACKUP_CATEGORY_MASK = 0x1
    internal const val BACKUP_CHAPTER = 0x2
    internal const val BACKUP_CHAPTER_MASK = 0x2
    internal const val BACKUP_HISTORY = 0x4
    internal const val BACKUP_HISTORY_MASK = 0x4
    internal const val BACKUP_TRACK = 0x8
    internal const val BACKUP_TRACK_MASK = 0x8
    internal const val BACKUP_READ_MANGA = 0x20
    internal const val BACKUP_READ_MANGA_MASK = 0x20
    internal const val BACKUP_ALL = 0x1F
}
