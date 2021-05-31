package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource

class SiteLoginPreference @JvmOverloads constructor(
    context: Context,
    val source: HttpSource,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.pref_widget_imageview
    }

    private var onLoginClick: () -> Unit = {}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnClickListener {
            onLoginClick()
        }

        val color = when (source.isLogged()) {
            true -> ContextCompat.getColor(context, R.color.gold)
            false -> ContextCompat.getColor(context, R.color.material_on_surface_disabled)

        }


        (holder.findViewById(R.id.image_view) as? ImageView)?.let { imageView ->
            imageView.setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_account_circle).apply {
                    sizeDp = 24
                    colorInt = color
                }
            )
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
