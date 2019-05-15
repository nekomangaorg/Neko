package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.pref_widget_imageview.view.*

class LoginPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.pref_widget_imageview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)


        if (getPersistedString("").isNullOrEmpty()) {
            holder.itemView.image_view.setImageResource(android.R.color.transparent)
        } else {
            holder.itemView.image_view.setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon.cmd_check).color(ContextCompat.getColor(context, R.color.md_green_500)).sizeDp(20))

        }
    }

    override public fun notifyChanged() {
        super.notifyChanged()
    }

}
