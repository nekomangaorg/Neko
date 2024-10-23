package org.nekomanga.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme
import org.nekomanga.presentation.theme.Typefaces.appTypography

@Composable
fun NekoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val (colorScheme) =
        createMdc3Theme(
            context = context,
            layoutDirection = LayoutDirection.Ltr,
            setTextColors = true,
            readTypography = false,
        )

    MaterialTheme(colorScheme = colorScheme!!, typography = appTypography, content = content)
}
