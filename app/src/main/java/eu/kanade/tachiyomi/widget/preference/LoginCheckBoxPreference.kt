package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.view.View
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import kotlinx.android.synthetic.main.pref_item_source.view.*

class LoginCheckBoxPreference @JvmOverloads constructor(
        context: Context,
        val source: HttpSource,
        attrs: AttributeSet? = null
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.pref_item_source
    }

    private var onLoginClick: () -> Unit = {}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnClickListener {
            onLoginClick()
        }
        val loginFrame = holder.itemView.login_frame
        if (source is LoginSource) {
            val color = if (source.isLogged())
                ContextCompat.getColor(context, R.color.material_green_500)
            else
                ContextCompat.getColor(context, R.color.material_blue_grey_300)

            holder.itemView.login.setImageDrawable(IconicsDrawable(context).icon(CommunityMaterial.Icon.cmd_account_circle).sizeDp(24).color(color))

            loginFrame.visibility = View.VISIBLE
            loginFrame.setOnClickListener {
                onLoginClick()
            }
        } else {
            loginFrame.visibility = View.GONE
        }
    }

    fun setOnLoginClickListener(block: () -> Unit) {
        onLoginClick = block
    }

    // Make method public
    public override fun notifyChanged() {
        super.notifyChanged()
    }

}