package com.picall.app.imageprocessing

import android.graphics.Bitmap

class FilterPipeline {

    private val stages = mutableListOf<Pair<ImageFilter, Float>>()

    fun addStage(filter: ImageFilter, intensity: Float = 1f) {
        stages.add(filter to intensity.coerceIn(0f, 1f))
    }

    fun clear() = stages.clear()

    fun process(input: Bitmap, globalIntensity: Float = 1f): Bitmap {
        var current = input.copy(Bitmap.Config.ARGB_8888, true)
        val g = globalIntensity.coerceIn(0f, 1f)

        for ((filter, intensity) in stages) {
            val effective = intensity * g
            if (effective > 0.001f) {
                val next = filter.apply(current, effective)
                if (next !== current) {
                    current.recycle()
                    current = next
                }
            }
        }
        return current
    }
}

interface ImageFilter {
    fun apply(input: Bitmap, intensity: Float): Bitmap
}
