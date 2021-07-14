## 🥳 New
- Set default for chapter sorting method + ui updates + sort by upload date (@Jays2Kings)
- Manga-wised rotation mode settings (@Jays2Kings / @GhostBear)
- Filter chapters by not d/l'd, not bookmarked (@Jays2Kings)
- Sharing an image directly now prepends SPOILER_ (@loocool2)
- Sharing an image now adds the Manga Url in the text
- Kitsu now defaults to plan to read for new unread entries (@Jays2Kings)

## 🏗️ Changes
- Refresh tracking metadata in advanced settings is faster and can be cancelled
- Syncing follow notification can now be cancelled
- Option to hide bottom sheets in controller by swiping down the side nav for tablet mode (@Jays2Kings)
- Lighten shadows on bottom sheets (@Jays2Kings)
- Using nav rail only for landscape (phone + tablet) (@Jays2Kings)
- Laxing on the all files permission for a11 (@Jays2Kings)
- Hide MDList tracker card if user is not logged in
- Error out if user is not logged in when trying to sync follows
- Category hopper icon color now matches the text color of the unread badge

## 🐜 Fixes
- Reader issue causing chapters to not load
- Amoled theming issue causing biometric issues (@Jays@Kings)
- Two view error log buttons showing in notification when library update error'd out
- library manga error stopped the entire update
- queuing multiple categories for update was not adding to the same notification
- Marking as read/unread from library view was not working correctly
- Filtering library by not tracked and clicking MDList would hide Unfollowed Manga
- Incognito mode does not update tracker, mark chapters read after reading, update pages left, or update  MangaDex read status anymore
- Status/nav bar were not matching splash screen green

## 🛠️ Other
- Added a release action to automate app release after a tag
- Removed .travis folder thats hasn't been used in years
- Update some libraries
