package org.nekomanga.presentation.components.dialog

import androidx.compose.runtime.saveable.SaverScope
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PullMangaDexFollowDialogTest {

    private val dummySaverScope =
        object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

    @Test
    fun defaultStateIncludesDefaultOptionsAndSelections() {
        val state = PullMangaDexFollowDialogState()

        assertEquals(PullMangaDexFollowDialogState.defaultStatusOptions, state.followStatusOptions)
        assertEquals(PullMangaDexFollowDialogState.defaultSelectedStatuses, state.selectedStatuses)
        assertTrue(state.isSelected(FollowStatus.READING))
        assertTrue(state.isSelected(FollowStatus.RE_READING))
        assertFalse(state.isSelected(FollowStatus.COMPLETED))
        assertFalse(state.isSelected(FollowStatus.ON_HOLD))
        assertFalse(state.isSelected(FollowStatus.PLAN_TO_READ))
        assertFalse(state.isSelected(FollowStatus.DROPPED))
        assertTrue(state.canConfirm)
    }

    @Test
    fun isSelectedReflectsCustomSelectedStatuses() {
        val state =
            PullMangaDexFollowDialogState(
                selectedStatuses = setOf(FollowStatus.COMPLETED, FollowStatus.PLAN_TO_READ),
            )

        assertTrue(state.isSelected(FollowStatus.COMPLETED))
        assertTrue(state.isSelected(FollowStatus.PLAN_TO_READ))
        assertFalse(state.isSelected(FollowStatus.READING))
        assertFalse(state.isSelected(FollowStatus.RE_READING))
        assertTrue(state.canConfirm)
    }

    @Test
    fun canConfirmIsFalseWhenSelectedStatusesIsEmpty() {
        val state = PullMangaDexFollowDialogState(selectedStatuses = emptySet())

        assertFalse(state.canConfirm)
    }

    @Test
    fun saverPreservesStateAcrossSerialization() {
        val originalState =
            PullMangaDexFollowDialogState(
                followStatusOptions = setOf(FollowStatus.READING, FollowStatus.DROPPED),
                selectedStatuses = setOf(FollowStatus.DROPPED),
            )

        val saved =
            with(PullMangaDexFollowDialogState.Saver) { dummySaverScope.save(originalState) }
        assertNotNull(saved)

        val restored = PullMangaDexFollowDialogState.Saver.restore(saved!!)
        assertEquals(originalState, restored)
    }
}
