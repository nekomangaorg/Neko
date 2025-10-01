package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.bluelinelabs.conductor.Controller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.nekomanga.logging.TimberKt

abstract class BaseController<VB : ViewBinding>(bundle: Bundle? = null) : Controller(bundle) {

    lateinit var binding: VB
    lateinit var viewScope: CoroutineScope
    var isDragging = false

    init {
        addLifecycleListener(
            object : LifecycleListener() {
                override fun postCreateView(controller: Controller, view: View) {
                    onViewCreated(view)
                }

                override fun preCreateView(controller: Controller) {
                    viewScope = MainScope()
                    TimberKt.d { "Create view for ${controller.instance()}" }
                }

                override fun preAttach(controller: Controller, view: View) {
                    TimberKt.d { "Attach view for ${controller.instance()}" }
                }

                override fun preDetach(controller: Controller, view: View) {
                    TimberKt.d { "Detach view for ${controller.instance()}" }
                }

                override fun preDestroyView(controller: Controller, view: View) {
                    viewScope.cancel()
                    TimberKt.d { "Destroy view for ${controller.instance()}" }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        binding = createBinding(inflater)
        return binding.root
    }

    abstract fun createBinding(inflater: LayoutInflater): VB

    open fun onViewCreated(view: View) {}

    private fun Controller.instance(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }
}
