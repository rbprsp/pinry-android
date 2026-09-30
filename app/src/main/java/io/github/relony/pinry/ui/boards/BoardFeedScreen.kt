package io.github.relony.pinry.ui.boards

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.ui.feed.FeedScreen
import io.github.relony.pinry.ui.feed.FeedViewModel

/** A board's pins, with rename / private / delete for the owner. */
@Composable
fun BoardFeedScreen(
    feed: FeedViewModel,
    vm: BoardViewModel,
    fallbackTitle: String,
    onOpen: (Pin) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(vm.deleted) { if (vm.deleted) onBack() }
    var menu by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val board = vm.board

    FeedScreen(
        vm = feed,
        onOpen = onOpen,
        title = board?.name ?: fallbackTitle,
        onBack = onBack,
        actions = {
            if (board != null && vm.isMine) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.board_rename)) }, onClick = { menu = false; renaming = true })
                        DropdownMenuItem(
                            text = { Text(stringResource(if (board.isPrivate) R.string.board_make_public else R.string.board_make_private)) },
                            onClick = { menu = false; vm.setPrivate(!board.isPrivate) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                }
            }
        },
        header = vm.error?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
    )

    if (renaming && board != null) {
        var name by rememberSaveable { mutableStateOf(board.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.board_rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.rename(name); renaming = false }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.board_delete_confirm_title)) },
            text = { Text(stringResource(R.string.board_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text(stringResource(R.string.board_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
