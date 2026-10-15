package dev.relony.pinry.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.relony.pinry.R
import dev.relony.pinry.data.api.Board
import dev.relony.pinry.ui.common.LocalToolbarInset
import dev.relony.pinry.ui.common.PinryLoadingIndicator
import dev.relony.pinry.ui.common.tagLabel

@Composable
fun SearchScreen(vm: SearchViewModel, onTag: (String) -> Unit, onBoard: (Board) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (vm.query.isEmpty()) focus.requestFocus() }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalToolbarInset.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
        )
        val nothing = vm.tags.isEmpty() && vm.boards.isEmpty()
        when {
            vm.query.isBlank() -> Unit
            nothing && vm.searching -> Box(Modifier.fillMaxSize(), Alignment.Center) { PinryLoadingIndicator() }
            nothing -> Hint(vm.error ?: stringResource(R.string.search_empty))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                if (vm.tags.isNotEmpty()) {
                    item { Section(stringResource(R.string.search_tags)) }
                    items(vm.tags) { tag ->
                        ListItem(headlineContent = { Text(tagLabel(tag)) }, modifier = Modifier.clickable { onTag(tag) })
                    }
                }
                if (vm.boards.isNotEmpty()) {
                    item { Section(stringResource(R.string.search_boards)) }
                    items(vm.boards, key = { it.id }) { board ->
                        ListItem(
                            headlineContent = { Text(board.name) },
                            supportingContent = { Text(stringResource(R.string.search_board_by, board.submitter.username)) },
                            modifier = Modifier.clickable { onBoard(board) },
                        )
                    }
                }
                vm.error?.let { item { Hint(it) } }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}
