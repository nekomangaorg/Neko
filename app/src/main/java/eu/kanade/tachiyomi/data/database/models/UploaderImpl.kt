package eu.kanade.tachiyomi.data.database.models

data class Uploader(val username: String, val uuid: String) {
    fun toUploaderImpl(): UploaderImpl {
        return UploaderImpl(username = this.username, uuid = this.uuid)
    }
}

data class UploaderImpl(val id: Long? = null, val username: String, val uuid: String)
