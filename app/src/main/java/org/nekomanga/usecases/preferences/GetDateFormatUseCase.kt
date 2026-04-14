package org.nekomanga.usecases.preferences

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.text.DateFormat

class GetDateFormatUseCase(private val preferences: PreferencesHelper) {
    operator fun invoke(): DateFormat {
        return preferences.dateFormat()
    }
}
