# Technical Proposal: Decoupling Category Management Dialogs & Sheets from Validation & Diff Logic

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (In-Composable name uniqueness checks, diff algorithms, and plural formatting)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`AddEditCategoryDialog.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/dialog/AddEditCategoryDialog.kt) and [`EditCategorySheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/EditCategorySheet.kt), UI composables embed business validation rules, set mathematics, and plural resource resolution:
> - In **`AddEditCategoryDialog.kt` (lines 62–100)**:
>   - The dialog takes `currentCategories: List<CategoryItem>`.
>   - Computes duplicate name checks on the fly inside `onValueChange`:
>     `validCategory = currentCategories.none { it.name.equals(categoryText.text, true) }`.
>   - Re-evaluates complex boolean validation logic repeatedly across `isError`, `supportingText`, and `confirmButton.enabled`:
>     `!categoryText.text.isBlank() && (!validCategory && categorySelected.isBlank() || (!validCategory && categoryText.text != categorySelected))`.
> - In **`EditCategorySheet.kt` (lines 176–228)**:
>   - Embeds an extensive domain helper `calculateText(...)` directly in the UI file.
>   - Performs set algebra (`initialIds.contains`, differences, intersections) to determine if categories are being added, removed, kept, or moved.
>   - Dynamically resolves quantity strings (`R.plurals.category_plural`) and updates mutable UI state (`acceptText.value`) on every checkbox toggle.
> - In **[`AddEditCategoriesScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/AddEditCategoriesScreen.kt#L97-L120)**:
>   - Uses category names as ephemeral keys and resolves category IDs via linear name scans:
>     `val id = nonSystemCategories.first { it.name.equals(editCategoryName, true) }.id`.
>
> **What This Proposal Solves:**
> Extracts pure domain UseCases (`ValidateCategoryNameUseCase` and `CalculateCategoryDiffUseCase`), decouples dialogs and sheets from domain entity lists, and converts `AddEditCategoryDialog` and `EditCategorySheet` into stateless, previewable UI components.

---

## 1. Executive Summary & Vision

UI dialogs and bottom sheets should focus exclusively on layout, input capture, and visual feedback. Embedding string normalization, case-insensitive uniqueness validation, set diffing, and plural formatting directly inside Composable functions violates the Single Responsibility Principle and creates brittle, untestable UI code.

### The Objective
Decouple category editing components by:
1. Extracting a pure, unit-tested `ValidateCategoryNameUseCase` for duplicate checking and validation errors.
2. Extracting a `CalculateCategoryDiffUseCase` that computes whether an operation is Add, Move, Remove, or Keep, and outputs localized button labels.
3. Making `AddEditCategoryDialog` completely stateless (receiving `text`, `errorText`, and `canConfirm`).
4. Making `AddEditCategoriesScreen` reference categories by unique ID rather than name-lookup heuristics.

---

## 2. Architectural Design

```mermaid
flowchart LR
    subgraph Current Architecture
        Dialog["AddEditCategoryDialog"] -->|Iterates & Validates| Cats["List<CategoryItem>"]
        Sheet["EditCategorySheet"] -->|calculateText() Set Math| Plurals["Quantity Strings & Diffing"]
    end

    subgraph Proposed Decoupled Architecture
        Input["User Types in Dialog"] --> VM["CategoryViewModel"]
        VM --> VUC["ValidateCategoryNameUseCase"]
        VUC -->|Emits State| DialogUI["Stateless AddEditCategoryDialog\n(text, errorText, canSave)"]

        Toggle["User Checks Category in Sheet"] --> VM
        VM --> DUC["CalculateCategoryDiffUseCase"]
        DUC -->|Emits State| SheetUI["Stateless EditCategorySheet\n(categories, confirmButtonText)"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Category Validation UseCase

```kotlin
class ValidateCategoryNameUseCase {
    sealed interface ValidationResult {
        object Valid : ValidationResult
        object Empty : ValidationResult
        object Duplicate : ValidationResult
    }

    operator fun invoke(
        name: String,
        existingNames: Collection<String>,
        currentName: String = "",
    ): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ValidationResult.Empty
        if (trimmed.equals(currentName.trim(), ignoreCase = true)) return ValidationResult.Valid
        val isDuplicate = existingNames.any { it.trim().equals(trimmed, ignoreCase = true) }
        return if (isDuplicate) ValidationResult.Duplicate else ValidationResult.Valid
    }
}
```

### 3.2 Category Diff UseCase

```kotlin
class CalculateCategoryDiffUseCase {
    data class DiffResult(
        val actionType: CategoryActionType,
        val targetName: String?,
        val count: Int,
    )

    enum class CategoryActionType {
        Add,
        Move,
        Remove,
        Keep,
    }

    operator fun invoke(
        initialCategoryIds: Set<Int>,
        selectedCategoryIds: Set<Int>,
        idToNameMap: Map<Int, String>,
        isAddingToLibrary: Boolean,
    ): DiffResult {
        val sameIds = selectedCategoryIds.intersect(initialCategoryIds)
        val addedIds = selectedCategoryIds - initialCategoryIds
        val removedIds = initialCategoryIds - selectedCategoryIds

        val actionType = when {
            isAddingToLibrary || (initialCategoryIds.isNotEmpty() && addedIds.isNotEmpty() && sameIds.size == initialCategoryIds.size) -> CategoryActionType.Add
            removedIds.isNotEmpty() && addedIds.isEmpty() -> CategoryActionType.Remove
            addedIds.isEmpty() && sameIds.size == initialCategoryIds.size -> CategoryActionType.Keep
            else -> CategoryActionType.Move
        }

        val targetName = when {
            sameIds.size == 1 && actionType == CategoryActionType.Keep -> idToNameMap[sameIds.first()]
            addedIds.size == 1 -> idToNameMap[addedIds.first()]
            else -> null
        }

        val count = when (actionType) {
            CategoryActionType.Add -> addedIds.size
            CategoryActionType.Remove -> removedIds.size
            else -> selectedCategoryIds.size
        }

        return DiffResult(actionType, targetName, count)
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless `AddEditCategoryDialog`

```kotlin
@Composable
fun AddEditCategoryDialog(
    themeColorState: ThemeColorState = defaultThemeColorState(),
    title: String,
    text: String,
    errorText: String? = null,
    canConfirm: Boolean = false,
    onTextChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
)
```
- No longer receives `currentCategories: List<CategoryItem>`.
- Does not compute boolean validation expressions internally.
- Can be previewed with `@Preview` for default, error, and editing states.

### 4.2 Streamlining `EditCategorySheet`
- Remove lines 176–228 (`calculateText(...)`).
- Pass the resolved button label string `confirmButtonText: String` directly into `EditCategorySheet`.
- Checkbox toggle emits `onCategoryToggled(categoryId: Int, isChecked: Boolean)`.

---

## 5. Technical Footprint & Integration

1. **[`AddEditCategoryDialog.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/dialog/AddEditCategoryDialog.kt)**: Hoist state up to caller; remove `currentCategories`.
2. **[`EditCategorySheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/EditCategorySheet.kt)**: Remove internal `calculateText` and `acceptText` mutable state.
3. **[`AddEditCategoriesScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/AddEditCategoriesScreen.kt)**: Switch dialog trigger state from `editCategoryName: String` to `selectedCategoryId: Int?`.
4. **New Unit Tests**: Exhaustive unit tests for `ValidateCategoryNameUseCase` and `CalculateCategoryDiffUseCase`.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement `ValidateCategoryNameUseCase` and unit tests.
- [ ] **Step 2**: Implement `CalculateCategoryDiffUseCase` and unit tests.
- [ ] **Step 3**: Refactor `AddEditCategoryDialog` to be 100% stateless and add `@Preview`.
- [ ] **Step 4**: Remove `calculateText` from `EditCategorySheet.kt` and hoist button string to ViewModel.
- [ ] **Step 5**: Update `AddEditCategoriesScreen.kt` to identify categories by ID.
- [ ] **Step 6**: Validate with `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
