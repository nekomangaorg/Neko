package org.nekomanga.presentation.screens.reader

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText

class NavigationRegionUiModelTest {

    private val defaultModel =
        NavigationRegionUiModel(
            id = "NEXT_0.33_0.33",
            leftFraction = 0.33f,
            topFraction = 0.33f,
            rightFraction = 1f,
            bottomFraction = 1f,
            label = UiText.StringResource(R.string.next),
            color = Color(0xCCFF124A),
        )

    @Test
    fun `given NavigationRegionUiModel, verify properties are correctly assigned`() {
        assertEquals("NEXT_0.33_0.33", defaultModel.id)
        assertEquals(0.33f, defaultModel.leftFraction, 0.0001f)
        assertEquals(0.33f, defaultModel.topFraction, 0.0001f)
        assertEquals(1f, defaultModel.rightFraction, 0.0001f)
        assertEquals(1f, defaultModel.bottomFraction, 0.0001f)
        assertTrue(defaultModel.label is UiText.StringResource)
        assertEquals(R.string.next, (defaultModel.label as UiText.StringResource).resourceId)
        assertEquals(Color(0xCCFF124A), defaultModel.color)
    }

    @Test
    fun `given NavigationRegionUiModel, when copied with new coordinates, preserves other properties`() {
        val updated =
            defaultModel.copy(
                leftFraction = 0f,
                rightFraction = 0.5f,
            )

        assertEquals("NEXT_0.33_0.33", updated.id)
        assertEquals(0f, updated.leftFraction, 0.0001f)
        assertEquals(0.33f, updated.topFraction, 0.0001f)
        assertEquals(0.5f, updated.rightFraction, 0.0001f)
        assertEquals(1f, updated.bottomFraction, 0.0001f)
        assertEquals(defaultModel.label, updated.label)
        assertEquals(defaultModel.color, updated.color)
        assertNotEquals(defaultModel, updated)
    }

    @Test
    fun `given NavigationRegionUiModel, when copied with new color, preserves other properties`() {
        val newColor = Color.Blue
        val updated = defaultModel.copy(color = newColor)

        assertEquals(newColor, updated.color)
        assertEquals(defaultModel.id, updated.id)
        assertEquals(defaultModel.leftFraction, updated.leftFraction, 0.0001f)
        assertEquals(defaultModel.label, updated.label)
        assertNotEquals(defaultModel, updated)
    }

    @Test
    fun `given NavigationRegionUiModel, when copied with new label, preserves other properties`() {
        val newLabel = UiText.String("Custom Label")
        val updated = defaultModel.copy(label = newLabel)

        assertEquals(newLabel, updated.label)
        assertEquals(defaultModel.id, updated.id)
        assertEquals(defaultModel.color, updated.color)
        assertNotEquals(defaultModel, updated)
    }

    @Test
    fun `given identical NavigationRegionUiModel instances, equals and hashCode contract is satisfied`() {
        val duplicate =
            NavigationRegionUiModel(
                id = "NEXT_0.33_0.33",
                leftFraction = 0.33f,
                topFraction = 0.33f,
                rightFraction = 1f,
                bottomFraction = 1f,
                label = UiText.StringResource(R.string.next),
                color = Color(0xCCFF124A),
            )

        assertEquals(defaultModel, duplicate)
        assertEquals(defaultModel.hashCode(), duplicate.hashCode())
    }

    @Test
    fun `given differing properties, models are not equal`() {
        assertNotEquals(defaultModel, defaultModel.copy(id = "DIFFERENT_ID"))
        assertNotEquals(defaultModel, defaultModel.copy(leftFraction = 0.1f))
        assertNotEquals(defaultModel, defaultModel.copy(topFraction = 0.1f))
        assertNotEquals(defaultModel, defaultModel.copy(rightFraction = 0.9f))
        assertNotEquals(defaultModel, defaultModel.copy(bottomFraction = 0.9f))
        assertNotEquals(
            defaultModel,
            defaultModel.copy(label = UiText.StringResource(R.string.previous)),
        )
        assertNotEquals(defaultModel, defaultModel.copy(color = Color.Green))
    }

    @Test
    fun `given NavigationRegionUiModel with String label, text is preserved`() {
        val model = defaultModel.copy(label = UiText.String("Menu"))

        assertTrue(model.label is UiText.String)
        assertEquals("Menu", (model.label as UiText.String).str)
    }
}
