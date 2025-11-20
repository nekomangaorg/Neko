package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IncognitoIcon: ImageVector by
    lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
                name = "IncognitoIcon",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(17.06f, 13f)
                    curveTo(15.2f, 13f, 13.64f, 14.33f, 13.24f, 16.1f)
                    curveTo(12.29f, 15.69f, 11.42f, 15.8f, 10.76f, 16.09f)
                    curveTo(10.35f, 14.31f, 8.79f, 13f, 6.94f, 13f)
                    curveTo(4.77f, 13f, 3f, 14.79f, 3f, 17f)
                    curveTo(3f, 19.21f, 4.77f, 21f, 6.94f, 21f)
                    curveTo(9f, 21f, 10.68f, 19.38f, 10.84f, 17.32f)
                    curveTo(11.18f, 17.08f, 12.07f, 16.63f, 13.16f, 17.34f)
                    curveTo(13.34f, 19.39f, 15f, 21f, 17.06f, 21f)
                    curveTo(19.23f, 21f, 21f, 19.21f, 21f, 17f)
                    curveTo(21f, 14.79f, 19.23f, 13f, 17.06f, 13f)
                    moveTo(6.94f, 19.86f)
                    curveTo(5.38f, 19.86f, 4.13f, 18.58f, 4.13f, 17f)
                    reflectiveCurveTo(5.39f, 14.14f, 6.94f, 14.14f)
                    curveTo(8.5f, 14.14f, 9.75f, 15.42f, 9.75f, 17f)
                    reflectiveCurveTo(8.5f, 19.86f, 6.94f, 19.86f)
                    moveTo(17.06f, 19.86f)
                    curveTo(15.5f, 19.86f, 14.25f, 18.58f, 14.25f, 17f)
                    reflectiveCurveTo(15.5f, 14.14f, 17.06f, 14.14f)
                    curveTo(18.62f, 14.14f, 19.88f, 15.42f, 19.88f, 17f)
                    reflectiveCurveTo(18.61f, 19.86f, 17.06f, 19.86f)
                    moveTo(22f, 10.5f)
                    horizontalLineTo(2f)
                    verticalLineTo(12f)
                    horizontalLineTo(22f)
                    verticalLineTo(10.5f)
                    moveTo(15.53f, 2.63f)
                    curveTo(15.31f, 2.14f, 14.75f, 1.88f, 14.22f, 2.05f)
                    lineTo(12f, 2.79f)
                    lineTo(9.77f, 2.05f)
                    lineTo(9.72f, 2.04f)
                    curveTo(9.19f, 1.89f, 8.63f, 2.17f, 8.43f, 2.68f)
                    lineTo(6f, 9f)
                    horizontalLineTo(18f)
                    lineTo(15.56f, 2.68f)
                    lineTo(15.53f, 2.63f)
                    close()
                }
            }
            .build()
    }
