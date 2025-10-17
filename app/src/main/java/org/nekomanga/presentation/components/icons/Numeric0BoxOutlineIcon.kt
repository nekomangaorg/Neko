package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric0BoxOutlineIcon: ImageVector
    get() {
        if (_Numeric0BoxOutline != null) {
            return _Numeric0BoxOutline!!
        }
        _Numeric0BoxOutline =
            ImageVector.Builder(
                    name = "Numeric0BoxOutline",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19f, 19f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        horizontalLineTo(19f)
                        moveTo(19f, 3f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 5f)
                        verticalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 21f)
                        horizontalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 19f)
                        verticalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 3f)
                        horizontalLineTo(19f)
                        moveTo(11f, 7f)
                        horizontalLineTo(13f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 9f)
                        verticalLineTo(15f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13f, 17f)
                        horizontalLineTo(11f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 15f)
                        verticalLineTo(9f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 11f, 7f)
                        moveTo(11f, 9f)
                        verticalLineTo(15f)
                        horizontalLineTo(13f)
                        verticalLineTo(9f)
                        horizontalLineTo(11f)
                        close()
                    }
                }
                .build()

        return _Numeric0BoxOutline!!
    }

@Suppress("ObjectPropertyName") private var _Numeric0BoxOutline: ImageVector? = null
