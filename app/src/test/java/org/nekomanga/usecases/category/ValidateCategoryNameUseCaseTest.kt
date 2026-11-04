package org.nekomanga.usecases.category

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class ValidateCategoryNameUseCaseTest {

    private lateinit var useCase: ValidateCategoryNameUseCase

    @Before
    fun setup() {
        useCase = ValidateCategoryNameUseCase()
    }

    @Test
    fun `given empty or blank string when validating name then returns Empty`() {
        useCase("", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Empty
        useCase("   ", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Empty
        useCase("\t\n", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Empty
    }

    @Test
    fun `given existing category name with case insensitivity when validating then returns Duplicate`() {
        useCase("Action", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("action", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("ACTION", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("  comedy  ", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
    }

    @Test
    fun `given same name as current editing category when validating then returns Valid`() {
        useCase("Action", listOf("Action", "Comedy"), currentName = "Action") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
        useCase("action", listOf("Action", "Comedy"), currentName = "Action") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
        useCase("  Action  ", listOf("Action", "Comedy"), currentName = "Action") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
    }

    @Test
    fun `given new unique category name when validating then returns Valid`() {
        useCase("Drama", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
        useCase("Slice of Life", listOf("Action", "Comedy")) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
    }

    @Test
    fun `given existing categories with leading or trailing whitespace when validating candidate name then returns Duplicate`() {
        val existing = listOf("  Action  ", "\tRomance\n")
        useCase("Action", existing) shouldBe ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("action", existing) shouldBe ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("Romance", existing) shouldBe ValidateCategoryNameUseCase.ValidationResult.Duplicate
    }

    @Test
    fun `given empty existing categories list when validating non-blank name then returns Valid`() {
        useCase("Any Category", emptyList()) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
    }

    @Test
    fun `given candidate name with unicode and emoji characters when validating then detects duplicates correctly`() {
        val existing = listOf("マンガ", "Webtoons 🎨")
        useCase("マンガ", existing) shouldBe ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("Webtoons 🎨", existing) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("webtoons 🎨", existing) shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
        useCase("コミック", existing) shouldBe ValidateCategoryNameUseCase.ValidationResult.Valid
    }

    @Test
    fun `given candidate name matching another category while editing when validating then returns Duplicate`() {
        val existing = listOf("Action", "Comedy", "Drama")
        // User is editing "Action" (currentName), but enters "Comedy" (which already exists
        // elsewhere)
        useCase("Comedy", existing, currentName = "Action") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Duplicate
    }

    @Test
    fun `given candidate name with surrounding whitespace matching currentName when validating then returns Valid`() {
        val existing = listOf("Action", "Comedy")
        useCase("  Action  ", existing, currentName = "Action") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
        useCase("Action", existing, currentName = "  Action  ") shouldBe
            ValidateCategoryNameUseCase.ValidationResult.Valid
    }
}
