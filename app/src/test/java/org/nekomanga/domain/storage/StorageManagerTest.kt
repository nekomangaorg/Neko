package org.nekomanga.domain.storage

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import tachiyomi.core.preference.Preference

class StorageManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var directoryPreference: FakeStringPreference
    private lateinit var storagePreferences: StoragePreferences

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { mockk(relaxed = true) }
        mockkStatic(UniFile::class)
        directoryPreference = FakeStringPreference(INITIAL_URI)
        storagePreferences = mockk()
        every { storagePreferences.baseStorageDirectory() } returns directoryPreference
    }

    @After
    fun tearDown() {
        unmockkStatic(UniFile::class)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `a location that cannot hold the app folders still notifies its listeners`() = runBlocking {
        every { UniFile.fromUri(any(), any()) } returnsMany
            listOf(directory(canCreateChildren = true), directory(canCreateChildren = false))

        val storageManager = createStorageManager()

        collectChanges(storageManager) { changes ->
            directoryPreference.set(UNWRITABLE_URI)
            withTimeout(TIMEOUT_MILLIS) { changes.receive() }
        }
    }

    @Test
    fun `a later location change is still observed after one that fails`() = runBlocking {
        val workingDirectory = directory(canCreateChildren = true)
        every { UniFile.fromUri(any(), any()) } returnsMany
            listOf(
                directory(canCreateChildren = true),
                directory(canCreateChildren = false),
                workingDirectory,
            )

        val storageManager = createStorageManager()

        collectChanges(storageManager) { changes ->
            directoryPreference.set(UNWRITABLE_URI)
            withTimeout(TIMEOUT_MILLIS) { changes.receive() }

            directoryPreference.set(WORKING_URI)
            withTimeout(TIMEOUT_MILLIS) { changes.receive() }
        }

        assertNotNull(storageManager.getDownloadsDirectory())
    }

    /**
     * The manager subscribes to the preference on an IO thread and drops the first value it reads
     * there. A location set before that read becomes that first value and is dropped, so wait for
     * the read before a test changes the location.
     */
    private suspend fun createStorageManager(): StorageManager {
        val storageManager = StorageManager(context, storagePreferences)
        withTimeout(TIMEOUT_MILLIS) { directoryPreference.firstValueCollected.await() }
        return storageManager
    }

    private suspend fun collectChanges(
        storageManager: StorageManager,
        block: suspend (Channel<Unit>) -> Unit,
    ) {
        val changes = Channel<Unit>(Channel.UNLIMITED)
        val scope = CoroutineScope(Dispatchers.Default + Job())
        scope.launch { storageManager.baseDirChanges.collect { changes.send(Unit) } }
        try {
            block(changes)
        } finally {
            scope.cancel()
        }
    }

    private fun directory(canCreateChildren: Boolean): UniFile {
        val directory = mockk<UniFile>(relaxed = true)
        every { directory.exists() } returns true
        every { directory.createDirectory(any()) } returns
            if (canCreateChildren) {
                mockk<UniFile>(relaxed = true).also { every { it.exists() } returns true }
            } else {
                null
            }
        return directory
    }

    private class FakeStringPreference(private val default: String) : Preference<String> {
        private val state = MutableStateFlow(default)
        val firstValueCollected = CompletableDeferred<Unit>()

        override fun key() = "storage_dir"

        override fun get() = state.value

        override fun set(value: String) {
            state.value = value
        }

        override fun isSet() = state.value != default

        override fun delete() {
            state.value = default
        }

        override fun defaultValue() = default

        override fun changes(): Flow<String> = state.onEach { firstValueCollected.complete(Unit) }

        override fun stateIn(scope: CoroutineScope): StateFlow<String> = state
    }

    private companion object {
        const val INITIAL_URI = "content://authority/tree/initial"
        const val UNWRITABLE_URI = "content://authority/tree/unwritable"
        const val WORKING_URI = "content://authority/tree/working"
        const val TIMEOUT_MILLIS = 5_000L
    }
}
