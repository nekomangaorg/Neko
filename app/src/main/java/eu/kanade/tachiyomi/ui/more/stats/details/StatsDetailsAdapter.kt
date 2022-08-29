package eu.kanade.tachiyomi.ui.more.stats.details

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ListStatsDetailsBinding
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import eu.kanade.tachiyomi.ui.more.stats.details.StatsDetailsPresenter.Stats
import eu.kanade.tachiyomi.ui.more.stats.details.StatsDetailsPresenter.StatsData
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal

class StatsDetailsAdapter(
    private val context: Context,
    var list: MutableList<StatsData>,
    private val stat: Stats,
    private val listCopy: MutableList<StatsData> = list,
) : RecyclerView.Adapter<StatsDetailsAdapter.StatsDetailsHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsDetailsHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_stats_details, parent, false)
        return StatsDetailsHolder(view)
    }

    override fun onBindViewHolder(holder: StatsDetailsHolder, position: Int) {
        when (stat) {
            Stats.SCORE -> handleScoreLayout(holder, position)
            Stats.TAG, Stats.SOURCE -> handleRankedLayout(holder, position)
            Stats.READ_DURATION -> handleDurationLayout(holder, position)
            else -> handleLayout(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun handleLayout(holder: StatsDetailsHolder, position: Int) {
        val item = list[position]
        holder.statsRankLayout.isVisible = false
        holder.statsDataLayout.isVisible = true
        holder.statsScoreStarImage.isVisible = true
        holder.statsSublabelText.isVisible = false

        holder.statsLabelText.setTextColor(
            item.color ?: context.getResourceColor(R.attr.colorOnBackground),
        )
        holder.statsLabelText.text = item.label
        holder.statsCountText.text = getCountText(item)
        holder.statsCountPercentageText.text = getCountPercentageText(item)
        holder.statsProgressText.text = getProgressText(item)
        holder.statsProgressPercentageText.text = getProgressPercentageString(item)
        val score = item.meanScore?.roundToTwoDecimal()?.toString() ?: ""
        holder.statsMeanScoreLayout.isVisible = score.isNotBlank()
        holder.statsScoreText.text = score
        holder.statsReadDurationText.text = item.readDuration.getReadDuration(context.getString(R.string.none))
    }

    private fun handleScoreLayout(holder: StatsDetailsHolder, position: Int) {
        val item = list[position]
        holder.statsRankLayout.isVisible = false
        holder.statsMeanScoreLayout.isVisible = false
        holder.statsDataLayout.isVisible = true
        holder.statsScoreStarImage.isVisible = true
        holder.statsSublabelText.isVisible = false

        holder.statsLabelText.setTextColor(
            item.color ?: context.getResourceColor(R.attr.colorOnBackground),
        )
        holder.statsLabelText.text = item.label
        holder.statsCountText.text = getCountText(item)
        holder.statsCountPercentageText.text = getCountPercentageText(item)
        holder.statsProgressText.text = getProgressText(item)
        holder.statsProgressPercentageText.text = getProgressPercentageString(item)
        holder.statsReadDurationText.text = item.readDuration.getReadDuration(context.getString(R.string.none))
    }

    private fun handleRankedLayout(holder: StatsDetailsHolder, position: Int) {
        val item = list[position]
        holder.statsRankLayout.isVisible = true
        holder.statsDataLayout.isVisible = true
        holder.statsScoreStarImage.isVisible = true
        holder.statsSublabelText.isVisible = false

        holder.statsRankText.text = String.format("%02d.", position + 1)
        holder.statsLabelText.setTextColor(
            item.color ?: context.getResourceColor(R.attr.colorOnBackground),
        )
        holder.statsLabelText.text = item.label
        holder.statsCountText.text = getCountText(item)
        holder.statsCountPercentageText.text = getCountPercentageText(item)
        holder.statsProgressText.text = getProgressText(item)
        holder.statsProgressPercentageText.text = getProgressPercentageString(item)
        val score = item.meanScore?.roundToTwoDecimal()?.toString() ?: ""
        holder.statsMeanScoreLayout.isVisible = score.isNotBlank()
        holder.statsScoreText.text = score
        holder.statsReadDurationText.text = item.readDuration.getReadDuration(context.getString(R.string.none))
    }

    private fun handleDurationLayout(holder: StatsDetailsHolder, position: Int) {
        val item = list[position]
        holder.statsRankLayout.isVisible = true
        holder.statsMeanScoreLayout.isVisible = true
        holder.statsDataLayout.isVisible = false
        holder.statsScoreStarImage.isVisible = false

        holder.statsRankText.text = String.format("%02d.", position + 1)
        holder.statsLabelText.setTextColor(
            item.color ?: context.getResourceColor(R.attr.colorOnBackground),
        )
        holder.statsLabelText.text = item.label
        holder.statsScoreText.text = item.readDuration.getReadDuration(context.getString(R.string.none))
        holder.statsSublabelText.isVisible = !item.subLabel.isNullOrBlank()
        holder.statsSublabelText.text = item.subLabel
    }

    private fun getCountText(item: StatsData): SpannableStringBuilder {
        return SpannableStringBuilder().bold { append(item.count.toString()) }
    }

    private fun getCountPercentageText(item: StatsData): String {
        val sumCount = list.sumOf { it.count.toDouble() }.coerceAtLeast(1.0)
        val percentage = (item.count / sumCount * 100).roundToTwoDecimal()
        return "($percentage%)"
    }

    private fun getProgressText(item: StatsData): SpannableStringBuilder {
        return SpannableStringBuilder().bold { append(item.chaptersRead.toString()) }.apply {
            if (item.totalChapters != 0) append(" / ${item.totalChapters}")
        }
    }

    private fun getProgressPercentageString(item: StatsData): String {
        if (item.chaptersRead == 0) return "(0%)"
        val percentage = (item.chaptersRead / list.sumOf { it.chaptersRead.toDouble() } * 100).roundToTwoDecimal()
        return "($percentage%)"
    }

    fun filter(text: String) {
        list = if (text.isEmpty()) {
            listCopy
        } else {
            listCopy.filter { it.label?.contains(text, true) == true }.toMutableList()
        }
        notifyDataSetChanged()
    }

    class StatsDetailsHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = ListStatsDetailsBinding.bind(view)

        val statsRankLayout = binding.statsRankLayout
        val statsRankText = binding.statsRankText
        val statsLabelText = binding.statsLabelText
        val statsCountText = binding.statsCountText
        val statsCountPercentageText = binding.statsCountPercentageText
        val statsProgressText = binding.statsProgressText
        val statsProgressPercentageText = binding.statsProgressPercentageText
        val statsMeanScoreLayout = binding.statsMeanScoreLayout
        val statsScoreText = binding.statsScoreText
        val statsReadDurationText = binding.statsReadDurationText
        val statsDataLayout = binding.statsDataLayout
        val statsScoreStarImage = binding.statsScoreStarImage
        val statsSublabelText = binding.statsSublabelText
    }
}
