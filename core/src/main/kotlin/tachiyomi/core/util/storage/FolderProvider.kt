package tachiyomi.core.util.storage

import java.io.File

interface FolderProvider {

    fun directory(): File

    fun path(): String
}
