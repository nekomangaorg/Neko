package eu.kanade.tachiyomi.source.online.merged.comix

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import org.nekomanga.constants.Constants

@Serializable
data class Term(
    @SerialName("term_id") private val termId: Int,
    private val type: String,
    val title: String,
    private val slug: String,
    private val count: Int?,
)

@Serializable
class Manga(
    @SerialName("hash_id") private val hashId: String,
    private val title: String,
    private val poster: Poster,
) {
    @Serializable
    class Poster(private val small: String, private val medium: String, private val large: String) {
        fun from(quality: String? = "large") =
            when (quality) {
                "large" -> large
                "small" -> small
                else -> medium
            }
    }

    fun toSManga() =
        SManga.create().apply {
            url = "/$hashId"
            title = this@Manga.title
            thumbnail_url = this@Manga.poster.from("large")
        }
}

@Serializable class SingleMangaResponse(val result: Manga)

@Serializable
class Pagination(
    @SerialName("current_page") val page: Int,
    @SerialName("last_page") val lastPage: Int,
)

@Serializable
class SearchResponse(val result: Items) {
    @Serializable class Items(val items: List<Manga>, val pagination: Pagination)
}

@Serializable
class ChapterDetailsResponse(val result: Items) {
    @Serializable class Items(val items: List<Chapter>, val pagination: Pagination)
}

@Serializable
class Chapter(
    @SerialName("chapter_id") private val chapterId: Int,
    @SerialName("scanlation_group_id") val scanlationGroupId: Int,
    val number: Double,
    private val name: String,
    val votes: Int,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("scanlation_group") private val scanlationGroup: ScanlationGroup?,
    @SerialName("is_official")
    @Serializable(with = SafeIntBooleanDeserializer::class)
    val isOfficial: Int,
) {
    @Serializable class ScanlationGroup(val name: String)

    fun toSChapter(mangaId: String) =
        SChapter.create().apply {
            url = "title/$mangaId/$chapterId"
            val chapterText = "Ch." + this@Chapter.number.toString().removeSuffix(".0")
            chapter_txt = chapterText
            name = buildString {
                append(chapterText)
                this@Chapter.name.takeUnless { it.isEmpty() }?.let { append("- $it") }
            }
            date_upload = this@Chapter.updatedAt * 1000
            chapter_number = this@Chapter.number.toFloat()

            val scanlatorList = mutableListOf(Comix.name)

            if (this@Chapter.scanlationGroup != null) {
                scanlatorList.add(this@Chapter.scanlationGroup.name)
            } else if (this@Chapter.isOfficial == 1) {
                scanlatorList.add("Official")
            } else {
                scanlatorList.add("Unknown")
            }
            scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
        }
}

object SafeIntBooleanDeserializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SafeIntBoolean", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: return try {
                    decoder.decodeInt()
} catch (e: Exception) {
                    try {
                        if (decoder.decodeBoolean()) 1 else 0
                    } catch (_: Exception) {
                        0
                    }
                }

        return try {
            val element = jsonDecoder.decodeJsonElement()
            when (element) {
                is JsonPrimitive ->
                    when {
                        element.booleanOrNull != null -> if (element.booleanOrNull == true) 1 else 0
                        element.intOrNull != null -> element.intOrNull ?: 0
                        else ->
                            element.content.toIntOrNull()
                                ?: if (element.content.equals("true", ignoreCase = true)) 1 else 0
                    }
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }
}

@Serializable
class ChapterResponse(val result: Items?) {
    @Serializable
    class Items(@SerialName("chapter_id") val chapterId: Int, val images: List<Images>)

    @Serializable class Images(val url: String)
}
