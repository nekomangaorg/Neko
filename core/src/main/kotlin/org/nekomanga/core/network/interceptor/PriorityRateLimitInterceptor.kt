package org.nekomanga.core.network.interceptor

import java.io.IOException
import java.util.PriorityQueue
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * An OkHttp interceptor that handles rate limiting with support for Request Priority.
 *
 * When the rate limit is reached, requests are queued. High priority requests will jump ahead of
 * low priority requests in the queue.
 *
 * @param permits {Int} Number of requests allowed within a period.
 * @param period {Long} The limiting duration.
 * @param unit {TimeUnit} The unit of time for the period.
 * @param prioritySelector {Function} A lambda that returns an Int priority level for a given URL.
 *   Higher numbers = Higher Priority.
 */
fun OkHttpClient.Builder.rateLimitPriority(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
    prioritySelector: (HttpUrl) -> Int,
) = addInterceptor(PriorityRateLimitInterceptor(permits, period, unit, prioritySelector))

class PriorityRateLimitInterceptor(
    private val permits: Int,
    period: Long,
    unit: TimeUnit,
    private val prioritySelector: (HttpUrl) -> Int,
) : Interceptor {

    private val lock = Object()
    private val queue = PriorityQueue<Ticket>()

    private var tokens = permits.toDouble()
    private var lastRefillTime = System.nanoTime()
    private val refillRatePerNano = permits.toDouble() / unit.toNanos(period)
    private var sequenceCounter = 0L

    private class Ticket(val priority: Int, val sequence: Long) : Comparable<Ticket> {
        override fun compareTo(other: Ticket): Int {
            // 1. Higher Priority = "Smaller" index (Head of Min-Heap)
            // If this(10) and other(1), we want this to be -1 (comes before)
            val pComp = other.priority.compareTo(this.priority)
            if (pComp != 0) return pComp

            // 2. FIFO fallback: Lower sequence = "Smaller" index
            return this.sequence.compareTo(other.sequence)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        val priority = prioritySelector(url)

        // We define the ticket outside, but initialize it inside the lock to ensure sequence safety
        val ticket: Ticket

        // SINGLE SYNCHRONIZED BLOCK
        // We do not release the lock between "Queueing" and "Checking/Waiting"
        synchronized(lock) {
            ticket = Ticket(priority, sequenceCounter++)
            queue.add(ticket)

            // Wake up any sleeping threads so they realize a new High Priority ticket has arrived
            lock.notifyAll()

            try {
                while (true) {
                    // 1. Handle Cancellation (Cleanup)
                    if (chain.call().isCanceled()) {
                        queue.remove(ticket)
                        lock.notifyAll() // Head might have changed
                        throw IOException("Canceled")
                    }

                    // 2. Refill Tokens
                    val now = System.nanoTime()
                    val duration = now - lastRefillTime
                    if (duration > 0) {
                        val newTokens = duration * refillRatePerNano
                        tokens = (tokens + newTokens).coerceAtMost(permits.toDouble())
                        lastRefillTime = now
                    }

                    // 3. Check Position
                    val head = queue.peek()

                    if (head === ticket) {
                        // WE ARE HEAD
                        if (tokens >= 1.0) {
                            // Success: Consume and Exit Loop
                            tokens -= 1.0
                            queue.poll() // Remove self
                            lock.notifyAll() // Wake up next candidate
                            break
                        } else {
                            // Head, but empty bucket. Wait for refill.
                            val missingTokens = 1.0 - tokens
                            val waitNanos = (missingTokens / refillRatePerNano).toLong()
                            if (waitNanos > 0) {
                                val millis = waitNanos / 1_000_000
                                val nanos = (waitNanos % 1_000_000).toInt()
                                lock.wait(millis, nanos)
                            } else {
                                lock.wait(1) // Safety net
                            }
                        }
                    } else {
                        // NOT HEAD.
                        // Someone with higher priority (or older sequence) is ahead.
                        // Sleep indefinitely until notified by the person leaving the queue.
                        lock.wait()
                    }
                }
            } catch (e: InterruptedException) {
                queue.remove(ticket)
                lock.notifyAll()
                Thread.currentThread().interrupt()
                throw IOException("Interrupted waiting for rate limit", e)
            }
        }

        // 4. Proceed (MUST be outside synchronized block to allow parallel network flight)
        return chain.proceed(chain.request())
    }
}
