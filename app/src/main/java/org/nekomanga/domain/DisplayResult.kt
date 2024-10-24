package org.nekomanga.domain

data class DisplayResult(val title: String, val information: String, val uuid: String)

data class SourceResult(val title: String, val information: String, val uuid: String)

fun SourceResult.toDisplayResult(): DisplayResult {
    return DisplayResult(title = this.title, information = this.information, uuid = this.uuid)
}
