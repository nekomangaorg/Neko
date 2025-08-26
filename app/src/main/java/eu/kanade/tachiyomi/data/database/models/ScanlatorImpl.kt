package eu.kanade.tachiyomi.data.database.models

data class ScanlatorGroup(val name: String, val uuid: String, val description: String?) {
    fun toScanlatorGroupImpl(): ScanlatorGroupImpl {
        return ScanlatorGroupImpl(
            name = this.name,
            uuid = this.uuid,
            description = this.description,
        )
    }
}

data class ScanlatorGroupImpl(
    val id: Long? = null,
    val name: String,
    val uuid: String,
    val description: String?,
)
