package org.nekomanga.presentation.screens.onboarding

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.theme.Themes
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockApp: Application
    private lateinit var mockPreferences: PreferencesHelper
    private lateinit var mockStoragePreferences: StoragePreferences

    private lateinit var lightThemeFlow: MutableStateFlow<Themes>
    private lateinit var darkThemeFlow: MutableStateFlow<Themes>
    private lateinit var nightModeFlow: MutableStateFlow<Int>
    private lateinit var baseStorageDirFlow: MutableStateFlow<String>

    private lateinit var lightThemePref: Preference<Themes>
    private lateinit var darkThemePref: Preference<Themes>
    private lateinit var nightModePref: Preference<Int>
    private lateinit var baseStorageDirPref: Preference<String>

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(testDispatcher)

        mockApp = mockk(relaxed = true)
        mockPreferences = mockk(relaxed = true)
        mockStoragePreferences = mockk(relaxed = true)

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { mockApp.appDelegateNightMode() } returns AppCompatDelegate.MODE_NIGHT_YES

        every { mockApp.getString(R.string.no_location_set) } returns "No location set"
        every { mockApp.getString(R.string.invalid_location, any()) } returns "Invalid location"

        lightThemeFlow = MutableStateFlow(Themes.Neko)
        darkThemeFlow = MutableStateFlow(Themes.Neko)
        nightModeFlow = MutableStateFlow(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        baseStorageDirFlow = MutableStateFlow("")

        lightThemePref = mockk {
            every { get() } answers { lightThemeFlow.value }
            every { changes() } returns lightThemeFlow
            every { set(any()) } answers { lightThemeFlow.value = firstArg() }
        }
        darkThemePref = mockk {
            every { get() } answers { darkThemeFlow.value }
            every { changes() } returns darkThemeFlow
            every { set(any()) } answers { darkThemeFlow.value = firstArg() }
        }
        nightModePref = mockk {
            every { get() } answers { nightModeFlow.value }
            every { changes() } returns nightModeFlow
            every { set(any()) } answers { nightModeFlow.value = firstArg() }
        }
        baseStorageDirPref = mockk {
            every { get() } answers { baseStorageDirFlow.value }
            every { changes() } returns baseStorageDirFlow
            every { defaultValue() } returns ""
            every { isSet() } answers { baseStorageDirFlow.value.isNotBlank() }
            every { set(any()) } answers { baseStorageDirFlow.value = firstArg() }
        }

        every { mockPreferences.lightTheme() } returns lightThemePref
        every { mockPreferences.darkTheme() } returns darkThemePref
        every { mockPreferences.nightMode() } returns nightModePref
        every { mockStoragePreferences.baseStorageDirectory() } returns baseStorageDirPref

        viewModel =
            OnboardingViewModel(
                app = mockApp,
                preferences = mockPreferences,
                storagePreferences = mockStoragePreferences,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `given initial state when initialized then uiState has expected defaults`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("No location set", state.storageStepUiState.currentStoragePath)
        assertFalse(state.storageStepUiState.isStorageConfigured)
        assertEquals(Themes.Neko, state.themeStepUiState.lightTheme)
        assertEquals(Themes.Neko, state.themeStepUiState.darkTheme)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, state.themeStepUiState.nightMode)
        assertTrue(state.themeStepUiState.followSystemTheme)
    }

    @Test
    fun `given dark theme selection when onThemeSelected then dark theme is saved and activity recreate event is dispatched`() =
        runTest {
            viewModel.onThemeSelected(Themes.Monet, isDark = true)
            advanceUntilIdle()

            verify { darkThemePref.set(Themes.Monet) }
            val event = viewModel.eventChannel.first()
            assertEquals(OnboardingEvent.RecreateActivity, event)
        }

    @Test
    fun `given light theme selection when onThemeSelected then light theme is saved and activity recreate event is dispatched`() =
        runTest {
            viewModel.onThemeSelected(Themes.Purple, isDark = false)
            advanceUntilIdle()

            verify { lightThemePref.set(Themes.Purple) }
            val event = viewModel.eventChannel.first()
            assertEquals(OnboardingEvent.RecreateActivity, event)
        }

    @Test
    fun `given follow system toggled true when onFollowSystemToggle then night mode is set to follow system`() =
        runTest {
            viewModel.onFollowSystemToggle(true)
            advanceUntilIdle()

            verify { nightModePref.set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
            val event = viewModel.eventChannel.first()
            assertEquals(OnboardingEvent.RecreateActivity, event)
        }

    @Test
    fun `given follow system toggled false when onFollowSystemToggle then night mode is set to appDelegateNightMode`() =
        runTest {
            viewModel.onFollowSystemToggle(false)
            advanceUntilIdle()

            verify { nightModePref.set(AppCompatDelegate.MODE_NIGHT_YES) }
            val event = viewModel.eventChannel.first()
            assertEquals(OnboardingEvent.RecreateActivity, event)
        }
}
