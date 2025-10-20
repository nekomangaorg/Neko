package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MergeCheckIcon: ImageVector
    get() {
        if (_IconName != null) {
            return _IconName!!
        }
        _IconName =
            ImageVector.Builder(
                    name = "IconName",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(13f, 14f)
                        curveTo(9.64f, 14f, 8.54f, 15.35f, 8.18f, 16.24f)
                        curveTo(9.25f, 16.7f, 10f, 17.76f, 10f, 19f)
                        curveTo(10f, 20.66f, 8.66f, 22f, 7f, 22f)
                        reflectiveCurveTo(4f, 20.66f, 4f, 19f)
                        curveTo(4f, 17.69f, 4.83f, 16.58f, 6f, 16.17f)
                        verticalLineTo(7.83f)
                        curveTo(4.83f, 7.42f, 4f, 6.31f, 4f, 5f)
                        curveTo(4f, 3.34f, 5.34f, 2f, 7f, 2f)
                        reflectiveCurveTo(10f, 3.34f, 10f, 5f)
                        curveTo(10f, 6.31f, 9.17f, 7.42f, 8f, 7.83f)
                        verticalLineTo(13.12f)
                        curveTo(8.88f, 12.47f, 10.16f, 12f, 12f, 12f)
                        curveTo(14.67f, 12f, 15.56f, 10.66f, 15.85f, 9.77f)
                        curveTo(14.77f, 9.32f, 14f, 8.25f, 14f, 7f)
                        curveTo(14f, 5.34f, 15.34f, 4f, 17f, 4f)
                        reflectiveCurveTo(20f, 5.34f, 20f, 7f)
                        curveTo(20f, 8.34f, 19.12f, 9.5f, 17.91f, 9.86f)
                        curveTo(17.65f, 11.29f, 16.68f, 14f, 13f, 14f)
                        moveTo(7f, 18f)
                        curveTo(6.45f, 18f, 6f, 18.45f, 6f, 19f)
                        reflectiveCurveTo(6.45f, 20f, 7f, 20f)
                        reflectiveCurveTo(8f, 19.55f, 8f, 19f)
                        reflectiveCurveTo(7.55f, 18f, 7f, 18f)
                        moveTo(7f, 4f)
                        curveTo(6.45f, 4f, 6f, 4.45f, 6f, 5f)
                        reflectiveCurveTo(6.45f, 6f, 7f, 6f)
                        reflectiveCurveTo(8f, 5.55f, 8f, 5f)
                        reflectiveCurveTo(7.55f, 4f, 7f, 4f)
                        moveTo(17f, 6f)
                        curveTo(16.45f, 6f, 16f, 6.45f, 16f, 7f)
                        reflectiveCurveTo(16.45f, 8f, 17f, 8f)
                        reflectiveCurveTo(18f, 7.55f, 18f, 7f)
                        reflectiveCurveTo(17.55f, 6f, 17f, 6f)
                        moveTo(16.75f, 21.16f)
                        lineTo(14f, 18.16f)
                        lineTo(15.16f, 17f)
                        lineTo(16.75f, 18.59f)
                        lineTo(20.34f, 15f)
                        lineTo(21.5f, 16.41f)
                        lineTo(16.75f, 21.16f)
                    }
                }
                .build()

        return _IconName!!
    }

@Suppress("ObjectPropertyName") private var _IconName: ImageVector? = null
