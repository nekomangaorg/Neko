package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.android.synthetic.main.pref_item_source.view.*

class SiteLoginPreference @JvmOverloads constructor(
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
        val color = if (source.isLogged())
            ContextCompat.getColor(context, R.color.material_green_500)
        else
            ContextCompat.getColor(context, R.color.material_blue_grey_300)

        holder.itemView.login
                .setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_account_circle).apply {
                    sizeDp = 24
                    colorInt = color
                })

        loginFrame.visibility = View.VISIBLE
        loginFrame.setOnClickListener {
            onLoginClick()
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
