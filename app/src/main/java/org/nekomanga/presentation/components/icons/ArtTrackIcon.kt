package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArtTrackIcon: ImageVector
    get() {
        if (_IconName != null) {
            return _IconName!!
        }
        _IconName =
            ImageVector.Builder(
                    name = "ArtTrack",
                    defaultWidth = 960.dp,
                    defaultHeight = 960.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
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
                        lineTo(120f, 760f)
                        close()
                        moveTo(120f, 680f)
                        horizontalLineToRelative(400f)
                        verticalLineToRelative(-400f)
                        lineTo(120f, 280f)
                        verticalLineToRelative(400f)
                        close()
                        moveTo(160f, 600f)
                        horizontalLineToRelative(320f)
                        lineTo(376f, 460f)
                        lineToRelative(-76f, 100f)
                        lineToRelative(-56f, -74f)
                        lineToRelative(-84f, 114f)
                        close()
                        moveTo(680f, 760f)
                        verticalLineToRelative(-560f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(560f)
                        horizontalLineToRelative(-80f)
                        close()
                        moveTo(840f, 760f)
                        verticalLineToRelative(-560f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(560f)
                        horizontalLineToRelative(-80f)
                        close()
                    }
                }
                .build()

        return _IconName!!
    }

@Suppress("ObjectPropertyName") private var _IconName: ImageVector? = null
