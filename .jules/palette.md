## 2024-05-23 - [Accessibility in Dynamic Button Blocks]
**Learning:** In `ButtonBlock.kt`, buttons can have their text labels hidden via `hideText` or be initialized with empty text (like the Favorite button). This creates icon-only buttons that are inaccessible to screen readers.
**Action:** When designing data-driven button components, always include an optional `contentDescription` field in the data model (e.g., `ActionButtonData`) to provide a fallback accessible label when the visual text is hidden or empty.
