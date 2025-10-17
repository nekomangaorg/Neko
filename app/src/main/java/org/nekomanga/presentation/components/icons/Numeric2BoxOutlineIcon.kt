package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric2BoxOutlineIcon: ImageVector
    get() {
        if (_Numeric2BoxOutline != null) {
            return _Numeric2BoxOutline!!
        }
        _Numeric2BoxOutline =
            ImageVector.Builder(
                    name = "Numeric2BoxOutline",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(15f, 15f)
                        horizontalLineTo(11f)
                        verticalLineTo(13f)
                        horizontalLineTo(13f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 11f)
                        verticalLineTo(9f)
                        curveTo(15f, 7.89f, 14.1f, 7f, 13f, 7f)
                        horizontalLineTo(9f)
                        verticalLineTo(9f)
                        horizontalLineTo(13f)
                        verticalLineTo(11f)
                        horizontalLineTo(11f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 13f)
                        verticalLineTo(17f)
                        horizontalLineTo(15f)
                        moveTo(19f, 19f)
                        horizontalLineTo(5f)
                        verticalLineTo(5f)
                        horizontalLineTo(19f)
                        moveTo(19f, 3f)
                        horizontalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3f, 5f)
                        verticalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5f, 21f)
                        horizontalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 21f, 19f)
                        verticalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 3f)
                        close()
                    }
                }
                .build()

        return _Numeric2BoxOutline!!
    }

@Suppress("ObjectPropertyName") private var _Numeric2BoxOutline: ImageVector? = null
