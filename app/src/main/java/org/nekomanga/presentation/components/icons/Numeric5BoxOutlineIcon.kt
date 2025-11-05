package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric5BoxOutlineIcon: ImageVector
    get() {
        if (_Numeric5BoxOutline != null) {
            return _Numeric5BoxOutline!!
        }
        _Numeric5BoxOutline =
            ImageVector.Builder(
                    name = "Numeric5BoxOutlineIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(15f, 15f)
                        verticalLineTo(13f)
                        curveTo(15f, 11.89f, 14.1f, 11f, 13f, 11f)
                        horizontalLineTo(11f)
                        verticalLineTo(9f)
                        horizontalLineTo(15f)
                        verticalLineTo(7f)
                        horizontalLineTo(9f)
                        verticalLineTo(13f)
                        horizontalLineTo(13f)
                        verticalLineTo(15f)
                        horizontalLineTo(9f)
                        verticalLineTo(17f)
                        horizontalLineTo(13f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 15f)
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

        return _Numeric5BoxOutline!!
    }

@Suppress("ObjectPropertyName") private var _Numeric5BoxOutline: ImageVector? = null
