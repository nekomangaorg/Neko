# Technical Proposal: Scanlation Group Hub, Profiles & Follow Feeds

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** August 2026  
**Target Milestone:** Neko 3.3 Community & Discovery  
**Implementation State:** 🟡 Partially Present Baseline (Scanlator Filtering & Blocking exist; Group Profiles & Follow Feeds are New)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> Neko associates scanlator names with chapters and provides scanlator group blocking and filter mechanisms ([`BlockScanlator.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/usecases/manga/BlockScanlator.kt) and [`ScanlatorGroupRepository.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/data/database/repository/ScanlatorGroupRepository.kt)).
>
> **What This Proposal Adds:**
> Group discovery and follow feeds. Currently, users cannot view a scanlation group's details, browse their translated projects, access their social links (Discord/Patreon/Website), or follow groups for release notifications. This proposal introduces rich group profile sheets, MangaDex group API integration, and a dedicated followed group updates feed.

---

## 1. Executive Summary & Vision

Scanlation groups are the lifeblood of manga translations on MangaDex. While Neko currently provides scanlator group blocking and chapter filtering, there is no way for users to discover more works by a favorite scanlation group, view group profiles, or track group releases in a dedicated feed.

### The Objective
This proposal introduces a **Scanlation Group Hub & Profile Subsystem** in Neko. Users can tap any scanlator name across the app to open a dedicated Group Profile sheet/screen, browse all translated series, view group links (Discord, Patreon, Website), and follow groups to receive custom release feed updates.

### Key Highlights:
1. **Interactive Group Profile Screen**: Displays group bio, leader/members, release statistics, active translated languages, and social links (Discord, Website, Donation links).
2. **Group Translated Series Catalogue**: Browse and search all manga projects translated by the scanlation group directly via MangaDex's `/group/{id}` endpoints.
3. **Follow Scanlation Groups**: 1-tap follow mechanism syncing to MangaDex user follows and local database storage.
4. **Dedicated Scanlator Release Feed**: A filterable tab in [FeedScreen.kt](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/FeedScreen.kt) displaying recent chapter drops exclusively from followed scanlation groups.

---

## 2. Architectural Design

```mermaid
graph TD
    TapGroup[Tap Scanlator Name in ChapterRow / Manga Details] --> Nav[Navigate to GroupScreen / GroupSheet]
    
    subgraph Data Flow
        Nav --> GroupVM[ScanlatorGroupViewModel]
        GroupVM --> GroupRepo[ScanlatorGroupRepository]
        GroupRepo --> MDGroupAPI[MangaDex API: /group/{uuid}]
        GroupRepo --> MDSeriesAPI[MangaDex API: /manga?includedGroups[]={uuid}]
    end
    
    subgraph Presentation Layer [Jetpack Compose]
        GroupVM --> GroupProfileScreen[GroupProfileScreen]
        GroupProfileScreen --> GroupHeader[GroupHeader & Social Chips]
        GroupProfileScreen --> GroupMangaGrid[TranslatedMangaGrid]
        GroupProfileScreen --> FollowButton[Follow / Unfollow Action]
    end
    
    subgraph Feed Integration
        FollowButton --> DBFollows[(Room followed_groups table)]
        DBFollows --> FeedVM[FeedViewModel]
        FeedVM --> FeedScreen[Followed Groups Feed Tab in FeedScreen]
    end
```

---

## 3. Core Domain & Data Layer Changes

### 3.1 Domain Models

```kotlin
package org.nekomanga.domain.scanlator

import androidx.compose.runtime.Immutable
import org.nekomanga.domain.manga.DisplayManga

@Immutable
data class ScanlatorGroupDetails(
    val id: String, // UUID
    val name: String,
    val description: String?,
    val websiteUrl: String?,
    val discordUrl: String?,
    val ircChannel: String?,
    val twitterUrl: String?,
    val contactEmail: String?,
    val focusedLanguages: List<String>,
    val leaderUsername: String?,
    val members: List<String>,
    val isFollowed: Boolean,
    val isBlocked: Boolean,
    val totalMangaCount: Int = 0,
)

@Immutable
data class ScanlatorGroupScreenState(
    val isLoading: Boolean = true,
    val groupDetails: ScanlatorGroupDetails? = null,
    val translatedManga: List<DisplayManga> = emptyList(),
    val error: String? = null,
)
```

### 3.2 Database Entity for Followed Groups

```kotlin
@Entity(tableName = "followed_scanlator_groups")
data class FollowedScanlatorGroupEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    val followedAt: Long = System.currentTimeMillis(),
)
```

---

## 4. UI / UX Design Specifications

### 4.1 Group Profile Screen (`GroupProfileScreen.kt`)
- **Header**: Group name, custom banner/avatar, active status badge, and follower count.
- **Social Action Row**: Quick-action icon chips for **Discord**, **Website**, **Donation/Patreon**, and **Email**.
- **Actions Bar**: **Follow Group** (primary button), **Block Group** (secondary button), and **Share Link**.
- **Catalogue Grid**: Fast-scrolling grid of all manga series translated by this group with language badges and release counts.

### 4.2 Group Chapter Quick Navigation
- In [ChapterRow.kt](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/ChapterRow.kt), tapping the scanlator tag chip opens the Group Profile bottom sheet immediately.

---

## 5. Technical Footprint & Integration

1. **MangaDex Network Layer**:
   - Add `fetchGroupDetails(uuid: String)` and `fetchGroupManga(uuid: String)` in `MangaHandler.kt` / `GroupHandler.kt`.
2. **Repository Layer**:
   - Expand `ScanlatorGroupRepository.kt` to handle followed group state and caching.
3. **Presentation Layer**:
   - Create `org.nekomanga.presentation.screens.scanlator`:
     - `ScanlatorGroupScreen.kt`
     - `ScanlatorGroupViewModel.kt`
     - `ScanlatorSocialLinksRow.kt`
4. **Feed Layer**:
   - Add a "Groups" sub-tab in [FeedScreen.kt](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/FeedScreen.kt) consuming followed groups' recent uploads.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement MangaDex API endpoints for group metadata and series queries.
- [ ] **Step 2**: Add Room table for `followed_scanlator_groups` and repository operations.
- [ ] **Step 3**: Build `ScanlatorGroupScreen` with Material 3 tabs, social links, and manga grid.
- [ ] **Step 4**: Connect chapter row scanlator chips to launch group profile sheets.
