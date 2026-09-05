package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SkipNext: ImageVector by
    lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
                name = "SkipNext",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f,
            )
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(660f, 720f)
                    verticalLineToRelative(-480f)
                    horizontalLineToRelative(80f)
                    verticalLineToRelative(480f)
                    horizontalLineToRelative(-80f)
                    close()
                    moveTo(220f, 720f)
                    verticalLineToRelative(-480f)
                    lineToRelative(360f, 240f)
                    lineToRelative(-360f, 240f)
                    close()
                    moveTo(300f, 480f)
                    close()
                    moveTo(300f, 570f)
                    lineTo(436f, 480f)
                    lineTo(300f, 390f)
                    verticalLineToRelative(180f)
                    close()
                }
            }
            .build()
    }
