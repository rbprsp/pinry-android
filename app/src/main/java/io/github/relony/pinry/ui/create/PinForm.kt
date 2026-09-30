@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.relony.pinry.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.BoardName

/** Description, tags and privacy: the fields shared by creating and editing a pin. */
class PinFormState(description: String = "", tags: List<String> = emptyList(), isPrivate: Boolean = false) {
    var description by mutableStateOf(description)
    var tags by mutableStateOf(tags)
        private set
    var tagInput by mutableStateOf("")
        private set
    var isPrivate by mutableStateOf(isPrivate)

    fun onTagInput(text: String) {
        if (text.endsWith(",")) addTag(text.dropLast(1)) else tagInput = text
    }

    fun addTag(raw: String) {
        val tag = raw.trim().removePrefix("#").trim()
        if (tag.isNotEmpty() && tag !in tags) tags = tags + tag
        tagInput = ""
    }

    fun removeTag(tag: String) {
        tags = tags - tag
    }

    /** A tag typed but not confirmed with Enter or a comma still counts when saving. */
    fun commitTagInput() {
        if (tagInput.isNotBlank()) addTag(tagInput)
    }

    fun suggestions(all: List<String>): List<String> {
        val query = tagInput.trim().removePrefix("#")
        if (query.isEmpty()) return emptyList()
        return all.filter { it.contains(query, ignoreCase = true) && it !in tags }
            .sortedBy { !it.startsWith(query, ignoreCase = true) }
            .take(8)
    }
}

@Composable
fun PinFormFields(form: PinFormState, allTags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = form.description,
            onValueChange = { form.description = it },
            label = { Text(stringResource(R.string.pin_description)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        TagEditor(form, allTags)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.pin_private), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.pin_private_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = form.isPrivate, onCheckedChange = { form.isPrivate = it })
        }
    }
}

@Composable
private fun TagEditor(form: PinFormState, allTags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = form.tagInput,
            onValueChange = form::onTagInput,
            label = { Text(stringResource(R.string.pin_tags)) },
            supportingText = { Text(stringResource(R.string.pin_tags_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { form.commitTagInput() }),
            modifier = Modifier.fillMaxWidth(),
        )
        val suggestions = form.suggestions(allTags)
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { SuggestionChip(onClick = { form.addTag(it) }, label = { Text("#$it") }) }
            }
        }
        if (form.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                form.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { form.removeTag(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.pin_remove_tag, tag),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun BoardPicker(
    boards: List<BoardName>,
    selected: BoardName?,
    onSelect: (BoardName?) -> Unit,
    onCreate: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.board_none),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.board)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.board_none)) }, onClick = { onSelect(null); expanded = false })
            boards.forEach { board ->
                DropdownMenuItem(text = { Text(board.name) }, onClick = { onSelect(board); expanded = false })
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.board_new)) },
                onClick = { creating = true; expanded = false },
            )
        }
    }
    if (creating) {
        NewBoardDialog(onDismiss = { creating = false }, onCreate = { onCreate(it); creating = false })
    }
}

@Composable
fun NewBoardDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.board_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.board_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
