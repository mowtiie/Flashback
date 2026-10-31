package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.PreferenceStore

class StudyPreferences(private val store: PreferenceStore) {

    var answerMode: AnswerMode
        get() = AnswerMode.fromValue(store.getInt(KEY_ANSWER_MODE, AnswerMode.Flip.value))
        set(mode) = store.putInt(KEY_ANSWER_MODE, mode.value)

    private companion object {
        const val KEY_ANSWER_MODE = "study_answer_mode"
    }
}
