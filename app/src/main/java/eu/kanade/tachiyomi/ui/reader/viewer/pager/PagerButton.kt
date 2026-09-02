package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatButton

/** A button class to be used by child views of the pager viewer. */
@SuppressLint("ViewConstructor")
class PagerButton(context: Context, val viewer: PagerViewer) : AppCompatButton(context)
