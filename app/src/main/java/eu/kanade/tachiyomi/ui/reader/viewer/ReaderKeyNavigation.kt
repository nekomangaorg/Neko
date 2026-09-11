package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.KeyEvent

object ReaderKeyNavigation {
    /**
     * Resolves whether a key event navigates to the next chapter in reading order (true) or the
     * previous chapter in reading order (false). Returns null if the key code is not a chapter
     * navigation shortcut.
     */
    fun resolveAdjacentDirection(keyCode: Int, isRtl: Boolean): Boolean? {
        return when (keyCode) {
            KeyEvent.KEYCODE_N -> true
            KeyEvent.KEYCODE_P -> false
            KeyEvent.KEYCODE_L -> isRtl
            KeyEvent.KEYCODE_R -> !isRtl
            else -> null
        }
    }
}
