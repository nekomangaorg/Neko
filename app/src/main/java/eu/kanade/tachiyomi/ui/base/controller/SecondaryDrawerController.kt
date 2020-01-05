package eu.kanade.tachiyomi.ui.base.controller

import androidx.drawerlayout.widget.DrawerLayout
import android.view.ViewGroup

interface SecondaryDrawerController {

    fun createSecondaryDrawer(drawer: androidx.drawerlayout.widget.DrawerLayout): ViewGroup?

    fun cleanupSecondaryDrawer(drawer: DrawerLayout)
}
