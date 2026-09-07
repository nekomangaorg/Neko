package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class MangaUpViewerResponse(@ProtoNumber(3) val pageBlocks: List<MangaUpPageBlock> = emptyList())

@Serializable class MangaUpPageBlock(@ProtoNumber(3) val pages: List<MangaUpPage> = emptyList())

@Serializable
class MangaUpPage(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(5) val key: String? = null,
    @ProtoNumber(6) val iv: String? = null,
)
