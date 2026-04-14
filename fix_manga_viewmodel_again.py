import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'r') as f:
    content = f.read()

# Fix import
content = content.replace(
    'import org.nekomanga.usecases.category.CategoryUseCases',
    'import org.nekomanga.usecases.category.CategoryUseCases\nimport org.nekomanga.domain.category.CategoryItem'
)

# Replace addNewCategory
old_add_new_category = """    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val order =
                (_mangaDetailScreenState.value.category.allCategories.maxOfOrNull { it.order }
                    ?: 0) + 1
            categoryUseCases.modifyCategory.addNewCategory(newCategory, order)
        }
    }"""
new_add_new_category = """    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.addNewCategory(newCategory)
        }
    }"""
content = content.replace(old_add_new_category, new_add_new_category)

# Clean up import to use simple name
content = content.replace(
    'private val categoryUseCases: org.nekomanga.usecases.category.CategoryUseCases = org.nekomanga.usecases.category.CategoryUseCases()',
    'private val categoryUseCases = CategoryUseCases()'
)
content = content.replace(
    'private val categoryUseCases = org.nekomanga.usecases.category.CategoryUseCases()',
    'private val categoryUseCases = CategoryUseCases()'
)

with open('app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt', 'w') as f:
    f.write(content)
