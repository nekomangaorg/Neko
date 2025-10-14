package org.nekomanga.presentation.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AccountTreeIcon: ImageVector
    get() {
        if (_Account_tree != null) return _Account_tree!!

        _Account_tree =
            ImageVector.Builder(
                    name = "Account_tree",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFF000000))) {
                        moveTo(600f, 840f)
                        verticalLineToRelative(-120f)
                        horizontalLineTo(440f)
                        verticalLineToRelative(-400f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(120f)
                        horizontalLineTo(80f)
                        verticalLineToRelative(-320f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(120f)
                        horizontalLineToRelative(240f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(320f)
                        horizontalLineTo(600f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(320f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(320f)
                        close()
                        moveTo(160f, 200f)
                        verticalLineToRelative(160f)
                        close()
                        moveToRelative(520f, 400f)
                        verticalLineToRelative(160f)
                        close()
                        moveToRelative(0f, -400f)
                        verticalLineToRelative(160f)
                        close()
                        moveToRelative(0f, 160f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        horizontalLineTo(680f)
                        close()
                        moveToRelative(0f, 400f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        horizontalLineTo(680f)
                        close()
                        moveTo(160f, 360f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-160f)
                        horizontalLineTo(160f)
                        close()
                    }
                }
                .build()

        return _Account_tree!!
    }

private var _Account_tree: ImageVector? = null
