package com.mowtiie.flashback.ui.prompt

import androidx.lifecycle.ViewModel
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.ChatPrompt
import com.mowtiie.flashback.generator.ChatPromptOptions
import com.mowtiie.flashback.generator.PromptCoverage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PromptBuilderUiState(
    val options: ChatPromptOptions = ChatPromptOptions(),
    val prompt: String = ChatPrompt.build(options),
)

class PromptBuilderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PromptBuilderUiState())
    val uiState: StateFlow<PromptBuilderUiState> = _uiState.asStateFlow()

    fun setCoverage(coverage: PromptCoverage) = change { it.copy(coverage = coverage) }

    fun setCardTypes(types: CardTypes) = change { it.copy(cardTypes = types) }

    fun setLanguage(language: String) = change { it.copy(language = language) }

    fun setDeckName(name: String) = change { it.copy(deckName = name) }

    private fun change(edit: (ChatPromptOptions) -> ChatPromptOptions) {
        _uiState.update {
            val options = edit(it.options)
            PromptBuilderUiState(options, ChatPrompt.build(options))
        }
    }
}
