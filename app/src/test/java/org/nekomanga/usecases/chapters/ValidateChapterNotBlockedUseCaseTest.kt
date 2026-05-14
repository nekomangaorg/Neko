package org.nekomanga.usecases.chapters

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.nekomanga.constants.Constants

class ValidateChapterNotBlockedUseCaseTest {

    private val useCase = ValidateChapterNotBlockedUseCase()

    @Test
    fun `test empty blocked groups and uploaders returns true`() {
        val result =
            useCase(
                scanlators = listOf("Group A"),
                uploader = "Uploader B",
                blockedGroups = emptySet(),
                blockedUploaders = emptySet(),
            )
        assertTrue(result)
    }

    @Test
    fun `test scanlator in blocked groups returns false`() {
        val result =
            useCase(
                scanlators = listOf("Group A", "Group B"),
                uploader = "Uploader B",
                blockedGroups = setOf("Group B"),
                blockedUploaders = emptySet(),
            )
        assertFalse(result)
    }

    @Test
    fun `test scanlator not in blocked groups returns true`() {
        val result =
            useCase(
                scanlators = listOf("Group A", "Group B"),
                uploader = "Uploader B",
                blockedGroups = setOf("Group C"),
                blockedUploaders = emptySet(),
            )
        assertTrue(result)
    }

    @Test
    fun `test blocked uploader with no group returns false`() {
        val result =
            useCase(
                scanlators = listOf(Constants.NO_GROUP),
                uploader = "Uploader B",
                blockedGroups = emptySet(),
                blockedUploaders = setOf("Uploader B"),
            )
        assertFalse(result)
    }

    @Test
    fun `test blocked uploader with group returns true`() {
        val result =
            useCase(
                scanlators = listOf("Group A"),
                uploader = "Uploader B",
                blockedGroups = emptySet(),
                blockedUploaders = setOf("Uploader B"),
            )
        assertTrue(result)
    }

    @Test
    fun `test no group but allowed uploader returns true`() {
        val result =
            useCase(
                scanlators = listOf(Constants.NO_GROUP),
                uploader = "Uploader B",
                blockedGroups = emptySet(),
                blockedUploaders = setOf("Uploader C"),
            )
        assertTrue(result)
    }

    @Test
    fun `test blocked group and blocked uploader returns false`() {
        val result =
            useCase(
                scanlators = listOf("Group A", Constants.NO_GROUP),
                uploader = "Uploader B",
                blockedGroups = setOf("Group A"),
                blockedUploaders = setOf("Uploader B"),
            )
        assertFalse(result)
    }
}
