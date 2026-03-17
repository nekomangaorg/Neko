sed -i 's/manga.map { it.toDisplayManga(db, mangaDex.id) }/manga.toDisplayManga(db, mangaDex.id)/g' app/src/main/java/eu/kanade/tachiyomi/ui/similar/SimilarRepository.kt
sed -i 's/mangaListPage.sourceManga.map { sourceManga ->/mangaListPage.sourceManga.toDisplayManga(db, mangaDex.id)/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseRepository.kt
sed -i 's/sourceManga.toDisplayManga(db, mangaDex.id)//g' app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseRepository.kt
sed -i 's/}/}/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseRepository.kt
