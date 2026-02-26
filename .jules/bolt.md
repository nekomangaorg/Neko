## 2025-02-18 - Hoist Event Callbacks in Compose Lists/Grids
**Learning:** Inline lambdas inside `items` / `itemsIndexed` blocks create new function instances on every recomposition, breaking skippability of item composables even if their data is unchanged. By hoisting the callback signature to accept the item ID/Model and passing a stable function reference (or a lambda that doesn't capture unstable state), we allow Compose to skip recomposition of individual items.
**Action:** Always prefer passing `(Id) -> Unit` or `(Item) -> Unit` to list item composables instead of `() -> Unit` that captures the item.

## 2025-02-18 - Stabilizing Data Classes with Function Properties
**Learning:** Data classes containing function types (e.g., event handlers like `LibraryScreenActions`) are recreated on every recomposition if initialized with unstable method references (e.g., `viewModel::method`) or capturing lambdas. This forces downstream recomposition of all children accepting these objects. Using `remember` to memoize the data class instance stabilizes the object across recompositions where dependencies (ViewModel, Context) remain unchanged.
**Action:** When passing a bag of callbacks to a composable, memoize the data class creation using `remember` with appropriate keys (e.g., `remember(viewModel) { Actions(...) }`).

## 2025-02-24 - Missing Partial Indexes on Frequently Filtered Booleans
**Learning:** The application frequently queries `chapters` table filtering by boolean flags like `bookmark=1` or `unavailable=1` inside complex library refresh queries. Without specific partial indexes (e.g., `WHERE bookmark=1`), these queries rely on wider indexes (like `manga_id`) or full table scans, causing significant IO overhead during library updates, especially for users with large libraries but few bookmarks.
**Action:** Always verify execution plans for queries involving boolean flags on large tables. Use partial indexes (e.g., `CREATE INDEX ... WHERE flag=1`) to drastically reduce index size and lookup time for sparse attributes.
