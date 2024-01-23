package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupMergeManga(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var title: String = "",
    @ProtoNumber(3) var coverUrl: String = "",
    @ProtoNumber(4) var mergeType: Int,
) {
    fun toMergeMangaImpl(): MergeMangaImpl {
        return MergeMangaImpl(
            mangaId = 0L,
            title = this.title,
            url = this.url,
            coverUrl = this.coverUrl,
            mergeType = MergeType.getById(this.mergeType),
        )
    }

    companion object {
        fun copyFrom(mergeManga: MergeMangaImpl): BackupMergeManga {
            return BackupMergeManga(
                url = mergeManga.url,
                coverUrl = mergeManga.coverUrl,
                title = mergeManga.title,
                mergeType = mergeManga.mergeType.id,
            )
        }
    }
}
