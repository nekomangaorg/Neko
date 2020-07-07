package eu.kanade.tachiyomi

import android.os.Environment
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DebugTree() : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return super.createStackElementTag(element) + ":" + element.lineNumber
    }
}

class FileDebugTree() : Timber.DebugTree() {
    val directory = Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOCUMENTS}/neko-logs")
    val fileName = "Neko.log"

    override fun createStackElementTag(element: StackTraceElement): String? {
        return super.createStackElementTag(element) + ":" + element.lineNumber
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {

            if (!directory.exists())
                directory.mkdirs()

            val file = File("${directory.absolutePath}${File.separator}$fileName")

            file.createNewFile()

            if (file.exists()) {
                val fos = FileOutputStream(file, true)

                fos.write("$message\n".toByteArray(Charsets.UTF_8))
                fos.close()
            }
        } catch (e: IOException) {
            Log.println(Log.ERROR, "FileLogTree", "Error while logging into file: $e")
        }
    }

    fun cleanup() {
        Timber.d("clean up and delete folder %s", directory.name)
        directory.deleteRecursively()
    }
}

class ReleaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority == Log.ERROR) {
            FirebaseCrashlytics.getInstance().log(message)
            if (throwable != null) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        } else return
    }
}
