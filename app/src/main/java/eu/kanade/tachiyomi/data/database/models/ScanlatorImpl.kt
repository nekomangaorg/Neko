package eu.kanade.tachiyomi.data.database.models

data class Scanlator(
    val name: String,
    val uuid: String,
    val description: String?,
) {
    fun toScanlatorImpl(): ScanlatorImpl {
        return ScanlatorImpl(
            name = this.name,
            uuid = this.uuid,
            description = this.description,
        )
    }
}

data class ScanlatorImpl(
    val id: Long? = null,
    val name: String,
    val uuid: String,
    val description: String?,
)
