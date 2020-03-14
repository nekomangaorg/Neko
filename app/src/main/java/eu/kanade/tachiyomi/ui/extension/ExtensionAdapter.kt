package eu.kanade.tachiyomi.ui.extension

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.extension.ExtensionAdapter.OnButtonClickListener
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param listener instance of [OnButtonClickListener].
 */
class ExtensionAdapter(val listener: OnButtonClickListener) :
        FlexibleAdapter<IFlexible<*>>(null, listener, true) {

    val cardBackground = (listener as ExtensionBottomSheet).context.getResourceColor(R.attr
        .background_card)

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val buttonClickListener: ExtensionAdapter.OnButtonClickListener = listener

    interface OnButtonClickListener {
        fun onButtonClick(position: Int)
    }
}
