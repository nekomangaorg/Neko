package eu.kanade.tachiyomi.ui.base.controller

import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout

interface SecondaryDrawerController {

    fun createSecondaryDrawer(drawer: androidx.drawerlayout.widget.DrawerLayout): ViewGroup?

    fun cleanupSecondaryDrawer(drawer: DrawerLayout)
}
