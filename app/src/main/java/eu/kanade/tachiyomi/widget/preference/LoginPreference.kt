package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import eu.kanade.tachiyomi.R

class LoginPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.pref_widget_imageview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.findViewById(R.id.image_view) as? ImageView)?.let { imageView ->
            imageView.setImageResource(
                if (getPersistedString("").isNullOrEmpty()) {
                    android.R.color.transparent
                } else {
                    R.drawable.ic_done_24dp
                },
            )
            imageView.imageTintList =
                ColorStateList.valueOf(Color.parseColor("#FF4CAF50"))
        }
    }

    public override fun notifyChanged() {
        super.notifyChanged()
    }
}
