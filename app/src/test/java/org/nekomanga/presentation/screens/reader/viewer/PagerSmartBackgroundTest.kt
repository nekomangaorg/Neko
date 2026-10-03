package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.junit.Assert.assertEquals
import org.junit.Test

class PagerSmartBackgroundTest {

    private val page = ReaderPage(index = 0)
    private val extraPage = ReaderPage(index = 1)

    private fun source(
        page: ReaderPage = this.page,
        extraPage: ReaderPage? = null,
        isRtl: Boolean = false,
        invertDoublePages: Boolean = false,
        rotateWide: Boolean = false,
        rotateReverse: Boolean = false,
    ) =
        smartBackgroundSource(
            page = page,
            extraPage = extraPage,
            isRtl = isRtl,
            invertDoublePages = invertDoublePages,
            rotateWide = rotateWide,
            rotateReverse = rotateReverse,
        )

    @Test
    fun `double pages are ordered like DoublePageLayout`() {
        assertEquals(
            SmartBackgroundSource.Double(left = page, right = extraPage),
            source(extraPage = extraPage),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = extraPage, right = page),
            source(extraPage = extraPage, isRtl = true),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = extraPage, right = page),
            source(extraPage = extraPage, invertDoublePages = true),
        )
        assertEquals(
            SmartBackgroundSource.Double(left = page, right = extraPage),
            source(extraPage = extraPage, isRtl = true, invertDoublePages = true),
        )
    }

    @Test
    fun `split page uses the half SplitPageLayout shows`() {
        val firstHalf = ReaderPage(index = 0).apply { this.firstHalf = true }
        val secondHalf = ReaderPage(index = 0).apply { this.firstHalf = false }

        assertEquals(
            SmartBackgroundSource.Split(firstHalf, leftHalf = true),
            source(page = firstHalf),
        )
        assertEquals(
            SmartBackgroundSource.Split(firstHalf, leftHalf = false),
            source(page = firstHalf, isRtl = true),
        )
        assertEquals(
            SmartBackgroundSource.Split(secondHalf, leftHalf = false),
            source(page = secondHalf),
        )
        assertEquals(
            SmartBackgroundSource.Split(secondHalf, leftHalf = true),
            source(page = secondHalf, isRtl = true),
        )
    }

    @Test
    fun `single page carries the rotation settings`() {
        assertEquals(
            SmartBackgroundSource.Single(page, rotateWide = true, rotateReverse = true),
            source(rotateWide = true, rotateReverse = true),
        )
        assertEquals(
            SmartBackgroundSource.Single(page, rotateWide = false, rotateReverse = false),
            source(),
        )
    }
}
