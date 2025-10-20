package org.nekomanga.presentation.components.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Shapes

val Shapes.Sunny
    get() = GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val radius = width / 2f

        moveTo(width / 2f, 0f)

        val segments = 16
        for (i in 1..segments) {
            val angle = i * (360f / segments)
            val nextAngle = (i + 1) * (360f / segments)

            val outerRadius = radius * (1f + 0.15f * (if (i % 2 == 0) 1f else 0.5f))

            val x =
                radius + outerRadius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y =
                radius + outerRadius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()

            lineTo(x, y)
        }
        close()
    }
