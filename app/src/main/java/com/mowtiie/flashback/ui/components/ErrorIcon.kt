package com.mowtiie.flashback.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.flashback.R

/** A field in error also shows an icon, so the state is not carried by colour alone. */
fun errorTrailingIcon(isError: Boolean): (@Composable () -> Unit)? =
    if (isError) {
        { Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.field_error)) }
    } else {
        null
    }
