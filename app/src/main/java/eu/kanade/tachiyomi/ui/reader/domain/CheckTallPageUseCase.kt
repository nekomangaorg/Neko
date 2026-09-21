package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import okio.buffer
import okio.source

/**
 * Pure domain interactor that inspects image dimensions and calculates optimal [ReaderPageSplit]
 * slices if an image exceeds OpenGL texture limits or viewport height thresholds.
 */
class CheckTallPageUseCase {

    operator fun invoke(
        page: ReaderPage,
        screenHeight: Int,
        maxTextureSize: Int = GLUtil.maxTextureSize,
    ): List<ReaderPageSplit>? {
        val precomputed = page.precomputedSplits
        if (precomputed != null) {
            return precomputed.ifEmpty { null }
        }
        val streamFn = page.stream ?: return null
        val options =
            try {
                streamFn().source().buffer().use { ImageUtil.extractImageOptions(it) }
            } catch (_: Exception) {
                return null
            }
        if (options.outWidth > 0 && options.outHeight > 0 && page.aspectRatio == 0f) {
            page.aspectRatio = options.outWidth.toFloat() / options.outHeight.toFloat()
        }
        val splits =
            computeSplits(
                page = page,
                outWidth = options.outWidth,
                outHeight = options.outHeight,
                screenHeight = screenHeight,
                maxTextureSize = maxTextureSize,
            )
        page.precomputedSplits = splits ?: emptyList()
        return splits
    }

    /** Pure calculation of optimal slice splits given dimensions and maximum texture sizes. */
    fun computeSplits(
        page: ReaderPage,
        outWidth: Int,
        outHeight: Int,
        screenHeight: Int,
        maxTextureSize: Int = GLUtil.maxTextureSize,
    ): List<ReaderPageSplit>? {
        if (outHeight <= 0 || outWidth <= 0) return null
        val displayMaxHeight =
            if (screenHeight > 0) {
                minOf(maxOf(screenHeight * 2, 4096), maxTextureSize)
            } else {
                maxTextureSize
            }
        val isTall = (outHeight.toFloat() / outWidth.toFloat() > 3f) || (outHeight > maxTextureSize)
        if (!isTall || outHeight <= displayMaxHeight) {
            return null
        }

        val maxSliceHeight = minOf(displayMaxHeight, maxTextureSize)
        val partCount = (outHeight - 1) / maxSliceHeight + 1
        if (partCount <= 1) return null

        val optimalSplitHeight = outHeight / partCount
        val splits = mutableListOf<ReaderPageSplit>()
        for (i in 0 until partCount) {
            val topOffset = i * optimalSplitHeight
            val splitH =
                if (i == partCount - 1) {
                    outHeight - topOffset
                } else {
                    optimalSplitHeight
                }
            val split = ReaderPageSplit(page = page, topOffset = topOffset, splitHeight = splitH)
            split.aspectRatio = outWidth.toFloat() / splitH.toFloat()
            splits.add(split)
        }
        return splits
    }
}
