package com.mowtiie.flashback.ui.tags

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.entity.Tag

@Immutable
data class TagEditorForm(
    val name: String = "",
    val description: String = "",
    val color: Int = TagPalette.default,
) {
    /** Surrounding whitespace is not an edit. */
    fun normalized() = copy(name = name.trim(), description = description.trim())

    val isValid: Boolean get() = name.isNotBlank()

    /** [existing] keeps its id when editing. */
    fun toTag(existing: Tag?): Tag {
        val form = normalized()
        return (existing ?: Tag()).copy(
            name = form.name,
            description = form.description.ifEmpty { null },
            color = form.color,
        )
    }

    companion object {
        fun from(tag: Tag) = TagEditorForm(
            name = tag.name,
            description = tag.description.orEmpty(),
            color = tag.color,
        )
    }
}
