package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.databinding.EmptyComposeControllerBinding
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import org.nekomanga.presentation.theme.NekoTheme

abstract class BaseComposeController<PS : BaseCoroutinePresenter<*>>(bundle: Bundle? = null) :
    BaseCoroutineController<EmptyComposeControllerBinding, BaseCoroutinePresenter<*>>(bundle) {

    override fun onViewCreated(view: View) {
        hideToolbar()
        super.onViewCreated(view)

        binding.root.setContent {
            NekoTheme {
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

/**
 * Basic Compose controller without a presenter.
 */
abstract class BasicComposeController : BaseController<EmptyComposeControllerBinding>() {

    override fun createBinding(inflater: LayoutInflater): EmptyComposeControllerBinding =
        EmptyComposeControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        hideToolbar()
        super.onViewCreated(view)

        binding.root.setContent {
            NekoTheme {
                ScreenContent()
            }
        }
    }

    @Composable
    abstract fun ScreenContent()
}
