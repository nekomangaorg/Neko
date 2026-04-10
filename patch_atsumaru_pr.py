import re

with open('app/src/main/java/eu/kanade/tachiyomi/source/online/merged/atsumaru/Atsumaru.kt', 'r') as f:
    content = f.read()

# Fix fetchChapters
content = re.sub(
    r'''override suspend fun fetchChapters\(mangaUrl: String\): Result<List<SChapterStatusPair>, ResultError> \{
        val response = client\.newCall\(GET\("\$baseUrl/api/manga/allChapters\?mangaId=\$mangaUrl", headers\)\)\.await\(\)

        if \(!response\.isSuccessful\) \{
            response\.closeQuietly\(\)
            return Err\(ResultError\.HttpError\(response\.code, "HTTP \$\{response\.code\}"\)\)
        \}

        val mangaId = mangaUrl
        val scanlatorMap =
            try \{
                val detailsRequest = GET\("\$baseUrl/api/manga/page\?id=\$mangaId", headers\)
                client
                    \.newCall\(detailsRequest\)
                    \.execute\(\)
                    \.use \{
                        json
                            \.decodeFromString<MangaObjectDto>\(it\.body\.string\(\)\)
                            \.mangaPage
                            \.scanlators
                            \?\.associate \{ it\.id to it\.name \}
                    \}
                    \.orEmpty\(\)
            \} catch \(_: Exception\) \{
                emptyMap\(\)
            \}

        val data = json\.decodeFromString<AllChaptersDto>\(response\.body\.string\(\)\)

        val chapters = data\.chapters\.map \{
            it\.toSChapter\(mangaId, it\.scanlationMangaId\?\.let \{ id -> scanlatorMap\[id\] \}\) to false
        \}
        return Ok\(chapters\)
    \}''',
    r'''override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapterStatusPair>, ResultError> {
        val response = client.newCall(GET("$baseUrl/api/manga/allChapters?mangaId=$mangaUrl", headers)).await()

        if (!response.isSuccessful) {
            response.closeQuietly()
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }

        return response.use { res ->
            val mangaId = mangaUrl
            val scanlatorMap =
                try {
                    val detailsRequest = GET("$baseUrl/api/manga/page?id=$mangaId", headers)
                    client
                        .newCall(detailsRequest)
                        .await()
                        .use {
                            json
                                .decodeFromString<MangaObjectDto>(it.body.string())
                                .mangaPage
                                .scanlators
                                ?.associate { it.id to it.name }
                        }
                        .orEmpty()
                } catch (_: Exception) {
                    emptyMap()
                }

            val data = json.decodeFromString<AllChaptersDto>(res.body.string())

            val chapters = data.chapters.map {
                it.toSChapter(mangaId, it.scanlationMangaId?.let { id -> scanlatorMap[id] }) to false
            }
            Ok(chapters)
        }
    }''',
    content
)

# Fix mapIndexed regex
content = re.sub(
    r'''Page\(index, imageUrl = imageUrl\.replaceFirst\(Regex\("\^https\?:?//"\), "https://"\)\)''',
    r'''Page(index, imageUrl = imageUrl.replaceFirst(PROTOCOL_REGEX, "https://"))''',
    content
)

# Add PROTOCOL_REGEX to companion object
content = re.sub(
    r'''companion object \{
        const val name = "Atsumaru"
        const val baseUrl = "https://atsu.moe"
    \}''',
    r'''companion object {
        const val name = "Atsumaru"
        const val baseUrl = "https://atsu.moe"
        private val PROTOCOL_REGEX = Regex("^https?://")
    }''',
    content
)

with open('app/src/main/java/eu/kanade/tachiyomi/source/online/merged/atsumaru/Atsumaru.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/eu/kanade/tachiyomi/source/online/merged/atsumaru/dto/AtsumaruDto.kt', 'r') as f:
    dto_content = f.read()

# Fix SimpleDateFormat thread safety
dto_content = re.sub(
    r'''    companion object \{
        private val DATE_FORMAT by lazy \{
            SimpleDateFormat\("yyyy-MM-dd'T'HH:mm:ss\.SSS'Z'", Locale\.ENGLISH\)\.apply \{
                timeZone = TimeZone\.getTimeZone\("UTC"\)
            \}
        \}
    \}''',
    r'''    companion object {
        private val DATE_FORMAT: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }
    }''',
    dto_content
)

dto_content = re.sub(
    r'''DATE_FORMAT\.parse\(dateStr\)\?\.time \?: 0L''',
    r'''DATE_FORMAT.get()?.parse(dateStr)?.time ?: 0L''',
    dto_content
)

with open('app/src/main/java/eu/kanade/tachiyomi/source/online/merged/atsumaru/dto/AtsumaruDto.kt', 'w') as f:
    f.write(dto_content)
