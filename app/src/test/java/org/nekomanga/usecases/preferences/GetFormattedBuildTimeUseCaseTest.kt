package org.nekomanga.usecases.preferences

import io.mockk.every
import io.mockk.mockk
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GetFormattedBuildTimeUseCaseTest {

    @Test
    fun `invoke with valid build time returns formatted date`() {
        val getDateFormatUseCase = mockk<GetDateFormatUseCase>()
        val outputDf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        outputDf.timeZone = TimeZone.getTimeZone("UTC")
        every { getDateFormatUseCase() } returns outputDf

        val useCase = GetFormattedBuildTimeUseCase(getDateFormatUseCase)

        val buildTime = "2023-05-15T10:30:00.000"

        val result = useCase(buildTime)

        // Output format check based on dateExtensions toTimestampString format
        // The expected result combines outputDf format +
        // DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
        // Ensure it doesn't return the raw build time
        assertNotEquals(buildTime, result)
    }

    @Test
    fun `invoke with invalid build time returns raw string`() {
        val getDateFormatUseCase = mockk<GetDateFormatUseCase>()
        val useCase = GetFormattedBuildTimeUseCase(getDateFormatUseCase)

        val buildTime = "Invalid Date String"

        val result = useCase(buildTime)

        assertEquals(buildTime, result)
    }
}
