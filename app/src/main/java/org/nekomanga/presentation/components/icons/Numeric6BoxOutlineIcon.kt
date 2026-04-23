package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric6BoxOutlineIcon: ImageVector by
    lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
                name = "Numeric6BoxOutlineIcon",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f,
            )
            .apply {
                path(fill = SolidColor(Color(0xFF1F1F1F))) {
                    moveTo(440f, 680f)
                    horizontalLineToRelative(80f)
                    quadToRelative(33f, 0f, 56.5f, -23.5f)
                    reflectiveQuadTo(600f, 600f)
                    verticalLineToRelative(-80f)
                    quadToRelative(0f, -33f, -23.5f, -56.5f)
                    reflectiveQuadTo(520f, 440f)
                    horizontalLineToRelative(-80f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(120f)
                    verticalLineToRelative(-80f)
                    lineTo(440f, 280f)
                    quadToRelative(-33f, 0f, -56.5f, 23.5f)
                    reflectiveQuadTo(360f, 360f)
                    verticalLineToRelative(240f)
                    quadToRelative(0f, 33f, 23.5f, 56.5f)
                    reflectiveQuadTo(440f, 680f)
                    close()
                    moveTo(440f, 520f)
                    horizontalLineToRelative(80f)
                    verticalLineToRelative(80f)
                    horizontalLineToRelative(-80f)
                    verticalLineToRelative(-80f)
                    close()
                    moveTo(200f, 840f)
                    quadToRelative(-33f, 0f, -56.5f, -23.5f)
                    reflectiveQuadTo(120f, 760f)
                    verticalLineToRelative(-560f)
                    quadToRelative(0f, -33f, 23.5f, -56.5f)
                    reflectiveQuadTo(200f, 120f)
                    horizontalLineToRelative(560f)
                    quadToRelative(33f, 0f, 56.5f, 23.5f)
                    reflectiveQuadTo(840f, 200f)
                    verticalLineToRelative(560f)
                    quadToRelative(0f, 33f, -23.5f, 56.5f)
                    reflectiveQuadTo(760f, 840f)
                    lineTo(200f, 840f)
                    close()
                    moveTo(200f, 760f)
                    horizontalLineToRelative(560f)
                    verticalLineToRelative(-560f)
                    lineTo(200f, 200f)
                    verticalLineToRelative(560f)
                    close()
                    moveTo(200f, 200f)
                    verticalLineToRelative(560f)
                    verticalLineToRelative(-560f)
                    close()
                }
            }
            .build()
    }
