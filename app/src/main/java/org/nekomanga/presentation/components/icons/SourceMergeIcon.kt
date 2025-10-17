package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SourceMergeIcon: ImageVector
    get() {
        if (_SourceMerge != null) {
            return _SourceMerge!!
        }
        _SourceMerge =
            ImageVector.Builder(
                    name = "SourceMerge",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(7f, 3f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10f, 6f)
                        curveTo(10f, 7.29f, 9.19f, 8.39f, 8.04f, 8.81f)
                        curveTo(8.58f, 13.81f, 13.08f, 14.77f, 15.19f, 14.96f)
                        curveTo(15.61f, 13.81f, 16.71f, 13f, 18f, 13f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 16f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 19f)
                        curveTo(16.69f, 19f, 15.57f, 18.16f, 15.16f, 17f)
                        curveTo(10.91f, 16.8f, 9.44f, 15.19f, 8f, 13.39f)
                        verticalLineTo(15.17f)
                        curveTo(9.17f, 15.58f, 10f, 16.69f, 10f, 18f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 21f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 18f)
                        curveTo(4f, 16.69f, 4.83f, 15.58f, 6f, 15.17f)
                        verticalLineTo(8.83f)
                        curveTo(4.83f, 8.42f, 4f, 7.31f, 4f, 6f)
                        arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 3f)
                        moveTo(7f, 5f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 6f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 7f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8f, 6f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 5f)
                        moveTo(7f, 17f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 18f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 19f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8f, 18f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 17f)
                        moveTo(18f, 15f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17f, 16f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 17f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 16f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 15f)
                        close()
                    }
                }
                .build()

        return _SourceMerge!!
    }

@Suppress("ObjectPropertyName") private var _SourceMerge: ImageVector? = null
