package eu.kanade.tachiyomi.ui.manga.track

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import coil.clear
import coil.load
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchItemBinding
import eu.kanade.tachiyomi.util.view.inflate
import java.util.ArrayList
import java.util.Locale

class TrackSearchAdapter(context: Context) :
    ArrayAdapter<TrackSearch>(context, R.layout.track_search_item, ArrayList<TrackSearch>()) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var v = view
        // Get the data item for this position
        val track = getItem(position)!!
        // Check if an existing view is being reused, otherwise inflate the view
        val holder: TrackSearchHolder // view lookup cache stored in tag
        if (v == null) {
            v = parent.inflate(R.layout.track_search_item)
            holder = TrackSearchHolder(v)
            v.tag = holder
        } else {
            holder = v.tag as TrackSearchHolder
        }
        holder.onSetValues(track)
        return v
    }

    fun setItems(syncs: List<TrackSearch>) {
        setNotifyOnChange(false)
        clear()
        addAll(syncs)
        notifyDataSetChanged()
    }

    class TrackSearchHolder(private val view: View) {

        fun onSetValues(track: TrackSearch) {
            val binding = TrackSearchItemBinding.bind(view)
            binding.trackSearchTitle.text = track.title
            binding.trackSearchSummary.text = track.summary
            binding.trackSearchCover.clear()
            if (track.cover_url.isNotEmpty()) {
                binding.trackSearchCover.load(track.cover_url)
            }

            if (track.publishing_status.isBlank()) {
                binding.trackSearchStatus.isVisible = false
                binding.trackSearchStatusResult.isVisible = false
            } else {
                binding.trackSearchStatusResult.text = track.publishing_status.capitalize(Locale.ROOT)
            }

            if (track.publishing_type.isBlank()) {
                binding.trackSearchType.isVisible = false
                binding.trackSearchTypeResult.isVisible = false
            } else {
                binding.trackSearchTypeResult.text = track.publishing_type.capitalize(Locale.ROOT)
            }

            if (track.start_date.isBlank()) {
                binding.trackSearchStart.isVisible = false
                binding.trackSearchStartResult.isVisible = false
            } else {
                binding.trackSearchStartResult.text = track.start_date
            }
        }
    }
}
