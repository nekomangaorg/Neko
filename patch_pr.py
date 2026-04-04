with open("app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerPageHolder.kt", "r") as f:
    content = f.read()

import re

old_block = """                    val isAnimated =
                        try {
                            val stream = streamFn().source().buffer()

                            val stream2 = streamFn2?.invoke()?.source()?.buffer()
                            openStream =
                                when (
                                    viewer.config.doublePageRotate &&
                                        stream2 == null &&
                                        ImageUtil.isWideImage(stream)
                                ) {
                                    true -> {
                                        val rotation =
                                            if (viewer.config.doublePageRotateReverse) -90f else 90f
                                        ImageUtil.rotateImage(stream, rotation)
                                    }
                                    false -> this@PagerPageHolder.mergeOrSplitPages(stream, stream2)
                                }

                            ImageUtil.isAnimatedAndSupported(stream) ||
                                if (stream2 != null) ImageUtil.isAnimatedAndSupported(stream2)
                                else false
                        } catch (e: Exception) {
                            TimberKt.e(e) { "Error checking image header" }
                            return@launch
                        }

                    withContext(Main) {
                        if (!isAnimated) {
                            if (viewer.config.readerTheme >= 2) {
                                if (
                                    page.bg != null &&
                                        page.bgType ==
                                            getBGType(viewer.config.readerTheme, context) +
                                                item.hashCode()
                                ) {
                                    setImage(openStream!!, false, imageConfig)
                                    pageView?.background = page.bg
                                }
                                // if the user switches to automatic when pages are already cached,
                                // the
                                // bg needs to be loaded
                                else {
                                    val bytesArray = openStream!!.readByteArray()
                                    val bytesSource = Buffer().write(bytesArray)
                                    setImage(bytesSource, false, imageConfig)
                                    bytesSource.close()

                                    scope.launchUI {
                                        try {
                                            pageView?.background = setBG(bytesArray)
                                        } catch (e: Exception) {
                                            TimberKt.e(e) { "Error setting BG" }
                                            pageView?.background = ColorDrawable(Color.WHITE)
                                        } finally {
                                            page.bg = pageView?.background
                                            page.bgType =
                                                getBGType(viewer.config.readerTheme, context) +
                                                    item.hashCode()
                                        }
                                    }
                                }
                            } else {
                                setImage(openStream!!, false, imageConfig)
                            }
                        } else {
                            setImage(openStream!!, true, imageConfig)
                            if (viewer.config.readerTheme >= 2 && page.bg != null) {
                                pageView?.background = page.bg
                            }
                        }
                    }"""

new_block = """                    val (isAnimated, bytesArray) =
                        try {
                            val stream = streamFn().source().buffer()

                            val stream2 = streamFn2?.invoke()?.source()?.buffer()
                            openStream =
                                when (
                                    viewer.config.doublePageRotate &&
                                        stream2 == null &&
                                        ImageUtil.isWideImage(stream)
                                ) {
                                    true -> {
                                        val rotation =
                                            if (viewer.config.doublePageRotateReverse) -90f else 90f
                                        ImageUtil.rotateImage(stream, rotation)
                                    }
                                    false -> this@PagerPageHolder.mergeOrSplitPages(stream, stream2)
                                }

                            val animated = ImageUtil.isAnimatedAndSupported(stream) ||
                                if (stream2 != null) ImageUtil.isAnimatedAndSupported(stream2)
                                else false

                            val bytes = if (!animated && viewer.config.readerTheme >= 2 &&
                                !(page.bg != null && page.bgType == getBGType(viewer.config.readerTheme, context) + item.hashCode())
                            ) {
                                openStream?.readByteArray()
                            } else null

                            animated to bytes
                        } catch (e: Exception) {
                            TimberKt.e(e) { "Error checking image header" }
                            return@launch
                        }

                    withContext(Main) {
                        if (!isAnimated) {
                            if (viewer.config.readerTheme >= 2) {
                                if (bytesArray == null && page.bg != null) {
                                    openStream?.let { setImage(it, false, imageConfig) }
                                    pageView?.background = page.bg
                                }
                                // if the user switches to automatic when pages are already cached,
                                // the
                                // bg needs to be loaded
                                else if (bytesArray != null) {
                                    val bytesSource = Buffer().write(bytesArray)
                                    setImage(bytesSource, false, imageConfig)
                                    bytesSource.close()

                                    launch(Main) {
                                        try {
                                            pageView?.background = setBG(bytesArray)
                                        } catch (e: Exception) {
                                            TimberKt.e(e) { "Error setting BG" }
                                            pageView?.background = ColorDrawable(Color.WHITE)
                                        } finally {
                                            page.bg = pageView?.background
                                            page.bgType =
                                                getBGType(viewer.config.readerTheme, context) +
                                                    item.hashCode()
                                        }
                                    }
                                }
                            } else {
                                openStream?.let { setImage(it, false, imageConfig) }
                            }
                        } else {
                            openStream?.let { setImage(it, true, imageConfig) }
                            if (viewer.config.readerTheme >= 2 && page.bg != null) {
                                pageView?.background = page.bg
                            }
                        }
                    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
else:
    print("Old block not found!")

with open("app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerPageHolder.kt", "w") as f:
    f.write(content)
