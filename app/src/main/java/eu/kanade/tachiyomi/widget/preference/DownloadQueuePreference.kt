package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.preference_update_text.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueuePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.preference_update_text
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val extUpdateText = holder.itemView.textView

        val updates = Injekt.get<DownloadManager>().queue.size
        if (updates > 0) {
            extUpdateText.text = context.resources.getQuantityString(R.plurals
                .downloads_pending, updates, updates)
            extUpdateText.visible()
        }
        else {
            extUpdateText.text = null
            extUpdateText.gone()
        }
    }

    public override fun notifyChanged() {
        super.notifyChanged()
    }

}
