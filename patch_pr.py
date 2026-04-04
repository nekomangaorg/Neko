with open("app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerPageHolder.kt", "r") as f:
    content = f.read()

# Fix the import inside the catch block that might have been left
content = content.replace("                                        import kotlinx.coroutines.CancellationException\n", "")

with open("app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerPageHolder.kt", "w") as f:
    f.write(content)
