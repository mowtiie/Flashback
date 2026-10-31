package com.mowtiie.flashback.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/** Marks a section title so screen readers can jump between sections. */
fun Modifier.asHeading(): Modifier = semantics { heading() }
