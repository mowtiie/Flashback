package com.mowtiie.flashback.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.util.MarkdownLite
import com.mowtiie.flashback.util.MdBlock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.flashback.R

private const val EASTER_EGG_TAPS = 7

@Composable
internal fun AboutSection(onOpenLink: (String) -> Unit) {
    val context = LocalContext.current
    var showLicense by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    val easterEgg = stringResource(R.string.settings_easter_egg)

    SectionHeader(R.string.settings_about_header)
    SettingsCard {
        ActionRow(
            title = stringResource(R.string.settings_privacy_policy),
            body = stringResource(R.string.settings_privacy_policy_body),
            onClick = { showPrivacy = true },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_license),
            body = stringResource(R.string.settings_license_body),
            onClick = { showLicense = true },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_source_code),
            body = stringResource(R.string.settings_source_code_body),
            onClick = { onOpenLink(AppLinks.SOURCE_CODE) },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_support),
            body = stringResource(R.string.settings_support_body),
            onClick = { onOpenLink(AppLinks.SUPPORT) },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_author),
            body = stringResource(R.string.settings_author_body),
            onClick = { onOpenLink(AppLinks.AUTHOR) },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.settings_version),
            body = remember { versionName(context) },
            showArrow = false,
            onClick = {
                versionTaps++
                if (versionTaps >= EASTER_EGG_TAPS) {
                    versionTaps = 0
                    Toast.makeText(context, easterEgg, Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    if (showLicense) LicenseDialog(onDismiss = { showLicense = false })
    if (showPrivacy) PrivacyDialog(onDismiss = { showPrivacy = false })
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember { licenseText(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_license)) },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_license_close)) } },
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val blocks = remember { MarkdownLite.parse(rawText(context, R.raw.privacy_policy)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_privacy_policy)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                blocks.forEach { PolicyBlock(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_license_close)) } },
    )
}

@Composable
private fun PolicyBlock(block: MdBlock) {
    val text = buildAnnotatedString {
        block.spans.forEach { span ->
            if (span.bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) } else append(span.text)
        }
    }
    when (block) {
        is MdBlock.Heading -> Text(
            text = text,
            style = if (block.level == 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp).asHeading(),
        )
        is MdBlock.Paragraph -> Text(text, style = MaterialTheme.typography.bodySmall)
        is MdBlock.Bullet -> Row {
            Text("•", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

private fun licenseText(context: Context): String = rawText(context, R.raw.license)

private fun rawText(context: Context, @RawRes id: Int): String =
    runCatching { context.resources.openRawResource(id).bufferedReader().use { it.readText() } }.getOrDefault("")
