package eu.kanade.tachiyomi.extension.all.mangaplus

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class
MangaPlusResponse(
    @ProtoNumber(1) val success: SuccessResult? = null,
    @ProtoNumber(2) val error: ErrorResult? = null,
)

@Serializable
class ErrorResult(
    @ProtoNumber(2) val englishPopup: Popup? = null,
    @ProtoNumber(3) val spanishPopup: Popup? = null,
)

@Serializable
class Popup(
    @ProtoNumber(1) val subject: String = "",
    @ProtoNumber(2) val body: String = "",
)

@Serializable
class SuccessResult(
    @ProtoNumber(10) val mangaViewer: MangaViewer? = null,
)

@Serializable
class MangaViewer(
    @ProtoNumber(1) val pages: List<MangaPlusPage> = emptyList(),
    @ProtoNumber(9) val titleId: Int? = null,
    @ProtoNumber(19) val viewToken: String? = null,
)

@Serializable
class MangaPlusPage(
    @ProtoNumber(1) val mangaPage: MangaPage? = null,
)

@Serializable
class MangaPage(
    @ProtoNumber(1) val imageUrl: String,
    @ProtoNumber(5) val encryptionKey: String? = null,
)
