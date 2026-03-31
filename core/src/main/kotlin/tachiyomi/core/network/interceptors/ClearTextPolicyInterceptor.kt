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
    return isAnyLocalAddress || // 0.0.0.0
        isLoopbackAddress || // 127.0.0.1
        isSiteLocalAddress || // 192.168.x.x, 10.x.x.x, 172.16-31.x.x
        isLinkLocalAddress // 169.254.x.x
}
