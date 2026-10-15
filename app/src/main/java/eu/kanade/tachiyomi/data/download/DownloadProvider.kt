package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.text.isDigitsOnly
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.lang.isUUID
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import tachiyomi.core.util.storage.displayablePath
import tachiyomi.core.util.storage.nameWithoutExtension
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to provide the directories where the downloads should be saved. It uses the
 * following path scheme: /<root downloads dir>/<source name>/<manga>/<chapter>
 *
 * @param context the application context.
 */
class DownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
) {

    /** Preferences helper. */
    private val source = Injekt.get<SourceManager>().mangaDex

    /** The root directory for downloads. */
    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param manga the manga to query.
     * @param source the source of the manga.
     */
    @Synchronized
    internal fun getMangaDir(manga: Manga): UniFile {
        try {
            val mangaDirName = getMangaDirName(manga)
            val sourceDirName = getSourceDirName()
            DiskUtil.createNoMediaFile(downloadsDir, context)
            TimberKt.d { "creating directory for $sourceDirName : $mangaDirName" }
            return downloadsDir!!.createDirectory(sourceDirName)!!.createDirectory(mangaDirName)!!
        } catch (e: Exception) {
            TimberKt.e(e) { "error getting download folder for ${manga.displayTitle()}" }

            throw Exception(
                context.getString(
                    R.string.invalid_download_location,
                    downloadsDir?.displayablePath ?: "",
                )
            )
        }
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(): UniFile? {
        return downloadsDir?.findFile(getSourceDirName())
    }

    /**
     * Returns the download directory for a manga if it exists.
     *
     * @param manga the manga to query.
     * @param source the source of the manga.
     */
    fun findMangaDir(manga: Manga): UniFile? {
        val sourceDir = findSourceDir()
        return sourceDir?.findFile(getMangaDirName(manga))
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param chapter the chapter to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findChapterDir(chapter: Chapter, manga: Manga): UniFile? {
        val mangaDir = findMangaDir(manga)
        return getValidChapterDirNames(chapter)
            .asSequence()
            .mapNotNull { mangaDir?.findFile(it) ?: mangaDir?.findFile("$it.cbz") }
            .firstOrNull()
            ?: mangaDir
                ?.listFiles { _, filename ->
                    filename.contains(chapter.uuid()) && !filename.endsWith(TMP_DIR_SUFFIX)
                }
                ?.firstOrNull()
    }

    fun chapterDirDoesNotExist(chapter: Chapter, chapterDirs: List<UniFile>): Boolean {
        // A name ending in TMP_DIR_SUFFIX belongs to a download that has not finished.
        val finishedDirs = chapterDirs.filterNot { it.name.orEmpty().endsWith(TMP_DIR_SUFFIX) }

        var found =
            getValidChapterDirNames(chapter).any { chapterDir ->
                finishedDirs.any { uniFile ->
                    uniFile.nameWithoutExtension!!.equals(chapterDir, true)
                }
            }

        if (!found && chapter.mangadex_chapter_id.isNotEmpty()) {
            found = finishedDirs.any { uniFile ->
                uniFile.nameWithoutExtension!!.contains(chapter.uuid())
            }
        }

        return !found
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findChapterDirs(chapters: List<Chapter>, manga: Manga): List<UniFile> {
        val mangaDir = findMangaDir(manga) ?: return emptyList()

        val idHashSet = chapters.map { it.mangadex_chapter_id }.toHashSet()
        val oldIdHashSet = chapters.mapNotNull { it.old_mangadex_id }.toHashSet()
        val chapterNameHashSet = chapters.map { it.name }.toHashSet()
        val scanalatorNameHashSet = chapters.map { getJ2kChapterName(it) }.toHashSet()
        val scanalatorCbzNameHashSet = chapters.map { "${getChapterDirName(it)}.cbz" }.toHashSet()

        val files = mangaDir.listFiles() ?: return emptyList()
        return files.filter { file ->
            file.name?.let { fileName ->
                val mangadexId = fileName.substringAfterLast(" - ", "")
                // legacy dex id
                if (mangadexId.isNotEmpty() && mangadexId.isUUID()) {
                    return@filter idHashSet.contains(mangadexId)
                } else if (mangadexId.isNotEmpty() && mangadexId.isDigitsOnly()) {
                    return@filter oldIdHashSet.contains(mangadexId)
                } else {
                    if (scanalatorNameHashSet.contains(fileName)) {
                        return@filter true
                    }
                    if (scanalatorCbzNameHashSet.contains(fileName)) {
                        return@filter true
                    }
                    val afterScanlatorCheck = fileName.substringAfter("_")
                    return@filter chapterNameHashSet.contains(fileName) ||
                        chapterNameHashSet.contains(afterScanlatorCheck)
                }
            }
            return@filter false
        }
    }

    /**
     * Returns the files in the manga directory that match none of the chapters, and the temp files
     * of downloads that are no longer queued.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param queuedChapters the queued chapters of the manga, whose temp files a running or paused
     *   download still uses. Called after the directory is listed, so every temp file in the
     *   listing was created before the queue was read.
     */
    fun findUnmatchedChapterDirs(
        chapters: List<Chapter>,
        manga: Manga,
        queuedChapters: () -> List<Chapter>,
    ): List<UniFile> {
        val mangaDir = findMangaDir(manga) ?: return emptyList()
        val idHashSet = chapters.map { it.mangadex_chapter_id }.toHashSet()
        val chapterNameHashSet = chapters.map { it.name }.toHashSet()
        val scanalatorNameHashSet = chapters.map { getJ2kChapterName(it) }.toHashSet()
        val scanalatorCbzNameHashSet = chapters.map { "${getChapterDirName(it)}.cbz" }.toHashSet()

        val files = mangaDir.listFiles()?.asList() ?: return emptyList()
        val queuedTempNames =
            queuedChapters()
                .map { getChapterDirName(it) }
                .flatMap { listOf("$it$TMP_DIR_SUFFIX", "$it.cbz$TMP_DIR_SUFFIX") }
                .toHashSet()

        return files.filter { file ->
            file.name?.let { fileName ->
                if (fileName.endsWith(TMP_DIR_SUFFIX)) {
                    return@filter fileName !in queuedTempNames
                }
                if (fileName.startsWith("${Constants.LOCAL_SOURCE}_")) {
                    return@filter false
                }

                val mangadexId = fileName.substringAfterLast("- ", "")
                if (mangadexId.isNotEmpty() && (mangadexId.isDigitsOnly() || mangadexId.isUUID())) {
                    return@filter !idHashSet.contains(mangadexId)
                } else {
                    if (scanalatorNameHashSet.contains(fileName)) {
                        return@filter false
                    }
                    if (scanalatorCbzNameHashSet.contains(fileName)) {
                        return@filter false
                    }

                    val afterScanlatorCheck = fileName.substringAfter("_")
                    return@filter !chapterNameHashSet.contains(fileName) &&
                        !chapterNameHashSet.contains(afterScanlatorCheck)
                }
            }
            // everything else is considered true
            return@filter true
        }
    }

    fun renameMangaFolder(from: String, to: String) {
        val sourceDir = findSourceDir()
        val mangaDir = sourceDir?.findFile(DiskUtil.buildValidFilename(from))
        val toFileName = DiskUtil.buildValidFilename(to)
        mangaDir?.renameTo(toFileName)
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findTempChapterDirs(chapters: List<Chapter>, manga: Manga): List<UniFile> {
        val mangaDir = findMangaDir(manga) ?: return emptyList()
        return chapters.flatMap {
            val chapterDirName = getChapterDirName(it)
            // The page folder, and the cbz if the download stopped while it was being written.
            listOfNotNull(
                mangaDir.findFile("$chapterDirName$TMP_DIR_SUFFIX"),
                mangaDir.findFile("$chapterDirName.cbz$TMP_DIR_SUFFIX"),
            )
        }
    }

    /**
     * Returns the download directory name for a source always english to not break with other forks
     * or current neko
     *
     * @param source the source to query.
     */
    fun getSourceDirName(): String {
        return "$source (EN)"
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param manga the manga to query.
     */
    fun getMangaDirName(manga: Manga): String {
        return DiskUtil.buildValidFilename(manga.displayTitle())
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param chapter the chapter to query.
     */
    fun getChapterDirName(chapter: Chapter): String {
        return when (chapter.isMergedChapter() || chapter.isLocalSource()) {
            true -> getJ2kChapterName(chapter)
            false -> DiskUtil.buildValidFilename(chapter.name, " - ${chapter.mangadex_chapter_id}")
        }
    }

    fun getJ2kChapterName(chapter: Chapter): String {
        return DiskUtil.buildValidFilename(
            if (chapter.scanlator != null) {
                "${chapter.scanlator}_${chapter.name}"
            } else {
                chapter.name
            }
        )
    }

    /**
     * Returns valid downloaded chapter directory names.
     *
     * @param chapter the chapter to query.
     */
    fun getValidChapterDirNames(chapter: Chapter): List<String> {
        return listOf(
                getChapterDirName(chapter),
                // chapter names from j2k
                getJ2kChapterName(chapter),
            )
            .filter { it.isNotEmpty() }
    }

    /**
     * Returns valid downloaded chapter directory names or cbz
     *
     * @param chapter the chapter to query.
     */
    fun getValidChapterDirOrFileNames(chapter: Chapter): List<String> {
        return listOf(
                getChapterDirName(chapter),
                getChapterDirName(chapter) + ".cbz",
                getJ2kChapterName(chapter) + ".cbz",
                // chapter names from j2k
                getJ2kChapterName(chapter),
            )
            .filter { it.isNotEmpty() }
    }
}
