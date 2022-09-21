package eu.kanade.tachiyomi.network.interceptor

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.isomorphism.util.TokenBuckets

/**
 * An OkHttp interceptor that handles rate limiting.
 *
 * Examples:
 *
 * permits = 5,  period = 1, unit = seconds  =>  5 requests per second
 * permits = 10, period = 2, unit = minutes  =>  10 requests per 2 minutes
 *
 * @param permits {Int}   Number of requests allowed within a period of units.
 * @param period {Long}   The limiting duration. Defaults to 1.
 * @param unit {TimeUnit} The unit of time for the period. Defaults to seconds.
 */
fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Int = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) = addInterceptor(RateLimitInterceptor(permits.toLong(), period.toLong(), unit))

private class RateLimitInterceptor(
    permits: Long,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {

    private val bucket = TokenBuckets.builder().withCapacity(permits)
        .withFixedIntervalRefillStrategy(permits, period, unit).build()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.call().isCanceled()) {
            throw IOException()
        }
        bucket.consume()

        if (chain.call().isCanceled()) {
            throw IOException()
        }
        return chain.proceed(chain.request())
    }
}
