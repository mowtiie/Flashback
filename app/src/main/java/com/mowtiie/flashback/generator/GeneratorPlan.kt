package com.mowtiie.flashback.generator

/** How many requests a run will make, so the user can see it before anything is sent. */
object GeneratorPlan {
    /** An estimate from the length alone; the real split follows paragraph boundaries and can differ by a few. */
    fun requests(coverage: Coverage, chars: Int, isPdf: Boolean): Int {
        if (isPdf || coverage == Coverage.KeyPoints) return 1
        val size = GeneratorLimits.SECTION_CHARS
        return ((chars + size - 1) / size).coerceAtLeast(1)
    }

    fun needsConfirmation(requests: Int): Boolean = requests > GeneratorLimits.CONFIRM_ABOVE_SECTIONS

    fun maxChars(coverage: Coverage): Int = when (coverage) {
        Coverage.Everything -> GeneratorLimits.MAX_SOURCE_CHARS
        Coverage.KeyPoints -> GeneratorLimits.KEY_POINTS_MAX_CHARS
    }
}
