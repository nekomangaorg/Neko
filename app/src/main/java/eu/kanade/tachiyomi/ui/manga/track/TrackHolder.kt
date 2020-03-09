package eu.kanade.tachiyomi.ui.manga.track

import android.annotation.SuppressLint
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.setVectorCompat
import eu.kanade.tachiyomi.util.visibleIf
import kotlinx.android.synthetic.main.track_item.*

class TrackHolder(view: View, adapter: TrackAdapter) : BaseViewHolder(view) {

    init {
        val listener = adapter.rowClickListener

        logo_container.setOnClickListener { listener.onLogoClick(adapterPosition) }
        track_set.setOnClickListener { listener.onSetClick(adapterPosition) }
        status_container.setOnClickListener { listener.onStatusClick(adapterPosition) }
        chapters_container.setOnClickListener { listener.onChaptersClick(adapterPosition) }
        score_container.setOnClickListener { listener.onScoreClick(adapterPosition) }
        track_set.setOnClickListener { listener.onTitleClick(adapterPosition) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TrackItem) {
        val track = item.track
        track_logo.setVectorCompat(item.service.getLogo())
        logo_container.setBackgroundColor(item.service.getLogoColor())

        track_details.visibleIf { track != null && !item.service.isExternalLink() }
        track_set.visibleIf { track == null || item.service.isExternalLink() }

        if (item.service.isExternalLink()) {
            track_set.isAllCaps = false
            track_set.text = item.service.name
            track_set.setOnClickListener { listener.onLogoClick(adapterPosition) }
        } else {
            if (track == null) {
                track_set.isAllCaps = true
                track_set.text = view.resources.getString(R.string.action_edit)
                track_set.setOnClickListener { listener.onTitleClick(adapterPosition) }
            } else {

                track_chapters.text = "${track.last_chapter_read}/" +
                    if (track.total_chapters > 0) track.total_chapters else "-"
                track_status.text = item.service.getStatus(track.status)
                track_score.text = if (track.score == 0f) "-" else item.service.displayScore(track)

                if (item.service.isMdList()) {
                    title_container.isClickable = false
                    score_container.gone()
                }
            }
        }
        if (track != null) {
            track_chapters.text = "${track.last_chapter_read}/" +
                    if (track.total_chapters > 0) track.total_chapters else "-"
            track_status.text = item.service.getStatus(track.status)
            track_score.text = if (track.score == 0f) "-" else item.service.displayScore(track)
        }
    }
}
