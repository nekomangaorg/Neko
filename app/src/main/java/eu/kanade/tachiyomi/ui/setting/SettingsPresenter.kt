package eu.kanade.tachiyomi.ui.setting

import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import uy.kohesive.injekt.api.get

class SettingsPresenter() : BaseCoroutinePresenter<SettingsController>() {
    var deepLink: NavKey? = null
}
