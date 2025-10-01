package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ExpandAllIcon: ImageVector
    get() {
        if (_ExpandAllIcon != null) {
            return _ExpandAllIcon!!
        }
        _ExpandAllIcon =
            ImageVector.Builder(
                    name = "ExpandAllIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF1F1F1F))) {
                        moveTo(480f, 880f)
                        lineTo(240f, 640f)
                        lineToRelative(57f, -57f)
                        lineToRelative(183f, 183f)
                        lineToRelative(183f, -183f)
                        lineToRelative(57f, 57f)
                        lineTo(480f, 880f)
                        close()
                        moveTo(298f, 376f)
                        lineToRelative(-58f, -56f)
                        lineToRelative(240f, -240f)
                        lineToRelative(240f, 240f)
                        lineToRelative(-58f, 56f)
                        lineToRelative(-182f, -182f)
                        lineToRelative(-182f, 182f)
                        close()
                    }
                }
                .build()

        return _ExpandAllIcon!!
    }

@Suppress("ObjectPropertyName") private var _ExpandAllIcon: ImageVector? = null
