package eu.kanade.tachiyomi.ui.more

import android.content.Context
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.lang.toTimestampString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AboutPresenter : BaseCoroutinePresenter<AboutPresenter>() {
    private val updateChecker by lazy { AppUpdateChecker() }

    fun getFormattedBuildTime(dateFormat: DateFormat): String {
        try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            val buildTime = inputDf.parse(BuildConfig.BUILD_TIME) ?: return BuildConfig.BUILD_TIME

            return buildTime.toTimestampString(dateFormat)
        } catch (e: ParseException) {
            return BuildConfig.BUILD_TIME
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    fun checkForUpdate(context: Context) = flow {
        try {
            emit(updateChecker.checkForUpdate(context, true))
        } catch (error: Exception) {
            XLog.e(error)
            emit(AppUpdateResult.CantCheckForUpdate(error.message ?: "Error"))
        }
    }.flowOn(Dispatchers.IO)
}
