package org.nekomanga.usecases.category

class ValidateCategoryNameUseCase {
    sealed interface ValidationResult {
        data object Valid : ValidationResult

        data object Empty : ValidationResult

        data object Duplicate : ValidationResult
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
