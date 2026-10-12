package org.nekomanga.presentation.components.snackbar

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.snackbar.SnackbarState

class SnackbarEventTest {

    private val calls = mutableListOf<String>()

    private val event =
        SnackbarState(
            message = "Deleted downloads",
            action = { calls += "action" },
            dismissAction = { calls += "dismiss" },
        )

    @Test
    fun `runs the dismiss action when the snackbar is cancelled while showing`() = runTest {
        val hostState = SnackbarHostState()
        val job = launch { hostState.showSnackbarEvent(event, "Deleted downloads", "Undo") }
        runCurrent()

        job.cancel()
        runCurrent()

        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun `runs the dismiss action when the snackbar is dismissed`() = runTest {
        val hostState = SnackbarHostState()
        val job = launch { hostState.showSnackbarEvent(event, "Deleted downloads", "Undo") }
        runCurrent()

        hostState.currentSnackbarData!!.dismiss()
        job.join()

        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun `runs only the action when the action is tapped`() = runTest {
        val hostState = SnackbarHostState()
        val job = launch { hostState.showSnackbarEvent(event, "Deleted downloads", "Undo") }
        runCurrent()

        hostState.currentSnackbarData!!.performAction()
        job.join()

        assertEquals(listOf("action"), calls)
    }
}
