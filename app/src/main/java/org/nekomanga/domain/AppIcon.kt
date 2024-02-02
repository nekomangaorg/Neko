package org.nekomanga.domain

import androidx.annotation.DrawableRes
import org.nekomanga.R

data class AppIcon(
    val component: String,
    @DrawableRes val icon: Int,
)

val nekoAppIcons: List<AppIcon> =
    listOf(
        AppIcon(
            component = "org.nekomanga.MainActivityGenLatest",
            icon = R.mipmap.ic_launcher,
        ),
        AppIcon(
            component = "org.nekomanga.MainActivityGen1",
            icon = R.mipmap.ic_launcher_gen1,
        )
    )
