## 2025-02-18 - Hoist Event Callbacks in Compose Lists/Grids
**Learning:** Inline lambdas inside `items` / `itemsIndexed` blocks create new function instances on every recomposition, breaking skippability of item composables even if their data is unchanged. By hoisting the callback signature to accept the item ID/Model and passing a stable function reference (or a lambda that doesn't capture unstable state), we allow Compose to skip recomposition of individual items.
**Action:** Always prefer passing `(Id) -> Unit` or `(Item) -> Unit` to list item composables instead of `() -> Unit` that captures the item.

## 2025-02-18 - Stabilizing Data Classes with Function Properties
**Learning:** Data classes containing function types (e.g., event handlers like `LibraryScreenActions`) are recreated on every recomposition if initialized with unstable method references (e.g., `viewModel::method`) or capturing lambdas. This forces downstream recomposition of all children accepting these objects. Using `remember` to memoize the data class instance stabilizes the object across recompositions where dependencies (ViewModel, Context) remain unchanged.
**Action:** When passing a bag of callbacks to a composable, memoize the data class creation using `remember` with appropriate keys (e.g., `remember(viewModel) { Actions(...) }`).
