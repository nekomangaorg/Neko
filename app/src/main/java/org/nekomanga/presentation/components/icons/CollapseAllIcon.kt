package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CollapseAllIcon: ImageVector
    get() {
        if (_CollapseAllIcon != null) {
            return _CollapseAllIcon!!
        }
        _CollapseAllIcon =
            ImageVector.Builder(
                    name = "CollapseAllIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF1F1F1F))) {
                        moveToRelative(296f, 880f)
                        lineToRelative(-56f, -56f)
                        lineToRelative(240f, -240f)
                        lineToRelative(240f, 240f)
                        lineToRelative(-56f, 56f)
                        lineToRelative(-184f, -184f)
                        lineTo(296f, 880f)
                        close()
                        moveTo(480f, 376f)
                        lineTo(240f, 136f)
                        lineToRelative(56f, -56f)
                        lineToRelative(184f, 184f)
                        lineToRelative(184f, -184f)
                        lineToRelative(56f, 56f)
                        lineToRelative(-240f, 240f)
                        close()
                    }
                }
                .build()

        return _CollapseAllIcon!!
    }

@Suppress("ObjectPropertyName") private var _CollapseAllIcon: ImageVector? = null
