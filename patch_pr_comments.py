import re

with open('app/src/main/java/eu/kanade/tachiyomi/data/track/mangabaka/MangaBakaApi.kt', 'r') as f:
    api_content = f.read()

# 1. fix Instant.parse to LocalDate.parse
api_content = api_content.replace('import kotlinx.datetime.Instant\nimport eu.kanade.tachiyomi.util.lang.toLocalDate', 'import kotlinx.datetime.Instant\nimport kotlinx.datetime.LocalDate\nimport kotlinx.datetime.TimeZone\nimport kotlinx.datetime.atStartOfDayIn\nimport eu.kanade.tachiyomi.util.lang.toLocalDate')

api_content = api_content.replace('Instant.parse(it).toEpochMilliseconds()', 'LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()')

# 2. fix static codeVerifier
api_content = api_content.replace(
'''    suspend fun getAccessToken(code: String): MangaBakaOAuth {
        return withIOContext {
            val formBody =
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("code", code)
                    .add("code_verifier", codeVerifier)''',
'''    suspend fun getAccessToken(code: String, codeVerifier: String): MangaBakaOAuth {
        return withIOContext {
            val formBody =
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("code", code)
                    .add("code_verifier", codeVerifier)''')

api_content = api_content.replace('private var codeVerifier: String = ""\n', '')

api_content = api_content.replace(
'''        fun authUrl(): Uri =
            "$OAUTH_URL/authorize"
                .toUri()
                .buildUpon() //
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("code_challenge", getPkceS256ChallengeCode())''',
'''        fun authUrl(codeChallenge: String): Uri =
            "$OAUTH_URL/authorize"
                .toUri()
                .buildUpon() //
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("code_challenge", codeChallenge)''')

api_content = api_content.replace(
'''        private fun getPkceS256ChallengeCode(): String {
            // MangaBaka requires an actually conformant PKCE process, unlike MAL
            // 1. create verifier
            // 2. create challenge from verifier (S256 hash -> base64 URL encode)
            // 3. send challenge to /authorize
            // 4. send verifier for access tokens to /token
            val codes = PkceUtil.generateS256Codes()
            codeVerifier = codes.codeVerifier
            return codes.codeChallenge
        }''',
'''        fun getPkceS256ChallengeCode(): PkceUtil.PkceCodes {
            // MangaBaka requires an actually conformant PKCE process, unlike MAL
            // 1. create verifier
            // 2. create challenge from verifier (S256 hash -> base64 URL encode)
            // 3. send challenge to /authorize
            // 4. send verifier for access tokens to /token
            return PkceUtil.generateS256Codes()
        }''')

# We need to change the return type to PkceCodes and fix usage in TrackingSettingsViewModel and MangaBakaLoginActivity
with open('app/src/main/java/eu/kanade/tachiyomi/data/track/mangabaka/MangaBakaApi.kt', 'w') as f:
    f.write(api_content)
