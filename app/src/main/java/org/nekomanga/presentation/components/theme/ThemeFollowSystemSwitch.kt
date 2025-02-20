package org.nekomanga.presentation.components.theme

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.core.app.ActivityCompat
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import org.nekomanga.R
import tachiyomi.core.preference.Preference

@Composable
fun ThemeFollowSystemSwitch(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    nightMode: Int,
    nightModePreference: Preference<Int>,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(id = R.string.follow_system_theme), style = textStyle)
        Switch(
            checked = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            onCheckedChange = {
                when (it) {
                    true -> {
                        nightModePreference.set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        (context as? Activity)?.let { activity ->
                            ActivityCompat.recreate(activity)
                        }
                    }
                    false -> nightModePreference.set(context.appDelegateNightMode())
                }
            },
        )
    }
}
