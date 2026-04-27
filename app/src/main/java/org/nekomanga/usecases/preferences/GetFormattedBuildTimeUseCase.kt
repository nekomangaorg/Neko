package org.nekomanga.usecases.preferences

import eu.kanade.tachiyomi.util.system.toTimestampString
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GetFormattedBuildTimeUseCase(private val getDateFormatUseCase: GetDateFormatUseCase) {
    operator fun invoke(buildTime: String): String {
        return runCatching {
                val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                inputDf.timeZone = TimeZone.getTimeZone("UTC")
                inputDf.parse(buildTime)!!.toTimestampString(getDateFormatUseCase())
            }
            .getOrDefault(buildTime)
    }
}
