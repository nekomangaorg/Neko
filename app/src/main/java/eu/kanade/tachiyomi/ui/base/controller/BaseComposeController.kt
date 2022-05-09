package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.runtime.Composable
import com.google.android.material.composethemeadapter3.Mdc3Theme
import eu.kanade.tachiyomi.databinding.EmptyComposeControllerBinding
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter

abstract class BaseComposeController<PS : BaseCoroutinePresenter>(bundle: Bundle? = null) :
    BaseCoroutineController<EmptyComposeControllerBinding, PS>(bundle) {

    override fun onViewCreated(view: View) {
        hideToolbar()
        super.onViewCreated(view)

        binding.root.setContent {
            Mdc3Theme {
                ScreenContent()
            }
        }
    }

    override fun onDestroyView(view: View) {
        showToolbar()
        super.onDestroyView(view)
    }

    override fun createBinding(inflater: LayoutInflater) =
        EmptyComposeControllerBinding.inflate(inflater)

    @Composable
    abstract fun ScreenContent()
}
