package eu.kanade.tachiyomi.source.model

class SChapterImpl : SChapter {

    override lateinit var url: String

    override lateinit var name: String

    override var vol: String = ""

    override var chapter_txt: String = ""

    override var chapter_title: String = ""

    override var date_upload: Long = 0

    override var chapter_number: Float = -1f

    override var scanlator: String? = null

    override var language: String? = null

    override var mangadex_chapter_id: String = ""

    override var old_mangadex_id: String? = null
}
