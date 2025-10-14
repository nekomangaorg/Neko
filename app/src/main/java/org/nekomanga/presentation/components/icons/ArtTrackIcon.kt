package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArtTrackIcon: ImageVector
    get() {
        if (_Art_track != null) return _Art_track!!

        _Art_track =
            ImageVector.Builder(
                    name = "Art_track",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(120f, 760f)
                        quadToRelative(-33f, 0f, -56.5f, -23.5f)
                        reflectiveQuadTo(40f, 680f)
                        verticalLineToRelative(-400f)
                        quadToRelative(0f, -33f, 23.5f, -56.5f)
                        reflectiveQuadTo(120f, 200f)
                        horizontalLineToRelative(400f)
                        quadToRelative(33f, 0f, 56.5f, 23.5f)
                        reflectiveQuadTo(600f, 280f)
                        verticalLineToRelative(400f)
                        quadToRelative(0f, 33f, -23.5f, 56.5f)
                        reflectiveQuadTo(520f, 760f)
                        close()
                        moveToRelative(0f, -80f)
                        horizontalLineToRelative(400f)
                        verticalLineToRelative(-400f)
                        horizontalLineTo(120f)
                        close()
                        moveToRelative(40f, -80f)
                        horizontalLineToRelative(320f)
                        lineTo(376f, 460f)
                        lineToRelative(-76f, 100f)
                        lineToRelative(-56f, -74f)
                        close()
                        moveToRelative(520f, 160f)
                        verticalLineToRelative(-560f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(560f)
                        close()
                        moveToRelative(160f, 0f)
                        verticalLineToRelative(-560f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(560f)
                        close()
                        moveToRelative(-720f, -80f)
                        verticalLineToRelative(-400f)
                        close()
                    }
                }
                .build()

        return _Art_track!!
    }

private var _Art_track: ImageVector? = null
