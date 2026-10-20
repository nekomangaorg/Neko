package eu.kanade.tachiyomi.ui.reader.domain

import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText

class GetNavigationRegionsUseCaseTest {

    private val useCase = GetNavigationRegionsUseCase()

    @Test
    fun `null navigation returns empty list`() {
        val result = useCase(navigation = null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `DisabledNavigation returns empty list`() {
        val navigation = DisabledNavigation()
        val result = useCase(navigation = navigation)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `KindlishNavigation transforms regions without inversion`() {
        val navigation = KindlishNavigation()
        val result =
            useCase(navigation = navigation, invertMode = ViewerNavigation.TappingInvertMode.NONE)

        assertEquals(2, result.size)

        val nextRegion = result[0]
        assertEquals("NEXT_0.33_0.33", nextRegion.id)
        assertEquals(0.33f, nextRegion.leftFraction, 0.001f)
        assertEquals(0.33f, nextRegion.topFraction, 0.001f)
        assertEquals(1f, nextRegion.rightFraction, 0.001f)
        assertEquals(1f, nextRegion.bottomFraction, 0.001f)
        assertTrue(nextRegion.label is UiText.StringResource)
        assertEquals(R.string.next, (nextRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFF124A), nextRegion.color)

        val prevRegion = result[1]
        assertEquals("PREV_0.0_0.33", prevRegion.id)
        assertEquals(0f, prevRegion.leftFraction, 0.001f)
        assertEquals(0.33f, prevRegion.topFraction, 0.001f)
        assertEquals(0.33f, prevRegion.rightFraction, 0.001f)
        assertEquals(1f, prevRegion.bottomFraction, 0.001f)
        assertTrue(prevRegion.label is UiText.StringResource)
        assertEquals(R.string.previous, (prevRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFFD9DF), prevRegion.color)
    }

    @Test
    fun `KindlishNavigation with HORIZONTAL inversion inverts horizontal coordinates`() {
        val navigation = KindlishNavigation()
        val result =
            useCase(
                navigation = navigation,
                invertMode = ViewerNavigation.TappingInvertMode.HORIZONTAL,
            )

        assertEquals(2, result.size)

        // NEXT was (0.33, 0.33, 1.0, 1.0) -> horizontal invert: (1 - 1.0 = 0.0, 0.33, 1 - 0.33 =
        // 0.67, 1.0)
        val nextRegion = result[0]
        assertEquals(0f, nextRegion.leftFraction, 0.001f)
        assertEquals(0.33f, nextRegion.topFraction, 0.001f)
        assertEquals(0.67f, nextRegion.rightFraction, 0.001f)
        assertEquals(1f, nextRegion.bottomFraction, 0.001f)

        // PREV was (0.0, 0.33, 0.33, 1.0) -> horizontal invert: (1 - 0.33 = 0.67, 0.33, 1 - 0.0 =
        // 1.0, 1.0)
        val prevRegion = result[1]
        assertEquals(0.67f, prevRegion.leftFraction, 0.001f)
        assertEquals(0.33f, prevRegion.topFraction, 0.001f)
        assertEquals(1f, prevRegion.rightFraction, 0.001f)
        assertEquals(1f, prevRegion.bottomFraction, 0.001f)
    }

    @Test
    fun `KindlishNavigation with VERTICAL inversion inverts vertical coordinates`() {
        val navigation = KindlishNavigation()
        val result =
            useCase(
                navigation = navigation,
                invertMode = ViewerNavigation.TappingInvertMode.VERTICAL,
            )

        assertEquals(2, result.size)

        // NEXT was (0.33, 0.33, 1.0, 1.0) -> vertical invert: (0.33, 1 - 1.0 = 0.0, 1.0, 1 - 0.33 =
        // 0.67)
        val nextRegion = result[0]
        assertEquals(0.33f, nextRegion.leftFraction, 0.001f)
        assertEquals(0f, nextRegion.topFraction, 0.001f)
        assertEquals(1f, nextRegion.rightFraction, 0.001f)
        assertEquals(0.67f, nextRegion.bottomFraction, 0.001f)
    }

    @Test
    fun `KindlishNavigation with BOTH inversion inverts both coordinates`() {
        val navigation = KindlishNavigation()
        val result =
            useCase(
                navigation = navigation,
                invertMode = ViewerNavigation.TappingInvertMode.BOTH,
            )

        assertEquals(2, result.size)

        // NEXT was (0.33, 0.33, 1.0, 1.0) -> both invert: (0.0, 0.0, 0.67, 0.67)
        val nextRegion = result[0]
        assertEquals(0f, nextRegion.leftFraction, 0.001f)
        assertEquals(0f, nextRegion.topFraction, 0.001f)
        assertEquals(0.67f, nextRegion.rightFraction, 0.001f)
        assertEquals(0.67f, nextRegion.bottomFraction, 0.001f)
    }

    @Test
    fun `RightAndLeftNavigation maps directional regions correctly in LTR mode`() {
        val navigation = RightAndLeftNavigation()
        val result = useCase(navigation = navigation, isLtr = true)

        assertEquals(2, result.size)

        // Region 0: LEFT -> in LTR becomes PREV
        val leftRegion = result[0]
        assertEquals(R.string.previous, (leftRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFFD9DF), leftRegion.color)

        // Region 1: RIGHT -> in LTR becomes NEXT
        val rightRegion = result[1]
        assertEquals(R.string.next, (rightRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFF124A), rightRegion.color)
    }

    @Test
    fun `RightAndLeftNavigation maps directional regions correctly in RTL mode`() {
        val navigation = RightAndLeftNavigation()
        val result = useCase(navigation = navigation, isLtr = false)

        assertEquals(2, result.size)

        // Region 0: LEFT -> in RTL becomes NEXT
        val leftRegion = result[0]
        assertEquals(R.string.next, (leftRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFF124A), leftRegion.color)

        // Region 1: RIGHT -> in RTL becomes PREV
        val rightRegion = result[1]
        assertEquals(R.string.previous, (rightRegion.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFFD9DF), rightRegion.color)
    }

    @Test
    fun `custom colorProvider overrides default colors`() {
        val navigation = KindlishNavigation()
        val customColor = Color.Magenta
        val result = useCase(navigation = navigation, colorProvider = { customColor })

        assertEquals(2, result.size)
        assertEquals(customColor, result[0].color)
        assertEquals(customColor, result[1].color)
    }

    @Test
    fun `LNavigation maps all 4 regions correctly`() {
        val navigation = LNavigation()
        val result = useCase(navigation = navigation)

        assertEquals(4, result.size)
    }
}
