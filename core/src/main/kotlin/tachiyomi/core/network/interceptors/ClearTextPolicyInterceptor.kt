package tachiyomi.core.network.interceptors

import java.io.IOException
import java.net.InetAddress
import okhttp3.Interceptor
import okhttp3.Response

class CleartextPolicyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        if (url.scheme == "http") {
            val host = url.host

            val addresses = InetAddress.getAllByName(host)

            val allPrivate = addresses.all { it.isPrivate() }

            if (!allPrivate) {
                throw IOException("Cleartext HTTP not allowed for non-private host: $host")
            }
        }

        return chain.proceed(request)
    }
}

fun InetAddress.isPrivate(): Boolean {
    return isLoopbackAddress || // 127.0.0.1, ::1
        isSiteLocalAddress || // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fec0:/10
        isLinkLocalAddress || // 169.254.0.0/16, fe80::/10
        (address.size == 16 && (address[0].toInt() and 0xfe) == 0xfc) // fc00::/7
}
