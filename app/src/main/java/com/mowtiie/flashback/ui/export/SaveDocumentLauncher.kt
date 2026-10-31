package com.mowtiie.flashback.ui.export

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mowtiie.flashback.data.io.ArchiveFiles

const val JSON_MIME_TYPE = "application/json"

/**
 * The system "save as" dialog for an export. Launch it with the suggested file name.
 * [onChosen] gets a blocking writer for the chosen place; run it off the main thread.
 */
@Composable
fun rememberSaveDocumentLauncher(
    onChosen: (writer: (String) -> Unit) -> Unit,
    onCancelled: () -> Unit,
): ManagedActivityResultLauncher<String, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
        if (uri == null) {
            onCancelled()
        } else {
            onChosen { text -> ArchiveFiles.write(context.contentResolver, uri, text) }
        }
    }
}
