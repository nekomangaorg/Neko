package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.text.isDigitsOnly
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife
import eu.kanade.tachiyomi.util.lang.isUUID
import org.nekomanga.R
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
            TimberKt.e(e) { "error getting download folder for ${manga.title}" }

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
                ?.listFiles { _, filename -> filename.contains(chapter.uuid()) }
                ?.firstOrNull()
    }

    fun chapterDirDoesNotExist(chapter: Chapter, chapterDirs: List<UniFile>): Boolean {
        var exists =
            getValidChapterDirNames(chapter).none { chapterDir ->
                chapterDirs.none { uniFile ->
                    uniFile.nameWithoutExtension!!.equals(chapterDir, true)
                }
            }

        if (!exists) {
            exists =
                chapterDirs.none { uniFile ->
                    uniFile.nameWithoutExtension!!.contains(chapter.uuid())
                }
        }

        return exists
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

        return mangaDir.listFiles()!!.asList().filter { file ->
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
     * Returns a list of all files in manga directory
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findUnmatchedChapterDirs(chapters: List<Chapter>, manga: Manga): List<UniFile> {
        val mangaDir = findMangaDir(manga) ?: return emptyList()
        val idHashSet = chapters.map { it.mangadex_chapter_id }.toHashSet()
        val chapterNameHashSet = chapters.map { it.name }.toHashSet()
        val scanalatorNameHashSet = chapters.map { getJ2kChapterName(it) }.toHashSet()
        val scanalatorCbzNameHashSet = chapters.map { "${getChapterDirName(it)}.cbz" }.toHashSet()

        return mangaDir.listFiles()!!.asList().filter { file ->
            file.name?.let { fileName ->
                if (fileName.endsWith(Downloader.TMP_DIR_SUFFIX)) {
                    return@filter true
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

    fun renameChapterFoldersForLegacyMerged(manga: Manga) {
        val mangaDir = findMangaDir(manga) ?: return
        mangaDir
            .listFiles()
            ?.filter { file -> file.name != null && file.name!!.startsWith(MangaLife.oldName) }
            ?.forEach { file ->
                val newFileName = file.name!!.replace(MangaLife.oldName, MangaLife.name)
                file.renameTo(newFileName)
            }
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
        return chapters.mapNotNull {
            mangaDir.findFile("${getChapterDirName(it)}${Downloader.TMP_DIR_SUFFIX}")
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
        return DiskUtil.buildValidFilename(manga.originalTitle)
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param chapter the chapter to query.
     */
    fun getChapterDirName(chapter: Chapter): String {
        return when (chapter.isMergedChapter()) {
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
}
