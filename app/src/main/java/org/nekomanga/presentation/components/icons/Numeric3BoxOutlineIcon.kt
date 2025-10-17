package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric3BoxOutlineIcon: ImageVector
    get() {
        if (_Numeric3BoxOutline != null) {
            return _Numeric3BoxOutline!!
        }
        _Numeric3BoxOutline =
            ImageVector.Builder(
                    name = "Numeric3BoxOutline",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(15f, 15f)
                        verticalLineTo(13.5f)
                        arcTo(
                            1.5f,
                            1.5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            13.5f,
                            12f,
                        )
                        arcTo(
                            1.5f,
                            1.5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            15f,
                            10.5f,
                        )
                        verticalLineTo(9f)
                        curveTo(15f, 7.89f, 14.1f, 7f, 13f, 7f)
                        horizontalLineTo(9f)
                        verticalLineTo(9f)
                        horizontalLineTo(13f)
                        verticalLineTo(11f)
                        horizontalLineTo(11f)
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

        return _Numeric3BoxOutline!!
    }

@Suppress("ObjectPropertyName") private var _Numeric3BoxOutline: ImageVector? = null
