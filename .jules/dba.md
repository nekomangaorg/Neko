
## 2026-04-22 - Tuple Aggregation
**Learning:** Returning targeted Tuples (`MangaHistoryStats`) directly from Room queries using SQL aggregation (`SUM`, `MIN`) instead of loading full Entities is much faster and reduces memory overhead.
**Action:** Use SQL aggregation and chunked queries (`chunked(500)`) over collection-based Kotlin mapping in ViewModels to avoid hitting SQLite parameter limits and prevent the N+1 problem.
