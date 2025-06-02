package eu.kanade.tachiyomi.source.online.merged.comick

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

fun getComickFilters(): FilterList { // Renamed from 완료ComickFilters
    return FilterList(
        SortFilter(getSorts()),
        TimeGroup(),
        DemographicGroup(),
        GenreGroup(),
        CompletedFilter(),
    )
}

fun getSorts(): List<Pair<String, String>> = listOf(
    Pair("Популярности", "popular"),
    Pair("Недавнему", "recent"),
    Pair("Новому", "new"),
)
class SortFilter(sorts: List<Pair<String, String>>) :
    Filter.Sort("Сортировка", sorts.map { it.second }.toTypedArray(), Filter.Sort.Selection(0, true))

open class UriPartFilter(displayName: String, private val vals: Array<Pair<String, String>>) :
    Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
    fun toUriPart() = vals[state].second
}

internal class Demographic(name: String, val id: String) : Filter.CheckBox(name)
internal class DemographicGroup : Filter.Group<Demographic>("Демография")

internal class Genre(name: String, val id: String) : Filter.CheckBox(name)
internal class GenreGroup : Filter.Group<Genre>("Жанры")

class CompletedFilter : Filter.CheckBox("Завершено")

class TimeFilter(times: List<Pair<String, String>>) : UriPartFilter("Время", times.toTypedArray())
internal class TimeGroup : Filter.Group<TimeFilter>("Время")
internal fun getTime(): List<Pair<String, String>> = listOf(
    Pair("Все время", "all"),
    Pair("Год", "year"),
    Pair("Месяц", "month"),
    Pair("Неделя", "week"),
    Pair("День", "day"),
)
