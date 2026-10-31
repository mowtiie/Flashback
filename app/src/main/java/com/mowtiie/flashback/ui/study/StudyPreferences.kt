package com.mowtiie.flashback.ui.study

import android.content.Context
import androidx.core.content.edit

/** Shares the Java app's preferences file, so reminder settings can live beside it. */
class StudyPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var answerMode: AnswerMode
        get() = AnswerMode.fromValue(prefs.getInt(KEY_ANSWER_MODE, AnswerMode.Flip.value))
        set(mode) = prefs.edit { putInt(KEY_ANSWER_MODE, mode.value) }

    private companion object {
        const val FILE = "flashback_prefs"
        const val KEY_ANSWER_MODE = "study_answer_mode"
    }
}
