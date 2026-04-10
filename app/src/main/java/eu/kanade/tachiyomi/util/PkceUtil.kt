package eu.kanade.tachiyomi.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PkceUtil {

    private const val PKCE_BASE64_ENCODE_SETTINGS =
        Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE

    fun generateCodeVerifier(): String {
        val codeVerifier = ByteArray(50)
        SecureRandom().nextBytes(codeVerifier)
        return Base64.encodeToString(codeVerifier, PKCE_BASE64_ENCODE_SETTINGS)
    }

    fun generateS256Codes(): PkceCodes {
        val codeVerifier = generateCodeVerifier()
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        val codeChallenge = android.util.Base64.encodeToString(digest, PKCE_BASE64_ENCODE_SETTINGS)

        return PkceCodes(codeVerifier = codeVerifier, codeChallenge = codeChallenge)
    }
}

data class PkceCodes(val codeVerifier: String, val codeChallenge: String)
