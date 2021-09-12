## 🥳 New
- New similar screen (only list view for now)
- Ability to split double pages ( @Jays2Kings)
- Clicking a genre tag searches your library, long clicking searches MangaDex ( @Jays2Kings for most of the heavy lifting )
- Completed and Cancelled status (@animusfracto)
- Add support for Comikey and BilliBilli chapters (@jobobby04)
- New tracking sheet ui (@Jays2Kings)
- Latest now follows the users default content ratings
- Add Grey Wolf Theme
- Add Teal Theme (@Jays2Kings)
- If there's a gap in the chapters, a warning will show missing number of chapters on transition pages while reading (@curche)
## 🏗️ Changes
- Update seekbar UI in reader to a slider: now shows the current page while dragging, and shows ticks through the track to represent the pages (@Jays2Kings)
- Side navigation now always shows on larger tablets For shorter and smaller phones, side navigation may no longer show (@Jays2Kings)
- Added more Browse Sort options
- Show unreleased Chapters
- Use color accented ripples (@Soitora)
## 🐜 Fixes
- Back button not working in latest chapter view
- ContentRating chapters were blocked by MangaDex Api change (@Zurkei)
- Restoring from Tachiyomi backup caused duplicates when searching for manga that was in library
- General J2K bug fixes ( @Jays2Kings)
- Browse Fab not aligning itself correctly (@Jays2Kings)
- App crashing when expanding an animated cover (@Jays2Kings)
- Excluding a category was causing app to crash (@Zurkei)
- Some manga that don't have titles because they weren't using en title as main title
## 🛠️ Other
- added jetpack compose
- some more logging for stuck chapter
- update dependencies
- Changed data model to for api changes
- Reduce API calls for covers on follows since includes parameter was added
- Removed authenticated request for Md@Home node url
- Tweaked token authenticator
