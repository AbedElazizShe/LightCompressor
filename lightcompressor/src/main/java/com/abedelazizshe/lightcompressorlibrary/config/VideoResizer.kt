package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.utils.CompressorUtils

fun interface VideoResizer {
    companion object {
        /**
         * Shrinks the video's resolution based on its original width and height.
         * - 50% If the width or height is greater than or equal to 1920 pixels.
         * - 75% If the width or height is greater than or equal to 1280 pixels.
         * - 95% If the width or height is greater than or equal to 960 pixels.
         * - 90% If the width and height are both less than 960 pixels.
         */
        @JvmStatic
        val auto: VideoResizer = ScaleResize(null);

        /**
         * Resize the video dimensions by the given scale factor
         */
        @JvmStatic
        fun scale(value: Double): VideoResizer = ScaleResize(value)

        /**
         * Scale the video down if the width or height are greater than [limit], retaining the video's aspect ratio.
         * @param limit The maximum width and height of the video
         */
        @JvmStatic
        fun limitSize(limit: Int): VideoResizer = LimitDimension(limit, limit)

        /**
         * Scale the video down if the width or height are greater than [maxWidth] or [maxHeight], retaining the video's aspect ratio.
         * @param maxWidth The maximum width of the video
         * @param maxHeight The maximum height of the video
         */
        @JvmStatic
        fun limitSize(maxWidth: Int, maxHeight: Int): VideoResizer = LimitDimension(maxWidth, maxHeight)

        /**
         * Scales the video so that the width and height matches [size], retaining the video's aspect ratio.
         * @param size The target width/height of the video
         */
        @JvmStatic
        fun matchSize(size: Int, stretch: Boolean = false): VideoResizer = MatchDimension(size, size, stretch)

        /**
         * Scales the video so that the width matches [width] or the height matches [height], retaining the video's aspect ratio.
         * @param width The target width of the video
         * @param height The target height of the video
         */
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