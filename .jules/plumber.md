## 2024-05-30 - Unclosed Streams in IO utilities
**Learning:** Utilities that copy databases (like `MangaMappings.kt`) and read streams for backups (`BackupUtil.kt`) frequently rely on chained `use` blocks or manual `.close()` calls that may fail if exceptions are thrown during read/write.
**Action:** Always wrap standard `InputStream` and `OutputStream` in Kotlin `.use { }` blocks instead of manually calling `.close()`. For Okio sources like `openInputStream(uri).source().buffer()`, wrap the returned `BufferedSource` in a `.use { }` block to ensure all underlying resources are closed.
