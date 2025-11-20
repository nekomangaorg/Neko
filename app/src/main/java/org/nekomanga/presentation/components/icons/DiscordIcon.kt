package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val DiscordIcon: ImageVector by
    lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
                name = "DiscordIcon",
                defaultWidth = 256.dp,
                defaultHeight = 256.dp,
                viewportWidth = 256f,
                viewportHeight = 256f,
            )
            .apply {
                path(fill = SolidColor(Color(0xFF5865F2))) {
                    moveTo(216.86f, 45.1f)
                    curveTo(200.29f, 37.34f, 182.57f, 31.71f, 164.04f, 28.5f)
                    curveTo(161.77f, 32.61f, 159.11f, 38.15f, 157.28f, 42.55f)
                    curveTo(137.58f, 39.58f, 118.07f, 39.58f, 98.74f, 42.55f)
                    curveTo(96.91f, 38.15f, 94.19f, 32.61f, 91.9f, 28.5f)
                    curveTo(73.35f, 31.71f, 55.61f, 37.36f, 39.04f, 45.14f)
                    curveTo(5.62f, 95.65f, -3.44f, 144.9f, 1.09f, 193.46f)
                    curveTo(23.26f, 210.01f, 44.74f, 220.07f, 65.86f, 226.65f)
                    curveTo(71.08f, 219.47f, 75.73f, 211.84f, 79.74f, 203.8f)
                    curveTo(72.1f, 200.9f, 64.79f, 197.32f, 57.89f, 193.17f)
                    curveTo(59.72f, 191.81f, 61.51f, 190.39f, 63.24f, 188.93f)
                    curveTo(105.37f, 208.63f, 151.13f, 208.63f, 192.75f, 188.93f)
                    curveTo(194.51f, 190.39f, 196.3f, 191.81f, 198.11f, 193.17f)
                    curveTo(191.18f, 197.34f, 183.85f, 200.92f, 176.22f, 203.82f)
                    curveTo(180.23f, 211.84f, 184.86f, 219.49f, 190.1f, 226.67f)
                    curveTo(211.24f, 220.09f, 232.74f, 210.03f, 254.91f, 193.46f)
                    curveTo(260.23f, 137.17f, 245.83f, 88.37f, 216.86f, 45.1f)
                    close()
                    moveTo(85.47f, 163.59f)
                    curveTo(72.83f, 163.59f, 62.46f, 151.79f, 62.46f, 137.41f)
                    curveTo(62.46f, 123.04f, 72.61f, 111.21f, 85.47f, 111.21f)
                    curveTo(98.34f, 111.21f, 108.71f, 123.02f, 108.49f, 137.41f)
                    curveTo(108.51f, 151.79f, 98.34f, 163.59f, 85.47f, 163.59f)
                    close()
                    moveTo(170.53f, 163.59f)
                    curveTo(157.88f, 163.59f, 147.51f, 151.79f, 147.51f, 137.41f)
                    curveTo(147.51f, 123.04f, 157.66f, 111.21f, 170.53f, 111.21f)
                    curveTo(183.39f, 111.21f, 193.76f, 123.02f, 193.54f, 137.41f)
                    curveTo(193.54f, 151.79f, 183.39f, 163.59f, 170.53f, 163.59f)
                    close()
                }
            }
            .build()
    }
