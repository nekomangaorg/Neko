package eu.kanade.tachiyomi.util.lang

fun Throwable.toDisplayMessage(): String {
    val message = this.message.orEmpty()
    return if (message.isBlank() || message.equals("unknown error", true)) {
        this.javaClass.simpleName
    } else {
        message
    }
}
