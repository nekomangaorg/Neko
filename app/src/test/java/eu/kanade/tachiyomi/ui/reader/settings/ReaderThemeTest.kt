package eu.kanade.tachiyomi.ui.reader.settings

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderThemeTest {

    private val darkBackground = Color(0xFF121212)
    private val lightBackground = Color(0xFFFEF7FF)

    @Test
    fun `white and black themes do not analyze pages`() {
        assertNull(ReaderTheme.WHITE.smartBaseColor(darkBackground))
        assertNull(ReaderTheme.BLACK.smartBaseColor(lightBackground))
    }

    @Test
    fun `smart by page uses white with any app theme`() {
        assertEquals(Color.White, ReaderTheme.SMART_BY_PAGE.smartBaseColor(darkBackground))
        assertEquals(Color.White, ReaderTheme.SMART_BY_PAGE.smartBaseColor(lightBackground))
    }

    @Test
    fun `smart by theme uses the app background`() {
        assertEquals(darkBackground, ReaderTheme.SMART_BY_THEME.smartBaseColor(darkBackground))
        assertEquals(lightBackground, ReaderTheme.SMART_BY_THEME.smartBaseColor(lightBackground))
    }

    @Test
    fun `smart by theme but black uses black unless the app background is white`() {
        assertEquals(
            Color.Black,
            ReaderTheme.SMART_BY_THEME_BUT_BLACK.smartBaseColor(darkBackground),
        )
        assertEquals(
            lightBackground,
            ReaderTheme.SMART_BY_THEME_BUT_BLACK.smartBaseColor(lightBackground),
        )
    }
}
