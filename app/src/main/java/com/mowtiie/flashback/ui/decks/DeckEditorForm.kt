package com.mowtiie.flashback.ui.decks

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.entity.Deck

const val DEFAULT_NEW_PER_DAY = 20
const val DEFAULT_REVIEWS_PER_DAY = 200
const val MAX_PER_DAY = 9999

@Immutable
data class DeckEditorForm(
    val name: String = "",
    val description: String = "",
    val reverseByDefault: Boolean = false,
    val newPerDay: String = DEFAULT_NEW_PER_DAY.toString(),
    val reviewsPerDay: String = DEFAULT_REVIEWS_PER_DAY.toString(),
    val tagIds: Set<Long> = emptySet(),
) {
    /** Surrounding whitespace is not an edit. */
    fun normalized() = copy(
        name = name.trim(),
        description = description.trim(),
        newPerDay = newPerDay.trim(),
        reviewsPerDay = reviewsPerDay.trim(),
    )

    fun validate(): DeckEditorValidation {
        val form = normalized()
        val newLimit = parseLimit(form.newPerDay)
        val reviewLimit = parseLimit(form.reviewsPerDay)
        val errors = DeckEditorErrors(
            nameRequired = form.name.isEmpty(),
            newPerDay = (newLimit as? LimitResult.Invalid)?.error,
            reviewsPerDay = (reviewLimit as? LimitResult.Invalid)?.error,
        )
        return if (errors.hasAny) {
            DeckEditorValidation.Invalid(errors)
        } else {
            DeckEditorValidation.Valid(
                (newLimit as LimitResult.Ok).value,
                (reviewLimit as LimitResult.Ok).value,
            )
        }
    }

    /** [existing] keeps its id and creation time when editing. */
    fun toDeck(valid: DeckEditorValidation.Valid, existing: Deck?): Deck {
        val form = normalized()
        return (existing ?: Deck()).copy(
            name = form.name,
            description = form.description.ifEmpty { null },
            reverseByDefault = form.reverseByDefault,
            newPerDay = valid.newPerDay,
            reviewsPerDay = valid.reviewsPerDay,
        )
    }

    companion object {
        fun from(deck: Deck, tagIds: Set<Long>) = DeckEditorForm(
            name = deck.name,
            description = deck.description.orEmpty(),
            reverseByDefault = deck.reverseByDefault,
            newPerDay = deck.newPerDay.toString(),
            reviewsPerDay = deck.reviewsPerDay.toString(),
            tagIds = tagIds,
        )
    }
}

enum class LimitError { Required, TooLarge }

@Immutable
data class DeckEditorErrors(
    val nameRequired: Boolean = false,
    val newPerDay: LimitError? = null,
    val reviewsPerDay: LimitError? = null,
) {
    val hasAny: Boolean get() = nameRequired || newPerDay != null || reviewsPerDay != null
}

sealed interface DeckEditorValidation {
    data class Valid(val newPerDay: Int, val reviewsPerDay: Int) : DeckEditorValidation
    data class Invalid(val errors: DeckEditorErrors) : DeckEditorValidation
}

private sealed interface LimitResult {
    data class Ok(val value: Int) : LimitResult
    data class Invalid(val error: LimitError) : LimitResult
}

private fun parseLimit(text: String): LimitResult {
    if (text.isEmpty()) return LimitResult.Invalid(LimitError.Required)
    val value = text.toIntOrNull()
    return if (value == null || value > MAX_PER_DAY) {
        LimitResult.Invalid(LimitError.TooLarge)
    } else {
        LimitResult.Ok(value)
    }
}
