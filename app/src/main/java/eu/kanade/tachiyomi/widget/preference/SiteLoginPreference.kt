package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.create
import eu.kanade.tachiyomi.util.system.createWithColorRes
import org.nekomanga.R

class SiteLoginPreference
@JvmOverloads
constructor(
    context: Context,
    val mangaDexLoginHelper: MangaDexLoginHelper,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.pref_widget_imageview
    }

    var username = ""

    private var onLoginClick: () -> Unit = {}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnClickListener { onLoginClick() }

        (holder.findViewById(R.id.image_view) as? ImageView)?.setImageDrawable(
            when (mangaDexLoginHelper.isLoggedIn()) {
                true -> CommunityMaterial.Icon.cmd_account_circle.create(context, 24f)
                false ->
                    CommunityMaterial.Icon.cmd_account_circle.createWithColorRes(
                        context,
                        24f,
                        R.color.material_on_surface_disabled,
                    )
            }
        )
        val summary = (holder.findViewById(android.R.id.summary) as? TextView)
        summary?.isVisible = true
        summary?.text =
            if (username.isBlank()) {
                ""
            } else {
                "Logged in as $username"
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
