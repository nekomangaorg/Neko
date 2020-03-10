package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.invisible
import eu.kanade.tachiyomi.util.setVectorCompat
import eu.kanade.tachiyomi.util.visibleIf
import kotlinx.android.synthetic.main.track_item.*
import timber.log.Timber

class TrackHolder(val view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    val listener = adapter.rowClickListener

    init {

        logo_container.setOnClickListener {
            Timber.d("logo_container clicked")
            listener.onLogoClick(adapterPosition)
        }
        track_button.setOnClickListener {
            Timber.d("track_button clicked")
            listener.onSetClick(adapterPosition)
        }
        status_container.setOnClickListener {
            Timber.d("status_container clicked")
            listener.onStatusClick(adapterPosition)
        }
        chapters_container.setOnClickListener {
            Timber.d("chapters_container clicked")
            listener.onChaptersClick(adapterPosition)
        }
        score_container.setOnClickListener {
            Timber.d("score_container clicked")
            listener.onScoreClick(adapterPosition)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        try {

            val track = item.track
            track_logo.setVectorCompat(item.service.getLogo())
            logo_container.setBackgroundColor(item.service.getLogoColor())

            track_details.visibleIf { track != null && !item.service.isExternalLink() }
            external_name.visibleIf { item.service.isExternalLink() || track == null }

            if (item.service.isExternalLink()) {
                external_name.text = item.service.name
                track_button.icon = createIcon(CommunityMaterial.Icon2.cmd_open_in_new)
                track_button.setOnClickListener {
                    Timber.d("open in new clicked")
                    listener.onLogoClick(adapterPosition)
                }
                resizeLayout(32f)
            } else {
                if (track != null) {
                    resizeLayout()

                    track_chapters.text = "${track.last_chapter_read}/" +
                        if (track.total_chapters > 0) track.total_chapters else "-"
                    track_status.text = item.service.getStatus(track.status)
                    track_score.text =
                        if (track.score == 0f) "-" else item.service.displayScore(track)
                } else {
                    resizeLayout(36f)
                }
                if (item.service.isMdList()) {
                    modifyTrackButton()
                } else {
                    external_name.text = item.service.name
                    track_button.icon = createIcon(MaterialDesignDx.Icon.gmf_edit)
                    track_button.setOnClickListener { listener.onSetClick(adapterPosition) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun modifyTrackButton() {
        track_button.invisible()
        track_button.isClickable = false
        track_button.isFocusable = false
    }

    fun resizeLayout(height: Float = 96f) {
        val params: ViewGroup.LayoutParams = track_constraint.layoutParams
        params.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            height,
            view.context.resources.displayMetrics
        ).toInt()

        track_constraint.layoutParams = params
    }

    fun createIcon(icon: IIcon): IconicsDrawable {
        return IconicsDrawable(view.context, icon)
    }
}
