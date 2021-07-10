package eu.kanade.tachiyomi.network

/**
 * Allow retrofit to handle multi values for same query https://github.com/square/retrofit/issues/1324
 */
class ProxyRetrofitQueryMap(m: MutableMap<String, Any>) : HashMap<String, Any>(m) {
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() {
            val originSet: Set<Map.Entry<String?, Any?>> = super.entries
            val newSet: MutableSet<MutableMap.MutableEntry<String, Any>> = HashSet()
            for ((key, entryValue) in originSet) {
                val entryKey =
                    key ?: throw IllegalArgumentException("Query map contained null key.")
                // Skip null values
                requireNotNull(entryValue) { "Query map contained null value for key '$entryKey'." }
                if (entryValue is List<*>) {
                    for (arrayValue in entryValue) {
                        if (arrayValue != null) { // Skip null values
                            val newEntry: MutableMap.MutableEntry<String, Any> =
                                SimpleEntry(entryKey, arrayValue)
                            newSet.add(newEntry)
                        }
                    }
                } else {
                    val newEntry: MutableMap.MutableEntry<String, Any> =
                        SimpleEntry(entryKey, entryValue)
                    newSet.add(newEntry)
                }
            }
            return newSet
        }
}
