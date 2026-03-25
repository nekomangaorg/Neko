import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'r') as f:
    content = f.read()

# Add missing import
if "import eu.kanade.tachiyomi.ui.main.SnackbarState" not in content:
    content = content.replace(
        "import eu.kanade.tachiyomi.ui.main.AppSnackbarManager",
        "import eu.kanade.tachiyomi.ui.main.AppSnackbarManager\nimport eu.kanade.tachiyomi.ui.main.SnackbarState"
    )

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'w') as f:
    f.write(content)
