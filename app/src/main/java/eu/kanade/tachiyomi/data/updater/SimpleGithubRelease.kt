package eu.kanade.tachiyomi.data.updater

data class SimpleGithubRelease(
    override val info: String,
    override val downloadLink: String,
    override val releaseLink: String,
) : Release
