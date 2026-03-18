import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt', 'r') as f:
    content = f.read()

# Define the old block starting with 'fun preferenceUpdates() {' and ending with '    }'
# We can find the start index of 'fun preferenceUpdates() {'
start_idx = content.find('fun preferenceUpdates() {')
if start_idx == -1:
    print("Could not find preferenceUpdates")
    exit(1)

# Find the matching closing brace
stack = []
end_idx = -1
in_block = False
for i in range(start_idx, len(content)):
    if content[i] == '{':
        stack.append(i)
        in_block = True
    elif content[i] == '}':
        stack.pop()
        if in_block and len(stack) == 0:
            end_idx = i
            break

if end_idx == -1:
    print("Could not find end of preferenceUpdates")
    exit(1)

new_block = """fun preferenceUpdates() {
        fun <T> Flow<T>.observeAndUpdate(update: (LibraryScreenState, T) -> LibraryScreenState) {
            this.distinctUntilChanged()
                .onEach { value -> _internalLibraryScreenState.update { state -> update(state, value) } }
                .launchIn(viewModelScope)
        }

        preferences.useVividColorHeaders().changes().observeAndUpdate { state, value ->
            state.copy(useVividColorHeaders = value)
        }

        libraryPreferences.showStartReadingButton().changes().observeAndUpdate { state, value ->
            state.copy(showStartReadingButton = value)
        }

        mangadexPreferences.includeUnavailableChapters().changes().observeAndUpdate { state, value ->
            state.copy(showUnavailableFilter = value)
        }

        securityPreferences.incognitoMode().changes().observeAndUpdate { state, value ->
            state.copy(incognitoMode = value)
        }

        libraryPreferences.outlineOnCovers().changes().observeAndUpdate { state, value ->
            state.copy(outlineCovers = value)
        }

        libraryPreferences.showDownloadBadge().changes().observeAndUpdate { state, value ->
            state.copy(showDownloadBadges = value)
        }

        libraryPreferences.showUnreadBadge().changes().observeAndUpdate { state, value ->
            state.copy(showUnreadBadges = value)
        }

        libraryPreferences.libraryHorizontalCategories().changes().observeAndUpdate { state, value ->
            state.copy(horizontalCategories = value)
        }

        libraryPreferences.showLibraryButtonBar().changes().observeAndUpdate { state, value ->
            state.copy(showLibraryButtonBar = value)
        }

        combine(libraryPreferences.gridSize().changes(), libraryPreferences.layout().changes()) { gridSize, layout ->
            gridSize to layout
        }.observeAndUpdate { state, value ->
            state.copy(libraryDisplayMode = value.second, rawColumnCount = value.first)
        }
    }"""

new_content = content[:start_idx] + new_block + content[end_idx+1:]

with open('app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt', 'w') as f:
    f.write(new_content)

print("Updated preferenceUpdates block")
