package eu.kanade.tachiyomi.util.system

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.materialAlertDialog() = MaterialAlertDialogBuilder(withOriginalWidth())
