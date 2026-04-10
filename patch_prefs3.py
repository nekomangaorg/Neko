with open('app/src/main/java/eu/kanade/tachiyomi/data/preference/PreferencesHelper.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    val mangabakaScoreType = this.preferenceStore.getString("mangabaka_score_type", "STEP_1")''',
'''    val mangabakaScoreType = this.preferenceStore.getString("mangabaka_score_type", "STEP_1")
    val mangabakaCodeVerifier = this.preferenceStore.getString("mangabaka_code_verifier", "")''')

with open('app/src/main/java/eu/kanade/tachiyomi/data/preference/PreferencesHelper.kt', 'w') as f:
    f.write(content)
