import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt', 'r') as f:
    content = f.read()

# Replace addNewCategory
old_add_new_category = """    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val order = (libraryScreenState.value.userCategories.maxOfOrNull { it.order } ?: 0) + 1
            categoryUseCases.modifyCategory.addNewCategory(newCategory, order)
        }
    }"""
new_add_new_category = """    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            categoryUseCases.modifyCategory.addNewCategory(newCategory)
        }
    }"""
content = content.replace(old_add_new_category, new_add_new_category)

with open('app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt', 'w') as f:
    f.write(content)
