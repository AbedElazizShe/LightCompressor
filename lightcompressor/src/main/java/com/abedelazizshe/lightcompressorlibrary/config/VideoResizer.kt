package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.utils.CompressorUtils

fun interface VideoResizer {
    companion object {
        @JvmStatic
        val auto: VideoResizer = ScaleResize(null);

        @JvmStatic
        fun scale(value: Double): VideoResizer = ScaleResize(value)

        @JvmStatic
        fun limitSize(limit: Int): VideoResizer = TargetDimension(limit, limit, false)

        @JvmStatic
        fun limitSize(maxWidth: Int, maxHeight: Int): VideoResizer = TargetDimension(maxWidth, maxHeight, false)

        @JvmStatic
        fun matchSize(size: Int): VideoResizer = TargetDimension(size, size, true)

        @JvmStatic
        fun matchSize(width: Int, height: Int): VideoResizer = TargetDimension(width, height, true)
    }

    fun resize(width: Double, height: Double): Pair<Double, Double>

    private class TargetDimension(private val width: Int, private val height: Int, private val scaleUp: Boolean = false) : VideoResizer {
        override fun resize(width: Double, height: Double): Pair<Double, Double> {
            var newWidth = this.width.toDouble()
            var newHeight = this.height.toDouble()

            if (!scaleUp && width < newWidth && height < newHeight)
                return Pair(width, height);

            val desiredAspect = width / height
            val videoAspect = newWidth / newHeight
            return if (videoAspect <= desiredAspect) Pair(newWidth, newWidth / desiredAspect) else Pair(newHeight * desiredAspect, newHeight)
        }
    }

    private class ScaleResize(private val percentage: Double? = null) : VideoResizer {
        override fun resize(width: Double, height: Double): Pair<Double, Double> {
            val p = percentage ?: CompressorUtils.autoResizePercentage(width, height)
            return Pair(width * p, height * p)
        }
    }
}