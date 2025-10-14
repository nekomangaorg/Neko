package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numeric1BoxOutlineIcon: ImageVector
    get() {
        if (_Numeric1BoxOutline != null) {
            return _Numeric1BoxOutline!!
        }
        _Numeric1BoxOutline =
            ImageVector.Builder(
                    name = "Numeric1BoxOutline",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
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
                        moveTo(12f, 17f)
                        horizontalLineTo(14f)
                        verticalLineTo(7f)
                        horizontalLineTo(10f)
                        verticalLineTo(9f)
                        horizontalLineTo(12f)
                    }
                }
                .build()

        return _Numeric1BoxOutline!!
    }

@Suppress("ObjectPropertyName") private var _Numeric1BoxOutline: ImageVector? = null
