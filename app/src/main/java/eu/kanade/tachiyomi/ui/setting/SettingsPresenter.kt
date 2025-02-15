package eu.kanade.tachiyomi.ui.setting

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import uy.kohesive.injekt.injectLazy

class SettingsPresenter() : BaseCoroutinePresenter<SettingsController>() {
    val preferencesHelper by injectLazy<PreferencesHelper>()
}
