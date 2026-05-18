package org.nekomanga.usecases.preferences

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tachiyomi.core.preference.Preference

class ToggleDeveloperModeUseCaseTest {

    private val preferences: PreferencesHelper = mockk()
    private val devModePref: Preference<Boolean> = mockk(relaxed = true)

    private lateinit var useCase: ToggleDeveloperModeUseCase

    @Before
    fun setup() {
        every { preferences.developerMode() } returns devModePref
        useCase = ToggleDeveloperModeUseCase(preferences)
    }

    @Test
    fun `when clicked less than 7 times, returns null and does not toggle`() {
        // We simulate clicking 6 times
        for (i in 1..6) {
            val result = useCase()
            assertNull(result)
        }

        verify(exactly = 0) { devModePref.get() }
        verify(exactly = 0) { devModePref.set(any()) }
    }

    @Test
    fun `when clicked 7 times rapidly, returns new state and toggles preference`() {
        every { devModePref.get() } returns false

        // First 6 clicks
        for (i in 1..6) {
            val result = useCase()
            assertNull(result)
        }

        // 7th click
        val finalResult = useCase()

        assertEquals(true, finalResult)
        verify(exactly = 1) { devModePref.get() }
        verify(exactly = 1) { devModePref.set(true) }
    }

    @Test
    fun `when clicks are too far apart, count resets and no toggle occurs`() {
        every { devModePref.get() } returns false

        // First 6 clicks
        for (i in 1..6) {
            useCase()
        }

        // Wait for timeout (simulated by sleep since use case uses System.currentTimeMillis())
        Thread.sleep(501)

        // 7th click (which will be treated as 1st click)
        val resultAfterWait = useCase()

        assertNull(resultAfterWait)
        verify(exactly = 0) { devModePref.get() }
        verify(exactly = 0) { devModePref.set(any()) }
    }

    @Test
    fun `when toggling twice, the value goes back and forth`() {
        // Mock the get() behavior to alternate
        var currentPrefState = false
        every { devModePref.get() } answers { currentPrefState }
        every { devModePref.set(any()) } answers { currentPrefState = firstArg() }

        // Click 7 times
        for (i in 1..6) useCase()
        val firstToggle = useCase()
        assertEquals(true, firstToggle)

        // Click 7 times again
        for (i in 1..6) useCase()
        val secondToggle = useCase()
        assertEquals(false, secondToggle)

        verify(exactly = 2) { devModePref.get() }
        verify(exactly = 2) { devModePref.set(any()) }
    }
}
