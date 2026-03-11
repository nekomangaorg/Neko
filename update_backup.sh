sed -i 's/val historyChapters = chapters.associateBy { it.id }/val historyChapters = chapters.associate { it.id to it.url }/' app/src/main/java/eu/kanade/tachiyomi/data/backup/BackupCreator.kt
sed -i 's/historyChapters\[history.chapter_id\]?.url?.let {/historyChapters[history.chapter_id]?.let {/' app/src/main/java/eu/kanade/tachiyomi/data/backup/BackupCreator.kt
