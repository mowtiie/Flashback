package com.mowtiie.flashback.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.ui.components.SectionTitle

internal const val DISABLED_ALPHA = 0.5f

/** Above this font scale, choice rows stack vertically. */
private const val STACK_FONT_SCALE = 1.3f

/** Padding and the check mark a selected segment adds around its label. */
private val SEGMENT_CHROME = 50.dp

/** Test tags: the leading icon of an action row, and the import chooser. */
internal const val ROW_ICON_TAG = "settings-row-icon"
internal const val IMPORT_SHEET_TAG = "import-sheet"

@Composable
internal fun SectionHeader(@StringRes titleRes: Int) {
    SectionTitle(stringResource(titleRes), Modifier.padding(top = 16.dp, start = 4.dp))
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
}

/** For an option that is saved with a form. A switch is only for a setting that applies at once. */
@Composable
internal fun CheckboxRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
internal fun ActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    /** The current setting, shown on the right. */
    value: String? = null,
    valueHighlighted: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    enabled: Boolean = true,
    /** For an action that removes something: error colour, and no chevron since nothing opens. */
    destructive: Boolean = false,
    /** False for a row that only shows information. */
    showArrow: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp).size(24.dp).testTag(ROW_ICON_TAG),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified,
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (valueHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        // The arrow tells a row that opens something from a line of text.
        if (!destructive && showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** A thin line between rows of one card. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** Explanatory text under a card, set apart from the rows so it is not mistaken for one. */
@Composable
internal fun Footnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** Card sits inside a settings card; Form sits among a form's own fields and lines up with them. */
internal enum class ChoiceRowStyle { Card, Form }

@Composable
internal fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    style: ChoiceRowStyle = ChoiceRowStyle.Card,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (style == ChoiceRowStyle.Form) 0.dp else 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = if (style == ChoiceRowStyle.Form) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labels = options.map { label(it) }
            val measurer = rememberTextMeasurer()
            val textStyle = MaterialTheme.typography.labelLarge
            val density = LocalDensity.current
            val widest = with(density) {
                labels.maxOf { measurer.measure(it, textStyle, maxLines = 1).size.width }.toDp()
            }
            // A segment that cannot hold its label on one line wraps it and ends up taller than the others.
            val fits = widest + SEGMENT_CHROME <= maxWidth / options.size
            if (density.fontScale > STACK_FONT_SCALE || !fits) {
                Column(Modifier.fillMaxWidth().padding(top = 4.dp).selectableGroup()) {
                    options.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = option == selected,
                                    enabled = enabled,
                                    role = Role.RadioButton,
                                    onClick = { onSelect(option) },
                                ),
                        ) {
                            RadioButton(selected = option == selected, onClick = null, enabled = enabled)
                            Text(labels[index], Modifier.padding(start = 16.dp))
                        }
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            enabled = enabled,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(labels[index], maxLines = 1)
                        }
                    }
                }
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
