package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val UngroupIcon: ImageVector
    get() {
        if (_UngroupIcon != null) {
            return _UngroupIcon!!
        }
        _UngroupIcon =
            ImageVector.Builder(
                    name = "UngroupIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(2f, 2f)
                        horizontalLineTo(6f)
                        verticalLineTo(3f)
                        horizontalLineTo(13f)
                        verticalLineTo(2f)
                        horizontalLineTo(17f)
                        verticalLineTo(6f)
                        horizontalLineTo(16f)
                        verticalLineTo(9f)
                        horizontalLineTo(18f)
                        verticalLineTo(8f)
                        horizontalLineTo(22f)
                        verticalLineTo(12f)
                        horizontalLineTo(21f)
                        verticalLineTo(18f)
                        horizontalLineTo(22f)
                        verticalLineTo(22f)
                        horizontalLineTo(18f)
                        verticalLineTo(21f)
                        horizontalLineTo(12f)
                        verticalLineTo(22f)
                        horizontalLineTo(8f)
                        verticalLineTo(18f)
                        horizontalLineTo(9f)
                        verticalLineTo(16f)
                        horizontalLineTo(6f)
                        verticalLineTo(17f)
                        horizontalLineTo(2f)
                        verticalLineTo(13f)
                        horizontalLineTo(3f)
                        verticalLineTo(6f)
                        horizontalLineTo(2f)
                        verticalLineTo(2f)
                        moveTo(18f, 12f)
                        verticalLineTo(11f)
                        horizontalLineTo(16f)
                        verticalLineTo(13f)
                        horizontalLineTo(17f)
                        verticalLineTo(17f)
                        horizontalLineTo(13f)
                        verticalLineTo(16f)
                        horizontalLineTo(11f)
                        verticalLineTo(18f)
                        horizontalLineTo(12f)
                        verticalLineTo(19f)
                        horizontalLineTo(18f)
                        verticalLineTo(18f)
                        horizontalLineTo(19f)
                        verticalLineTo(12f)
                        horizontalLineTo(18f)
                        moveTo(13f, 6f)
                        verticalLineTo(5f)
                        horizontalLineTo(6f)
                        verticalLineTo(6f)
                        horizontalLineTo(5f)
                        verticalLineTo(13f)
                        horizontalLineTo(6f)
                        verticalLineTo(14f)
                        horizontalLineTo(9f)
                        verticalLineTo(12f)
                        horizontalLineTo(8f)
                        verticalLineTo(8f)
                        horizontalLineTo(12f)
                        verticalLineTo(9f)
                        horizontalLineTo(14f)
                        verticalLineTo(6f)
                        horizontalLineTo(13f)
                        moveTo(12f, 12f)
                        horizontalLineTo(11f)
                        verticalLineTo(14f)
                        horizontalLineTo(13f)
                        verticalLineTo(13f)
                        horizontalLineTo(14f)
                        verticalLineTo(11f)
                        horizontalLineTo(12f)
                        verticalLineTo(12f)
                        close()
                    }
                }
                .build()

        return _UngroupIcon!!
    }

@Suppress("ObjectPropertyName") private var _UngroupIcon: ImageVector? = null
