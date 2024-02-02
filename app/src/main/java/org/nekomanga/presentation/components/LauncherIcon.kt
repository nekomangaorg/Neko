package org.nekomanga.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import org.nekomanga.R

@Composable
fun LauncherIcon(size: Dp, iconId: Int = R.mipmap.ic_launcher, onClick: () -> Unit = {}) {
    ResourcesCompat.getDrawable(LocalContext.current.resources, iconId, LocalContext.current.theme)
        ?.let { drawable ->
            val bitmap =
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.requiredSize(size).clickable { onClick() }
            )
        }
}
