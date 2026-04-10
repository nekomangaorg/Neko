with open('app/src/main/java/eu/kanade/tachiyomi/data/track/mangabaka/MangaBaka.kt', 'r') as f:
    mb_content = f.read()

mb_content = mb_content.replace('suspend fun login(code: String) {', 'suspend fun login(code: String, codeVerifier: String) {')
mb_content = mb_content.replace('val oauth = api.getAccessToken(code)', 'val oauth = api.getAccessToken(code, codeVerifier)')

with open('app/src/main/java/eu/kanade/tachiyomi/data/track/mangabaka/MangaBaka.kt', 'w') as f:
    f.write(mb_content)
