package tachiyomi.core.util.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.core.content.ContextCompat
import com.hippo.unifile.UniFile
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.nekomanga.constants.Constants.TMP_FILE_SUFFIX
import tachiyomi.core.util.lang.Hash

object DiskUtil {

    /** Returns the root folders of all the available external storages. */
    fun getExternalStorages(context: Context): List<File> {
        return ContextCompat.getExternalFilesDirs(context, null).filterNotNull().mapNotNull {
            val file = File(it.absolutePath.substringBefore("/Android/"))
            val state = Environment.getExternalStorageState(file)
            if (
                state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
            ) {
                file
            } else {
                null
            }
        }
    }

    fun hashKeyForDisk(key: String): String {
        return Hash.md5(key)
    }

    fun getCacheDirSize(context: Context): Long {
        return File(context.cacheDir, "")
            .listFiles()!!
            .mapNotNull {
                if (it.isFile && (it.name.endsWith(TMP_FILE_SUFFIX))) {
                    getDirectorySize(it)
                } else {
                    null
                }
            }
            .sum()
    }

    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            f.listFiles()?.let {
                for (file in it) {
                    size += getDirectorySize(file)
                }
            }
        } else {
            size = f.length()
        }
        return size
    }

    /** Gets the available space for the disk that a file path points to, in bytes. */
    fun getAvailableStorageSpace(f: UniFile): Long {
        return try {
            val stat = StatFs(f.uri.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /** Don't display downloaded chapters in gallery apps creating `.nomedia`. */
    fun createNoMediaFile(dir: UniFile?, context: Context?) {
        if (dir != null && dir.exists()) {
            val nomedia = dir.findFile(NOMEDIA_FILE)
            if (nomedia == null) {
                dir.createFile(NOMEDIA_FILE)!!
                context?.let { scanMedia(it, dir.uri) }
            }
        }
    }

    /** Scans the given file so that it can be shown in gallery apps, for example. */
    fun scanMedia(context: Context, file: File) {
        scanMedia(context, Uri.fromFile(file))
    }

    /** Scans the given file so that it can be shown in gallery apps, for example. */
    fun scanMedia(context: Context, uri: Uri) {
        val action = Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
        val mediaScanIntent = Intent(action)
        mediaScanIntent.data = uri
        context.sendBroadcast(mediaScanIntent)
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem, replacing any invalid
     * characters with "_". This method doesn't allow hidden files (starting with a dot), but you
     * can manually add it later.
     */
    fun buildValidFilename(origName: String, suffix: String = ""): String {
        val name = origName.trim('.', ' ')
        if (name.isEmpty()) {
            return "(invalid)"
        }
        val fatCompliantName =
            name
                .map { ch ->
                    if (isValidFatFilenameChar(ch)) {
                        ch
                    } else {
                        "_"
                    }
                }
                .joinToString("")

        if (suffix.isNotEmpty()) {
            return fatCompliantName.take(240 - suffix.length) + suffix
        } else {
            // Even though vfat allows 255 UCS-2 chars, we might eventually write to
            // ext4 through a FUSE layer, so use that limit minus  reserved characters.
            return fatCompliantName.take(240)
        }
    }

    /** Returns true if the given character is a valid filename character, false otherwise. */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '\'',
            '"',
            '*',
            '/',
            ':',
            '<',
            '>',
            '?',
            '\\',
            '|',
            '~',
            0x7f.toChar() -> false
            else -> true
        }
    }

    /** Returns real size of directory in human readable format. */
    fun readableDiskSize(context: Context, file: File): String {
        return Formatter.formatFileSize(context, getDirectorySize(file))
    }

    /** Returns real size of directory in human readable format. */
    fun readableDiskSize(context: Context, bytes: Long): String {
        return Formatter.formatFileSize(context, bytes)
    }

    fun cleanupDiskSpace(directory: File, context: Context, isTmpFileLookup: Boolean = false) {
        if (isTmpFileLookup) {
            File(context.cacheDir, "").listFiles()?.asSequence()?.forEach {
                if (it.isFile && (it.name.endsWith(TMP_FILE_SUFFIX))) {
                    it.delete()
                }
            }
        } else {
            directory.listFiles()?.asSequence()?.forEach { it.delete() }
        }
    }

    fun observeDiskSpace(
        directory: File,
        context: Context,
        isTmpFileLookup: Boolean = false,
    ): Flow<String> = flow {
        while (true) {
            if (isTmpFileLookup) {
                val tmpFiles =
                    File(context.cacheDir, "")
                        .listFiles()!!
                        .mapNotNull {
                            if (it.isFile && (it.name.endsWith(TMP_FILE_SUFFIX))) {
                                getDirectorySize(it)
                            } else {
                                null
                            }
                        }
                        .sum()
                emit(readableDiskSize(context, tmpFiles))
            } else {

                emit(readableDiskSize(context, directory))
            }
            delay(3.seconds)
        }
    }

    const val NOMEDIA_FILE = ".nomedia"
}
