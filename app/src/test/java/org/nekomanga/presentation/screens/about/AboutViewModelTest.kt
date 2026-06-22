package org.nekomanga.presentation.screens.about

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.Release
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.usecases.preferences.GetFormattedBuildTimeUseCase
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockPreferences: PreferencesHelper
    private lateinit var mockSecurityPreferences: SecurityPreferences
    private lateinit var mockGetFormattedBuildTimeUseCase: GetFormattedBuildTimeUseCase
    private lateinit var mockAppSnackbarManager: AppSnackbarManager

    private lateinit var viewModel: AboutViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockPreferences = mockk()
        mockSecurityPreferences = mockk()
        mockGetFormattedBuildTimeUseCase = mockk()
        mockAppSnackbarManager = mockk()

        // Mock defaults needed during initialization of AboutViewModel
        val incognitoModePref = mockk<Preference<Boolean>> {
            every { get() } returns false
        }
        every { mockSecurityPreferences.incognitoMode() } returns incognitoModePref
        every { mockGetFormattedBuildTimeUseCase(any()) } returns "Jan 1, 2026"

        Injekt.addSingleton(mockPreferences)
        Injekt.addSingleton(mockSecurityPreferences)
        Injekt.addSingleton(mockGetFormattedBuildTimeUseCase)
        Injekt.addSingleton(mockAppSnackbarManager)

        viewModel = AboutViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        try {
            val fields = Injekt::class.java.declaredFields
            for (field in fields) {
                if (field.name == "registrars") {
                    field.isAccessible = true
                    val map = field.get(Injekt) as MutableMap<*, *>
                    map.clear()
                } else if (field.name.contains("delegate")) {
                    field.isAccessible = true
                    val delegate = field.get(Injekt)
                    if (delegate != null) {
                        val delegateFields = delegate.javaClass.declaredFields
                        for (df in delegateFields) {
                            if (df.name == "registrars") {
                                df.isAccessible = true
                                val map = df.get(delegate) as MutableMap<*, *>
                                map.clear()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `given initial state when initialized then buildTime and incognitoMode are correct`() {
        // Assert
        val state = viewModel.aboutScreenState.value
        assertEquals("Jan 1, 2026", state.buildTime)
        assertFalse(state.incognitoMode)
        assertFalse(state.checkingForUpdates)
        assertFalse(state.shouldShowUpdateDialog)
    }

    @Test
    fun `given notOnlineSnackbar when called then appSnackbarManager shows no network snackbar`() =
        runTest {
            // Arrange
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.notOnlineSnackbar()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals(R.string.no_network_connection, it.messageRes)
                    }
                )
            }
        }

    @Test
    fun `given checkForUpdate when update check is successful with a new update then state is updated`() =
        runTest {
            // Arrange
            mockkConstructor(AppUpdateChecker::class)
            val mockRelease = mockk<Release>()
            val newUpdateResult = AppUpdateResult.NewUpdate(mockRelease)
            coEvery { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) } returns newUpdateResult
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.checkForUpdate()
            advanceUntilIdle()

            // Assert
            val state = viewModel.aboutScreenState.value
            assertTrue(state.shouldShowUpdateDialog)
            assertEquals(newUpdateResult, state.updateResult)
            assertFalse(state.checkingForUpdates)
        }

    @Test
    fun `given checkForUpdate when update check fails then error snackbar is shown`() =
        runTest {
            // Arrange
            mockkConstructor(AppUpdateChecker::class)
            coEvery { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) } returns
                AppUpdateResult.CantCheckForUpdate("Network timeout")
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.checkForUpdate()
            advanceUntilIdle()

            // Assert
            val state = viewModel.aboutScreenState.value
            assertFalse(state.shouldShowUpdateDialog)
            assertFalse(state.checkingForUpdates)
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals("Network timeout", it.message)
                    }
                )
            }
        }

    @Test
    fun `given checkForUpdate when no new update is available then no new updates snackbar is shown`() =
        runTest {
            // Arrange
            mockkConstructor(AppUpdateChecker::class)
            coEvery { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) } returns AppUpdateResult.NoNewUpdate
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.checkForUpdate()
            advanceUntilIdle()

            // Assert
            val state = viewModel.aboutScreenState.value
            assertFalse(state.shouldShowUpdateDialog)
            assertFalse(state.checkingForUpdates)
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals(R.string.no_new_updates_available, it.messageRes)
                    }
                )
            }
        }

    @Test
    fun `given checkForUpdate when already checking for updates then returns early`() =
        runTest {
            // Arrange
            mockkConstructor(AppUpdateChecker::class)
            coEvery { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) } coAnswers {
                delay(100)
                AppUpdateResult.NoNewUpdate
            }
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            // Start the first check for update (will suspend at delay(100))
            viewModel.checkForUpdate()
            runCurrent()

            // Try to trigger a second check for update concurrently while first one is suspended
            viewModel.checkForUpdate()
            advanceUntilIdle()

            // Assert
            // The underlying checker should only be invoked once
            coVerify(exactly = 1) { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) }
        }

    @Test
    fun `given developerMode is false when onVersionClicked clicked 7 times then developerMode is toggled to true`() =
        runTest {
            // Arrange
            val developerModePref = mockk<Preference<Boolean>>(relaxed = true)
            var devModeValue = false
            every { developerModePref.get() } answers { devModeValue }
            every { developerModePref.set(any()) } answers { devModeValue = firstArg() }
            every { mockPreferences.developerMode() } returns developerModePref

            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            repeat(7) {
                viewModel.onVersionClicked()
            }
            advanceUntilIdle()

            // Assert
            assertTrue(devModeValue)
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals(R.string.developer_mode_enabled, it.messageRes)
                    }
                )
            }
        }

    @Test
    fun `given version long clicked when called then build info copied snackbar is shown`() =
        runTest {
            // Arrange
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Act
            viewModel.onVersionLongClicked()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                mockAppSnackbarManager.showSnackbar(
                    withArg {
                        assertEquals(R.string._copied_to_clipboard, it.messageRes)
                        assertEquals(R.string.build_information, it.fieldRes)
                    }
                )
            }
        }

    @Test
    fun `given hideUpdateDialog when called then state shouldShowUpdateDialog is false`() =
        runTest {
            // Arrange
            mockkConstructor(AppUpdateChecker::class)
            val mockRelease = mockk<Release>()
            val newUpdateResult = AppUpdateResult.NewUpdate(mockRelease)
            coEvery { anyConstructed<AppUpdateChecker>().checkForUpdate(any()) } returns newUpdateResult
            coEvery { mockAppSnackbarManager.showSnackbar(any()) } returns Unit

            // Trigger show update dialog first
            viewModel.checkForUpdate()
            advanceUntilIdle()
            assertTrue(viewModel.aboutScreenState.value.shouldShowUpdateDialog)

            // Act
            viewModel.hideUpdateDialog()
            advanceUntilIdle()

            // Assert
            assertFalse(viewModel.aboutScreenState.value.shouldShowUpdateDialog)
        }
}
