package eu.kanade.tachiyomi.source.online.english.utils

class MdUtil {
    companion object {
        const val cdnUrl = "https://cdndex.com"

        //guess the thumbnail url is .jpg  this has a ~80% success rate
        fun formThumbUrl(mangaUrl: String): String {
            return cdnUrl + "/images/manga/" + getMangaId(mangaUrl) + ".jpg"
        }

        //Get the ID from the manga url
        fun getMangaId(url: String): String {
            val lastSection = url.trimEnd('/').substringAfterLast("/")
            return if (lastSection.toIntOrNull() != null) {
                lastSection
            } else {
                //this occurs if person has manga from before that had the id/name/
                url.trimEnd('/').substringBeforeLast("/").substringAfterLast("/")
            }
        }

        //creates the manga url from the browse for the api
        fun modifyMangaUrl(url: String): String = url.replace("/title/", "/manga/").substringBeforeLast("/") + "/"

    }
}