package eu.kanade.tachiyomi.ui.extension.details

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.extension.getApplicationIcon
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.inflate
import kotlinx.android.synthetic.main.extension_detail_header.view.*

class ExtensionDetailsHeaderAdapter(private val presenter: ExtensionDetailsPresenter) :
    RecyclerView.Adapter<ExtensionDetailsHeaderAdapter.HeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = parent.inflate(R.layout.extension_detail_header)
        return HeaderViewHolder(view)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.extension_detail_header
    }

    override fun getItemId(position: Int): Long {
        return presenter.pkgName.hashCode().toLong()
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val extension = presenter.extension ?: return
            val context = view.context

            extension.getApplicationIcon(context)?.let { view.extension_icon.setImageDrawable(it) }
            view.extension_title.text = extension.name
            view.extension_version.text = context.getString(R.string.version_, extension.versionName)
            view.extension_lang.text = context.getString(R.string.language_, LocaleHelper.getSourceDisplayName(extension.lang, context))
            view.extension_nsfw.isVisible = extension.isNsfw
            view.extension_pkg.text = extension.pkgName

            view.extension_uninstall_button.setOnClickListener {
                presenter.uninstallExtension()
            }

            if (extension.isObsolete) {
                view.extension_warning_banner.isVisible = true
                view.extension_warning_banner.setText(R.string.obsolete_extension_message)
            }

            if (extension.isUnofficial) {
                view.extension_warning_banner.isVisible = true
                view.extension_warning_banner.setText(R.string.unofficial_extension_message)
            }
        }
    }
}
