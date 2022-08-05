package eu.kanade.tachiyomi.ui.manga.track

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackItem
import eu.kanade.tachiyomi.util.view.inflate

class TrackAdapter(controller: OnClickListener) : RecyclerView.Adapter<TrackHolder>() {

    var items = emptyList<TrackItem>()
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    val rowClickListener: OnClickListener = controller

    fun getItem(index: Int): TrackItem? {
        return items.getOrNull(index)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
        val view = parent.inflate(R.layout.track_item)
        return TrackHolder(view, this)
    }

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.bind(items[position])
    }

    fun indexOf(item: TrackService?): Int {
        return items.indexOfFirst { item?.id == it.service.id }
    }

    interface OnClickListener {
        fun onLogoClick(position: Int)
        fun onTitleClick(position: Int)
        fun onTitleLongClick(position: Int)
        fun onStatusClick(position: Int)
        fun onChaptersClick(position: Int)
        fun onScoreClick(position: Int)
        fun onRemoveClick(position: Int)
        fun onStartDateClick(view: View, position: Int)
        fun onFinishDateClick(view: View, position: Int)
    }
}
