package org.nekomanga.domain

data class DisplayResult(
    val title: String,
    val biography: String,
    val uuid: String,
)

data class SourceResult(
    val title: String,
    val biography: String,
    val uuid: String,
)

fun SourceResult.toDisplayResult(): DisplayResult {
    return DisplayResult(
        title = this.title,
        biography = this.biography,
        uuid = this.uuid,
    )
}
