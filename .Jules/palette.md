## 2024-05-24 - [Accessible Icon-Only Buttons in Compose]
**Learning:** In Jetpack Compose, `OutlinedButton` containers do not automatically provide accessible names if their content (text) is hidden or empty. For buttons that switch between icon-only (responsive) and text+icon layouts:
1. If text is visible, the icon should be decorative (`contentDescription = null`) to avoid duplicate announcements.
2. If text is hidden (responsive) or empty, the icon MUST have a `contentDescription`.
3. For state-toggling buttons (like Favorite), the `contentDescription` must dynamically reflect the state (e.g., "Add to Library" vs "In Library").

**Action:** Always implement a fallback logic for `Icon` content description: `val description = explicitDescription ?: (if (isTextHidden) text else null)`. This ensures accessibility in all responsive modes without redundancy.
