# Architecture & Technical Proposals

This directory houses technical proposals, architectural specifications, and migration blueprints for Neko.

Proposals are categorized by subsystem and scope to keep documentation organized and maintainable:

- [📖 Reader Architecture (`reader/`)](#reader-architecture-reader)
- [🧩 Subsystem & UI Decoupling (`decoupling/`)](#subsystem--ui-decoupling-decoupling)
- [✨ Features & Enhancements (`features/`)](#features--enhancements-features)
- [🔄 Migrations (`migrations/`)](#migrations-migrations)

---

## 📖 Reader Architecture (`reader/`)

Proposals focused on reader performance, Jetpack Compose viewers, navigation lifecycle orchestration, memory optimization, and UI controls decoupling.

| Proposal | Description | Target Milestone |
| :--- | :--- | :--- |
| [**Decouple Reader Chapter Navigation, Concurrency Guards & Lifecycle Orchestration**](reader/decouple_reader_navigation_and_lifecycle_orchestration_proposal.md) | Migrates chapter navigation into `viewModelScope`, introduces `ReaderNavCommand` and `ReaderChapterTransitionState`, guards rapid key events with a `Mutex`, and outlines the deprecation of legacy `PagerViewer`. | Neko 3.x Reader Decoupling |
| [**Decouple ComposePagerViewer & ComposeWebtoonViewer**](reader/decouple_reader_compose_viewers_proposal.md) | Removes legacy View references, `DownloadManager`, and `Injekt.get()` from Compose viewers, hoisting configurations into immutable UI models. | Neko 3.x Reader Decoupling |
| [**Decouple ReaderControls and Bottom Action Bar**](reader/decouple_reader_controls_and_bottom_bar_proposal.md) | Refactors parameter-heavy reader control bars into grouped UI state models and event listeners. | Neko 3.x Reader Decoupling |
| [**Decouple ReaderChaptersSheet**](reader/decouple_reader_chapters_sheet_proposal.md) | Decouples the chapter selection bottom sheet from direct preferences, context color resolvers, and inline repository calls. | Neko 3.x Reader Decoupling |
| [**Decouple ReaderSettingsSheet**](reader/decouple_reader_settings_sheet_proposal.md) | Decouples reader settings sheet from service locators, preference mutations, and domain flags. | Neko 3.x Reader Decoupling |
| [**Decouple ReaderTransitionPage**](reader/decouple_reader_transition_page_proposal.md) | Decouples chapter transition pages from `DownloadManager`, legacy entity conversions, and chapter gap math. | Neko 3.x Reader Decoupling |
| [**Decouple GestureNavigationOverlay**](reader/decouple_gesture_navigation_overlay_proposal.md) | Decouples gesture navigation overlays from viewer navigation geometry inversion math. | Neko 3.x Reader Decoupling |
| [**Zero-Allocation Webtoon Active Page Resolver**](reader/webtoon_active_item_resolver_proposal.md) | Implements an allocation-free active page resolver for Webtoon reading mode to eliminate scroll stutter. | Neko Performance |
| [**Native Compose Subsampling Tile Renderer**](reader/native_compose_webtoon_subsampling_renderer_proposal.md) | Introduces high-performance tiled subsampling for long webtoon image strips in Compose. | Neko Performance |
| [**Zen Focus Reading Mode & Touch Shield**](reader/zen_focus_reading_mode_proposal.md) | Adds a distraction-free reading mode with accidental touch prevention. | Neko Feature |

---

## 🧩 Subsystem & UI Decoupling (`decoupling/`)

Proposals focused on breaking down God classes, separating domain business logic from Jetpack Compose components, and removing service locator lookups.

| Proposal | Description | Target Milestone |
| :--- | :--- | :--- |
| [**Deconstructing MangaViewModel God Class**](decoupling/decouple_manga_viewmodel_god_class_proposal.md) | Breaks down the massive `MangaViewModel` God class into dedicated, cohesive domain controllers. | Neko Architecture |
| [**Decouple Presentation Repositories & Eliminate Layer Inversion**](decoupling/decouple_presentation_repositories_proposal.md) | Fixes architectural layer inversion where UI presentation components depended on concrete repository implementations. | Clean Architecture |
| [**Decouple ArtworkSheet State & Domain Entities**](decoupling/decouple_artwork_sheet_state_proposal.md) | Hoists internal selection state, Coil image builders, and database models from `ArtworkSheet`. | UI Decoupling |
| [**Decouple Browse & Display Screens**](decoupling/decouple_browse_and_display_screen_navigation_proposal.md) | Decouples Browse and Display screens from database entities and in-UI navigation resolvers. | UI Decoupling |
| [**Decouple Category Management Dialogs & Sheets**](decoupling/decouple_category_management_dialog_and_sheet_proposal.md) | Decouples category dialogs and sheets from validation and diff computation logic. | UI Decoupling |
| [**Decouple ChapterRow & Chapter Actions**](decoupling/decouple_chapter_row_and_actions_proposal.md) | Decouples chapter list items from domain logic, download status, and database entities. | UI Decoupling |
| [**Decouple DownloadScreen and DownloadChapterRow**](decoupling/decouple_download_screen_and_chapter_row_proposal.md) | Decouples download management screens from domain grouping and legacy download states. | UI Decoupling |
| [**Decouple FeedScreen Jobs & Actions**](decoupling/decouple_feed_screen_jobs_and_actions_proposal.md) | Isolates feed screen from background job orchestration, duplicate filtering rules, and flow propagation. | UI Decoupling |
| [**Decouple LibraryScreen Job Dispatching**](decoupling/decouple_library_screen_job_dispatching_proposal.md) | Decouples library screen from background jobs, database entity mapping, and share logic. | UI Decoupling |
| [**Decouple Manga Details Screen**](decoupling/decouple_manga_details_share_and_header_proposal.md) | Decouples manga details screen from network request building and share intent assembly. | UI Decoupling |
| [**Decouple MergeSheet Domain Logic**](decoupling/decouple_merge_sheet_domain_logic_proposal.md) | Decouples merge sheet from domain source resolvers and business merge rules. | UI Decoupling |
| [**Decouple Onboarding Steps**](decoupling/decouple_onboarding_steps_proposal.md) | Decouples onboarding steps from service locators, direct preferences, and Activity lifecycle. | UI Decoupling |
| [**Decouple Settings Screens Jobs & I/O**](decoupling/decouple_settings_screens_jobs_and_io_proposal.md) | Decouples settings screens from coroutine scopes, background work, and disk I/O. | UI Decoupling |
| [**Decouple Statistics Aggregation**](decoupling/decouple_statistics_aggregation_and_formatting_proposal.md) | Offloads heavy stats computation and dataset transformations from Jetpack Compose rendering. | UI Decoupling |
| [**Decouple TrackingSheet Domain Logic**](decoupling/decouple_tracking_sheet_domain_logic_proposal.md) | Decouples tracking sheet from domain models, functional providers, and dialog state multiplexing. | UI Decoupling |

---

## ✨ Features & Enhancements (`features/`)

Technical proposals outlining specifications for new features, social integrations, and user utilities.

| Proposal | Description | Target Milestone |
| :--- | :--- | :--- |
| [**Automated Smart-Merge Chapter Gap Engine**](features/automated_smart_merge_engine_proposal.md) | Intelligent chapter deduplication and gap filling across merged manga sources. | Neko 3.x Feature |
| [**Local Peer-to-Peer Sync (Neko Beam)**](features/local_peer_sync_neko_beam_proposal.md) | Local Wi-Fi / Hotspot peer-to-peer sync and direct chapter transfer between devices. | Neko 3.x Feature |
| [**Native MangaDex Discussion Threads & Comments**](features/native_mangadex_comments_proposal.md) | Direct integration with MangaDex forums and inline chapter comments. | Neko 3.x Feature |
| [**Neko Lens (On-Device Manga OCR & Dictionary)**](features/neko_lens_on_device_ocr_translation_proposal.md) | ML Kit / Tesseract on-device OCR and Japanese dictionary lookup assistant in reader. | Neko 3.x Feature |
| [**Neko Wrapped (Annual & Monthly Review)**](features/neko_wrapped_year_in_review_proposal.md) | Visual personalized reading statistics, milestones, and annual recap presentation. | Neko 3.x Feature |
| [**Manga Reading Activity Heatmap & Interactive Timeline**](features/reading_activity_heatmap_proposal.md) | GitHub-style reading streak heatmap and interactive reading history timeline. | Neko 3.x Feature |
| [**Chapter Release Radar & Cadence Predictor**](features/release_radar_predictive_schedule_proposal.md) | Predictive release schedule and cadence estimation for ongoing manga series. | Neko 3.x Feature |
| [**Scanlation Group Hub, Profiles & Follow Feeds**](features/scanlation_group_hub_proposal.md) | Dedicated scanlation group profiles, release hubs, and follow notifications. | Neko 3.x Feature |
| [**Smart Collections & Rule-Based Dynamic Categories**](features/smart_collections_dynamic_categories_proposal.md) | Dynamic smart categories defined by rule-based queries (status, genre, unread). | Neko 3.x Feature |

---

## 🔄 Migrations (`migrations/`)

Large-scale external API and infrastructure migrations.

| Proposal | Description | Target Milestone |
| :--- | :--- | :--- |
| [**Migrating Primary Metadata & Search to MangaBaka**](migrations/mangabaka_migration_proposal.md) | Architectural migration of Neko's primary metadata, search, and browse provider to MangaBaka. | Neko 4.0 Platform Migration |
