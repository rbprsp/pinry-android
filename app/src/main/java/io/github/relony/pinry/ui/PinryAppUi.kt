package io.github.relony.pinry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.relony.pinry.AppContainer
import io.github.relony.pinry.PinryApp
import io.github.relony.pinry.R
import io.github.relony.pinry.data.PinFilter
import io.github.relony.pinry.ui.common.ToolbarScaffold
import io.github.relony.pinry.ui.common.ToolbarTab
import io.github.relony.pinry.ui.feed.FeedScreen
import io.github.relony.pinry.ui.feed.FeedViewModel
import io.github.relony.pinry.ui.pin.ImageViewerScreen
import io.github.relony.pinry.ui.pin.PinDetailScreen
import io.github.relony.pinry.ui.pin.PinViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object BoardsKey : NavKey
@Serializable data object ProfileKey : NavKey
@Serializable data class FeedKey(val filter: PinFilter) : NavKey
@Serializable data class PinKey(val id: Int) : NavKey
@Serializable data class ViewerKey(val id: Int) : NavKey

private val tabs = listOf(
    ToolbarTab<NavKey>(HomeKey, R.drawable.ic_home, R.string.tab_home),
    ToolbarTab<NavKey>(BoardsKey, R.drawable.ic_boards, R.string.tab_boards),
    ToolbarTab<NavKey>(ProfileKey, R.drawable.ic_profile, R.string.tab_profile),
)

@Composable
fun PinryAppUi(username: String, onLogout: () -> Unit) {
    val container = appContainer()
    // Home is always the root; another tab sits on top of it, so Back from a tab returns Home.
    val backStack = rememberNavBackStack(HomeKey)
    val selectedTab = backStack.lastOrNull { key -> tabs.any { it.key == key } }
    val open: (NavKey) -> Unit = { backStack.add(it) }
    val back: () -> Unit = { backStack.removeLastOrNull() }

    ToolbarScaffold(
        tabs = tabs,
        selected = selectedTab,
        onSelect = { key ->
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            if (key != HomeKey) backStack.add(key)
        },
        showToolbar = tabs.any { it.key == backStack.lastOrNull() },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = back,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeKey> {
                    FeedScreen(feedViewModel(container, PinFilter.All), onOpen = { open(PinKey(it.id)) })
                }
                entry<BoardsKey> { Placeholder(stringResource(R.string.tab_boards)) }
                entry<ProfileKey> {
                    FeedScreen(
                        feedViewModel(container, PinFilter.User(username)),
                        onOpen = { open(PinKey(it.id)) },
                        header = { ProfileHeader(username, onLogout) },
                    )
                }
                entry<FeedKey> { key ->
                    FeedScreen(
                        feedViewModel(container, key.filter),
                        onOpen = { open(PinKey(it.id)) },
                        title = key.filter.title(),
                        onBack = back,
                    )
                }
                entry<PinKey> { key ->
                    PinDetailScreen(
                        viewModel { PinViewModel(container.pins, key.id) },
                        onBack = back,
                        onOpenImage = { open(ViewerKey(key.id)) },
                        onTag = { open(FeedKey(PinFilter.Tag(it))) },
                        onUser = { open(FeedKey(PinFilter.User(it))) },
                    )
                }
                entry<ViewerKey> { key ->
                    ImageViewerScreen(viewModel { PinViewModel(container.pins, key.id) }, onBack = back)
                }
            },
        )
    }
}

@Composable
private fun appContainer(): AppContainer = (LocalContext.current.applicationContext as PinryApp).container

@Composable
private fun feedViewModel(container: AppContainer, filter: PinFilter) =
    viewModel { FeedViewModel(container.pins, filter) }

private fun PinFilter.title(): String? = when (this) {
    PinFilter.All -> null
    is PinFilter.Tag -> "#$name"
    is PinFilter.User -> username
    is PinFilter.Board -> name
}

@Composable
private fun ProfileHeader(username: String, onLogout: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(username, style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = onLogout) { Text(stringResource(R.string.logout)) }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.displaySmall)
    }
}
