with open('app/src/main/java/eu/kanade/tachiyomi/ui/setting/logins/MangaBakaLoginActivity.kt', 'r') as f:
    login_content = f.read()

login_content = login_content.replace(
'''import eu.kanade.tachiyomi.util.system.launchIO''',
'''import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy''')

login_content = login_content.replace(
'''class MangaBakaLoginActivity : BaseOAuthLoginActivity() {''',
'''class MangaBakaLoginActivity : BaseOAuthLoginActivity() {

    private val preferences: PreferencesHelper by injectLazy()''')

login_content = login_content.replace(
'''trackManager.mangaBaka.login(code)''',
'''val codeVerifier = preferences.mangabakaCodeVerifier.get()
                trackManager.mangaBaka.login(code, codeVerifier)''')

with open('app/src/main/java/eu/kanade/tachiyomi/ui/setting/logins/MangaBakaLoginActivity.kt', 'w') as f:
    f.write(login_content)
