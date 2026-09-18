package tachiyomi.core.util.storage

import android.content.Context
import android.os.Build
import android.os.FileUtils
import com.hippo.unifile.UniFile
import java.io.BufferedOutputStream
import java.io.File

val UniFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')

val UniFile.extension: String?
    get() = name?.substringAfterLast('.')

val UniFile.displayablePath: String
    get() = filePath ?: uri.toString()

fun UniFile.toTempFile(context: Context): File {
    val rawName = nameWithoutExtension.orEmpty()
    val prefix = if (rawName.length < 3) rawName.padEnd(3, '_') else rawName
    val tempFile = File.createTempFile(prefix, null, context.cacheDir)

    context.contentResolver.openInputStream(uri)!!.use { inputStream ->
        tempFile.outputStream().use { outputStream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(inputStream, outputStream)
            } else {
                BufferedOutputStream(outputStream).use { tmpOut ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (inputStream.read(buffer).also { count = it } > 0) {
                        tmpOut.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    return tempFile
}
