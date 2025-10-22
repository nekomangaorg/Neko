package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AccountTreeIcon: ImageVector
    get() {
        if (_IconName != null) {
            return _IconName!!
        }
        _IconName =
            ImageVector.Builder(
                    name = "AccountTree",
                    defaultWidth = 960.dp,
                    defaultHeight = 960.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(600f, 840f)
                        verticalLineToRelative(-120f)
                        lineTo(440f, 720f)
                        verticalLineToRelative(-400f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(120f)
                        lineTo(80f, 440f)
                        verticalLineToRelative(-320f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(120f)
                        horizontalLineToRelative(240f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(320f)
                        lineTo(600f, 440f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(320f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(320f)
                        lineTo(600f, 840f)
                        close()
                        moveTo(680f, 360f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        lineTo(680f, 200f)
                        verticalLineToRelative(160f)
                        close()
                        moveTo(680f, 760f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        lineTo(680f, 600f)
                        verticalLineToRelative(160f)
                        close()
                        moveTo(160f, 360f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        lineTo(160f, 200f)
                        verticalLineToRelative(160f)
                        close()
                    }
                }
                .build()

        return _IconName!!
    }

@Suppress("ObjectPropertyName") private var _IconName: ImageVector? = null
