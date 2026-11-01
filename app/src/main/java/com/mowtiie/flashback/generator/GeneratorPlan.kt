package com.mowtiie.flashback.generator

/** How many requests a run will make, so the user can see it before anything is sent. */
object GeneratorPlan {
    /** An estimate from the length alone; the real split follows paragraph boundaries and can differ by a few. */
    fun requests(
        coverage: Coverage,
        chars: Int,
        isPdf: Boolean,
        provider: GeneratorProvider = GeneratorProvider.Anthropic,
    ): Int {
        if (isPdf) return 1
        if (coverage == Coverage.KeyPoints && splitsKeyPoints(provider, chars).not()) return 1
        val size = provider.sectionChars
        return ((chars + size - 1) / size).coerceAtLeast(1)
    }

    /**
     * A small on-device model reads a long text from the start and runs out of cards before the end,
     * so Key points is split into sections that each get a share of the card count.
     */
    fun splitsKeyPoints(provider: GeneratorProvider, chars: Int): Boolean =
        !provider.usesNetwork && chars > provider.sectionChars

    fun needsConfirmation(requests: Int): Boolean = requests > GeneratorLimits.CONFIRM_ABOVE_SECTIONS

    fun maxChars(coverage: Coverage, provider: GeneratorProvider = GeneratorProvider.Anthropic): Int = when (coverage) {
        Coverage.Everything -> GeneratorLimits.MAX_SOURCE_CHARS
        Coverage.KeyPoints -> provider.keyPointsChars
    }
}
