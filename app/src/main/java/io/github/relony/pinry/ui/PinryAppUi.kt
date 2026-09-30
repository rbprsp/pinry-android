package io.github.relony.pinry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.relony.pinry.R
import io.github.relony.pinry.ui.common.PinryToolbar
import io.github.relony.pinry.ui.common.ToolbarTab
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object BoardsKey : NavKey
@Serializable data object ProfileKey : NavKey

private val tabs = listOf(
    ToolbarTab<NavKey>(HomeKey, R.drawable.ic_home, R.string.tab_home),
    ToolbarTab<NavKey>(BoardsKey, R.drawable.ic_boards, R.string.tab_boards),
    ToolbarTab<NavKey>(ProfileKey, R.drawable.ic_profile, R.string.tab_profile),
)

@Composable
fun PinryAppUi(username: String, onLogout: () -> Unit) {
    // Home is always the root; another tab sits on top of it, so Back from a tab returns Home.
    val backStack = rememberNavBackStack(HomeKey)
    val selectedTab = backStack.last { key -> tabs.any { it.key == key } }

    Surface(Modifier.fillMaxSize()) {
        Box {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<HomeKey> { Placeholder("Home") }
                    entry<BoardsKey> { Placeholder("Boards") }
                    entry<ProfileKey> { ProfilePlaceholder(username, onLogout) }
                },
            )
            PinryToolbar(
                tabs = tabs,
                selected = selectedTab,
                onSelect = { key ->
                    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    if (key != HomeKey) backStack.add(key)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
private fun ProfilePlaceholder(username: String, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.signed_in_as, username), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onLogout) { Text(stringResource(R.string.logout)) }
    }
}
