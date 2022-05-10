package eu.kanade.tachiyomi.ui.base.components.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.composethemeadapter3.createMdc3Theme
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces.appTypography

@Composable
fun NekoTheme(
    content: @Composable () -> Unit,
) {

    val context = LocalContext.current

    val (colorScheme) = createMdc3Theme(
        context = context,
        setTextColors = true,
    )

    MaterialTheme(
        colorScheme = colorScheme!!,
        typography = appTypography,
        content = content
    )
}