with open('app/src/main/java/eu/kanade/tachiyomi/ui/setting/TrackingSettingsViewModel.kt', 'r') as f:
    vm_content = f.read()

# Update mangaBakaAuthUrl to take the codeChallenge
vm_content = vm_content.replace(
'''val mangaBakaAuthUrl: Uri = MangaBakaApi.authUrl(),''',
'''val mangaBakaAuthUrl: Uri = Uri.EMPTY,''')

# Inside init:
init_replacement = '''
        launchTrackerUpdates(
            tracker = trackManager.mangaBaka,
            updateUsername = { username ->
                _state.update { it.copy(mangaBakaUsername = username) }
            },
            updateLoggedIn = { loggedIn ->
                _state.update { it.copy(mangaBakaIsLoggedIn = loggedIn) }
            },
        )

        val pkceCodes = MangaBakaApi.getPkceS256ChallengeCode()
        preferences.mangabakaCodeVerifier.set(pkceCodes.codeVerifier)
        _state.update { it.copy(mangaBakaAuthUrl = MangaBakaApi.authUrl(pkceCodes.codeChallenge)) }
'''

vm_content = vm_content.replace(
'''        launchTrackerUpdates(
            tracker = trackManager.mangaBaka,
            updateUsername = { username ->
                _state.update { it.copy(mangaBakaUsername = username) }
            },
            updateLoggedIn = { loggedIn ->
                _state.update { it.copy(mangaBakaIsLoggedIn = loggedIn) }
            },
        )''',
init_replacement)

with open('app/src/main/java/eu/kanade/tachiyomi/ui/setting/TrackingSettingsViewModel.kt', 'w') as f:
    f.write(vm_content)
