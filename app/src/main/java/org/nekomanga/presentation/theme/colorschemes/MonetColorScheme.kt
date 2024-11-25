package org.nekomanga.presentation.theme.colorschemes

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

internal class MonetColorScheme(context: Context) : BaseColorScheme() {

    private val monet =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MonetSystemColorScheme(context)
        } else {
            NekoColorScheme
        }
    override val darkScheme = monet.darkScheme
    override val lightScheme = monet.lightScheme
}

@RequiresApi(Build.VERSION_CODES.S)
private class MonetSystemColorScheme(context: Context) : BaseColorScheme() {
    override val lightScheme = dynamicLightColorScheme(context)
    override val darkScheme = dynamicDarkColorScheme(context)
}
