## 2026-06-27 - Adaptive Layouts for Branding/Detail Screens
**Learning:** For branding and configuration screens (like AboutScreen), standard full-width list items stretch uncomfortably on Expanded screens. Splitting the layout into a left pane for visual identity (brand logo and social links) and a right pane for vertical configuration lists creates a much cleaner, responsive, and premium UX.
**Action:** Always check `windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded` to structure brand, settings, or detail screens into multi-pane side-by-side layouts rather than wrapping everything in a single full-width list.

## 2026-07-01 - Responsive Feed Screens for Tablets and Foldables
**Learning:** Full-width lists and summaries (like the FeedScreen sections) stretch awkwardly on Expanded screens (tablets/foldables) if not constrained. Restricting the main list/grid container to `Modifier.widthIn(max = 800.dp)` and centering it horizontally inside a parent container with `Alignment.TopCenter` significantly improves scan-ability and visual premium feel.
**Action:** For lists, feeds, and forms that display in a single-column layout on compact devices, always check `windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded` and constrain their width using `Modifier.widthIn(max = 800.dp)` centered horizontally.

## 2026-07-23 - Propagating WindowSizeClass to Recommendations & Detail Screens
**Learning:** Top-level screens with content lists/grids (like SimilarScreen and DisplayScreen) that don't receive WindowSizeClass by default will stretch uncomfortably on Expanded devices. Consistently passing WindowSizeClass down from MainActivity through MainScreen, and wrapping these lists/grids in a top-centered Box constrained to a maximum width of 800.dp, ensures a polished, premium, and unified adaptive styling across all views.
**Action:** When creating new screen-level composables containing feeds, lists, or grids, always accept WindowSizeClass and apply a centered maximum width constraint of 800.dp on WindowWidthSizeClass.Expanded.
