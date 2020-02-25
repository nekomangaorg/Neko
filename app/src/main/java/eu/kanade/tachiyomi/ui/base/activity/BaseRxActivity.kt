package eu.kanade.tachiyomi.ui.base.activity

import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import nucleus.view.NucleusAppCompatActivity

abstract class BaseRxActivity<P : BasePresenter<*>> : NucleusAppCompatActivity<P>() {

    init {
    }
}
