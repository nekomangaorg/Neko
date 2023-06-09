package eu.kanade.tachiyomi.jobs.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import java.util.concurrent.TimeUnit
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DelayedLibrarySuggestionsJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val libraryPreferences = Injekt.get<LibraryPreferences>()
        if (!libraryPreferences.showSearchSuggestions().isSet()) {
            libraryPreferences.showSearchSuggestions().set(true)
            LibraryPresenter.setSearchSuggestion(libraryPreferences, Injekt.get(), Injekt.get())
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "DelayedLibrarySuggestions"

        fun setupTask(context: Context, enabled: Boolean) {
            if (enabled) {
                val request = OneTimeWorkRequestBuilder<DelayedLibrarySuggestionsJob>()
                    .setInitialDelay(1, TimeUnit.DAYS)
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
