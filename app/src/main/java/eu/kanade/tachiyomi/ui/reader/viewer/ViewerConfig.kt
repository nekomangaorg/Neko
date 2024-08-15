package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.domain.reader.ReaderPreferences
import tachiyomi.core.preference.Preference

/** Common configuration for all viewers. */
abstract class ViewerConfig(
    preferences: PreferencesHelper,
    readerPreferences: ReaderPreferences,
    protected val scope: CoroutineScope
) {

    var imagePropertyChangedListener: (() -> Unit)? = null
    var reloadChapterListener: ((Boolean) -> Unit)? = null

    var navigationModeChangedListener: (() -> Unit)? = null
    var navigationModeInvertedListener: (() -> Unit)? = null

    var longTapEnabled = true
    var tappingInverted = ViewerNavigation.TappingInvertMode.NONE
    var doubleTapAnimDuration = 500
    var volumeKeysEnabled = false
    var volumeKeysInverted = false
    var alwaysShowChapterTransition = true

    var navigationOverlayForNewUser = false
    var navigationMode = 0
        protected set

    var doublePageRotate = false
        protected set

    var doublePageRotateReverse = false
        protected set

    abstract var navigator: ViewerNavigation
        protected set

    init {
        readerPreferences.readWithLongTap().register({ longTapEnabled = it })

        readerPreferences.doubleTapAnimSpeed().register({ doubleTapAnimDuration = it })

        readerPreferences.readWithVolumeKeys().register({ volumeKeysEnabled = it })

        readerPreferences.readWithVolumeKeysInverted().register({ volumeKeysInverted = it })

        readerPreferences
            .alwaysShowChapterTransition()
            .register({ alwaysShowChapterTransition = it })
    }

    fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {},
    ) {
        changes()
            .onEach {
                valueAssignment(it)
                onChanged(it)
            }
            .launchIn(scope)
    }

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)
}
