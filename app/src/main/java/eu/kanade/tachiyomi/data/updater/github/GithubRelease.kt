package eu.kanade.tachiyomi.data.updater.github

import android.os.Build
import com.google.gson.annotations.SerializedName
import eu.kanade.tachiyomi.data.updater.Release

/**
 * Release object.
 * Contains information about the latest release from GitHub.
 *
 * @param version version of latest release.
 * @param info log of latest release.
 * @param assets assets of latest release.
 */
class GithubRelease(
    @SerializedName("tag_name") val version: String,
    @SerializedName("body") override val info: String,
    @SerializedName("html_url") override val releaseLink: String,
    @SerializedName("assets") private val assets: List<Assets>
) : Release {

    /**
     * Get download link of latest release from the assets.
     * @return download link of latest release.
     */
    override val downloadLink: String
        get() {
            val apkVariant = when (Build.SUPPORTED_ABIS[0]) {
                "arm64-v8a" -> "-arm64-v8a"
                "armeabi-v7a" -> "-armeabi-v7a"
                "x86", "x86_64" -> "-x86"
                else -> ""
            }

            return assets.find { it.downloadLink.contains("tachiyomij2k$apkVariant-") }?.downloadLink
                ?: assets[0].downloadLink
        }

    /**
     * Assets class containing download url.
     * @param downloadLink download url.
     */
    inner class Assets(@SerializedName("browser_download_url") val downloadLink: String)
}
