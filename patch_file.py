import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'r') as f:
    content = f.read()

# Add trackUseCases property
content = re.sub(
    r'private val chapterUseCases: ChapterUseCases = Injekt\.get\(\)',
    r'private val chapterUseCases: ChapterUseCases = Injekt.get()\n    private val trackUseCases: org.nekomanga.usecases.tracking.TrackUseCases = Injekt.get()',
    content
)

# Replace initialLoad method tracking logic
replacement = """    fun initialLoad() {
        // refresh tracking
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO

            trackUseCases.refreshTracking.refreshTracking(
                mangaId = mangaId,
                onRefreshError = { error, serviceNameRes, errorMessage ->
                    delay(3000)
                    appSnackbarManager.showSnackbar(
                        eu.kanade.tachiyomi.ui.main.SnackbarState(
                            message = errorMessage,
                            fieldRes = serviceNameRes,
                            messageRes = org.nekomanga.R.string.error_refreshing_,
                            snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                        )
                    )
                },
                onChaptersToMarkRead = { chaptersToMark ->
                    markChapters(chaptersToMark, org.nekomanga.domain.chapter.ChapterMarkActions.Read())
                }
            )
        }

        viewModelScope.launchIO {"""

pattern = re.compile(r'    fun initialLoad\(\) \{\n        // refresh tracking\n        viewModelScope\.launchIO \{\n            if \(!isOnline\(\)\) return@launchIO\n.*?        \}\n\n        viewModelScope\.launchIO \{', re.DOTALL)
content = pattern.sub(replacement, content)

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'w') as f:
    f.write(content)
