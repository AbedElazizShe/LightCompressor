package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.utils.CompressorUtils

fun interface VideoResizer {
    companion object {
        @JvmStatic
        val auto: VideoResizer = ScaleResize(null);

        @JvmStatic
        fun scale(value: Double): VideoResizer = ScaleResize(value)

        @JvmStatic
        fun limitSize(limit: Int): VideoResizer = LimitDimension(limit, limit)

        @JvmStatic
        fun limitSize(maxWidth: Int, maxHeight: Int): VideoResizer = LimitDimension(maxWidth, maxHeight)

        @JvmStatic
        fun matchSize(size: Int, stretch: Boolean = false): VideoResizer = MatchDimension(size, size, stretch)

        @JvmStatic
        fun matchSize(width: Int, height: Int, stretch: Boolean = false): VideoResizer = MatchDimension(width, height, stretch)

        private fun keepAspect(width: Double, height: Double, newWidth: Double, newHeight: Double): Pair<Double, Double> {
            val desiredAspect = width / height
            val videoAspect = newWidth / newHeight
            return if (videoAspect <= desiredAspect) Pair(newWidth, newWidth / desiredAspect) else Pair(newHeight * desiredAspect, newHeight)
        }
    }

    fun resize(width: Double, height: Double): Pair<Double, Double>

    private class LimitDimension(private val width: Int, private val height: Int) : VideoResizer {
        override fun resize(width: Double, height: Double): Pair<Double, Double> {
            val newWidth = this.width.toDouble()
            val newHeight = this.height.toDouble()

            return if (width < newWidth && height < newHeight) Pair(width, height) else keepAspect(width, height, newWidth, newHeight)
        }
    }

    private class MatchDimension(private val width: Int, private val height: Int, private val stretch: Boolean) : VideoResizer {
        override fun resize(width: Double, height: Double): Pair<Double, Double> {
            var newWidth = this.width.toDouble()
            var newHeight = this.height.toDouble()

            return if (stretch) Pair(newWidth, newHeight) else keepAspect(width, height, newWidth, newHeight)
        }
    }

    private class ScaleResize(private val percentage: Double? = null) : VideoResizer {
        override fun resize(width: Double, height: Double): Pair<Double, Double> {
            val p = percentage ?: CompressorUtils.autoResizePercentage(width, height)
            return Pair(width * p, height * p)
        }
    }
}