package org.nekomanga.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.nekomanga.R
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Slim, error-themed bar shown at the top of the main app scaffold whenever the user has been
 * unexpectedly signed out of MangaDex (auth refresh rejected). Renders nothing while signed in or
 * after a manual sign-out.
 *
 * Intentionally not rendered inside [eu.kanade.tachiyomi.ui.reader.ReaderActivity] — the immersive
 * reader has its own UI surface, and the in-helper toast covers the in-reader case.
 */
@Composable
fun MangaDexLogoutBanner(onSignInClick: () -> Unit, modifier: Modifier = Modifier) {
    val preferences = remember { Injekt.get<MangaDexPreferences>() }
    val unexpectedLogout by
        preferences
            .unexpectedLogout()
            .changes()
            .collectAsStateWithLifecycle(initialValue = preferences.unexpectedLogout().get())

    AnimatedVisibility(
        visible = unexpectedLogout,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.ErrorOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.mangadex_session_expired_banner),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSignInClick) { Text(stringResource(R.string.sign_in)) }
                IconButton(onClick = { preferences.unexpectedLogout().set(false) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
        }
    }
}
