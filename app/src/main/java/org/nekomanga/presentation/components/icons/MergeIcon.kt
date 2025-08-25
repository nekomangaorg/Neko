package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MergeIcon: ImageVector
    get() {
        if (_MergeIcon != null) {
            return _MergeIcon!!
        }
        _MergeIcon =
            ImageVector.Builder(
                    name = "MergeIcon",
                    defaultWidth = 32.dp,
                    defaultHeight = 32.dp,
                    viewportWidth = 32f,
                    viewportHeight = 32f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF444444))) {
                        moveTo(23.308f, 14.459f)
                        curveToRelative(-1.36f, 0f, -2.53f, 0.751f, -3.158f, 1.853f)
                        curveToRelative(-0.164f, -0.012f, -0.325f, -0.026f, -0.496f, -0.026f)
                        curveToRelative(-3.742f, 0f, -7.292f, -2.85f, -8.588f, -6.379f)
                        curveToRelative(0.779f, -0.67f, 1.279f, -1.651f, 1.279f, -2.757f)
                        curveToRelative(0f, -2.017f, -1.637f, -3.654f, -3.654f, -3.654f)
                        reflectiveCurveToRelative(-3.654f, 1.637f, -3.654f, 3.654f)
                        curveToRelative(0f, 1.348f, 0.738f, 2.514f, 1.827f, 3.148f)
                        verticalLineToRelative(11.975f)
                        curveToRelative(-1.089f, 0.633f, -1.827f, 1.799f, -1.827f, 3.147f)
                        curveToRelative(0f, 2.016f, 1.637f, 3.654f, 3.654f, 3.654f)
                        reflectiveCurveToRelative(3.654f, -1.638f, 3.654f, -3.654f)
                        curveToRelative(0f, -1.349f, -0.738f, -2.514f, -1.827f, -3.147f)
                        verticalLineToRelative(-6.574f)
                        curveToRelative(2.403f, 2.542f, 5.72f, 4.24f, 9.135f, 4.24f)
                        curveToRelative(0.182f, 0f, 0.332f, -0.012f, 0.496f, -0.018f)
                        curveToRelative(0.632f, 1.097f, 1.802f, 1.845f, 3.158f, 1.845f)
                        curveToRelative(2.016f, 0f, 3.654f, -1.638f, 3.654f, -3.654f)
                        reflectiveCurveToRelative(-1.638f, -3.654f, -3.654f, -3.654f)
                        close()
                        moveTo(8.692f, 27.248f)
                        curveToRelative(-1.008f, 0f, -1.827f, -0.817f, -1.827f, -1.827f)
                        curveToRelative(0f, -1.008f, 0.819f, -1.827f, 1.827f, -1.827f)
                        curveToRelative(1.011f, 0f, 1.827f, 0.819f, 1.827f, 1.827f)
                        curveToRelative(0f, 1.01f, -0.816f, 1.827f, -1.827f, 1.827f)
                        close()
                        moveTo(8.692f, 8.977f)
                        curveToRelative(-1.008f, 0f, -1.827f, -0.816f, -1.827f, -1.827f)
                        reflectiveCurveToRelative(0.819f, -1.827f, 1.827f, -1.827f)
                        curveToRelative(1.011f, 0f, 1.827f, 0.816f, 1.827f, 1.827f)
                        reflectiveCurveToRelative(-0.816f, 1.827f, -1.827f, 1.827f)
                        close()
                        moveTo(23.308f, 19.94f)
                        curveToRelative(-1.008f, 0f, -1.827f, -0.817f, -1.827f, -1.827f)
                        reflectiveCurveToRelative(0.819f, -1.827f, 1.827f, -1.827f)
                        curveToRelative(1.01f, 0f, 1.827f, 0.816f, 1.827f, 1.827f)
                        reflectiveCurveToRelative(-0.817f, 1.827f, -1.827f, 1.827f)
                        close()
                    }
                }
                .build()

        return _MergeIcon!!
    }

@Suppress("ObjectPropertyName") private var _MergeIcon: ImageVector? = null
