sed -i 's/mangaListPage.sourceManga.map { sourceManga ->/mangaListPage.sourceManga.toDisplayManga(db, mangaDex.id)/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayRepository.kt
sed -i 's/listResults.sourceManga.map { sourceManga ->/listResults.sourceManga.toDisplayManga(db, mangaDex.id)/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayRepository.kt
sed -i 's/sourceManga.toDisplayManga(db, mangaDex.id)//g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayRepository.kt
sed -i 's/                        }//g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayRepository.kt
