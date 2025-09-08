package eu.kanade.tachiyomi.ui.library.filter

import androidx.annotation.StringRes
import org.nekomanga.presentation.components.UiText

abstract class BaseFilter(val value: Int, @param:StringRes val stringRes: Int? = null) :
    LibraryFilterType {

    override fun toInt(): Int = value

    override fun UiText(): UiText {
        if (stringRes != null) {
            return UiText.StringResource(stringRes)
        }
        return UiText.String("")
    }
}
