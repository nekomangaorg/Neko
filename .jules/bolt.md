## 2024-05-15 - [Filter-Map Chain Optimization]
**Learning:** Chaining `.filterNot { ... }` with `.mapNotNull { ... }` generates an intermediate List allocation in memory before mapping. Using `.asSequence()` works but adds overhead.
**Action:** Replace `list.filterNot { cond }.mapNotNull { transform(it) }` with a single pass `list.mapNotNull { if(cond) null else transform(it) }` for small-to-medium lists to save allocations and GC churn without the sequence overhead.
