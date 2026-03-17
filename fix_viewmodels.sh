sed -i 's/viewModelScope.launch {/viewModelScope.launchIO {/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseViewModel.kt
sed -i 's/viewModelScope.launchIO { paginator.loadNextItems() }/viewModelScope.launch { paginator.loadNextItems() }/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseViewModel.kt
sed -i 's/viewModelScope.launch {/viewModelScope.launchIO {/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayViewModel.kt
sed -i 's/viewModelScope.launchIO { paginator.loadNextItems() }/viewModelScope.launch { paginator.loadNextItems() }/g' app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayViewModel.kt
