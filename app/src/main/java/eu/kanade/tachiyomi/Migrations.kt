package eu.kanade.tachiyomi

import eu.kanade.tachiyomi.data.preference.PreferencesHelper

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        return false
    }

}