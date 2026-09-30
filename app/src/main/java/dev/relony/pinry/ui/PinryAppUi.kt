package dev.relony.pinry.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.relony.pinry.AppContainer
import dev.relony.pinry.PinryApp
import dev.relony.pinry.R
import dev.relony.pinry.data.PinFilter
import dev.relony.pinry.data.Session
import dev.relony.pinry.data.api.BoardName
import dev.relony.pinry.ui.boards.BoardFeedScreen
import dev.relony.pinry.ui.boards.BoardViewModel
import dev.relony.pinry.ui.boards.BoardsScreen
import dev.relony.pinry.ui.boards.BoardsViewModel
import dev.relony.pinry.ui.common.FabAction
import dev.relony.pinry.ui.common.LocalSharedTransitionScope
import dev.relony.pinry.ui.common.ToolbarScaffold
import dev.relony.pinry.ui.common.ToolbarTab
import dev.relony.pinry.ui.create.CreatePinScreen
import dev.relony.pinry.ui.create.CreateSource
import dev.relony.pinry.ui.create.createPinViewModel
import dev.relony.pinry.ui.feed.FeedScreen
import dev.relony.pinry.ui.feed.FeedViewModel
import dev.relony.pinry.ui.pin.EditPinScreen
import dev.relony.pinry.ui.pin.EditPinViewModel
import dev.relony.pinry.ui.pin.ImageViewerScreen
import dev.relony.pinry.ui.pin.PinDetailScreen
import dev.relony.pinry.ui.pin.PinViewModel
import dev.relony.pinry.ui.settings.SettingsScreen
import dev.relony.pinry.ui.theme.LocalAppSettings
import dev.relony.pinry.ui.theme.screenTitle
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey
@Serializable data object BoardsKey : NavKey
@Serializable data object ProfileKey : NavKey
@Serializable data class FeedKey(val filter: PinFilter) : NavKey
/** [fromBoard]: opened from one of the user's boards, so the pin can be removed from it. */
@Serializable data class PinKey(val id: Int, val fromBoard: BoardName? = null) : NavKey
@Serializable data class ViewerKey(val id: Int) : NavKey
@Serializable data class CreateKey(val source: CreateSource) : NavKey
@Serializable data class EditPinKey(val id: Int) : NavKey
@Serializable data object SettingsKey : NavKey

private val tabs = listOf(
    ToolbarTab<NavKey>(HomeKey, R.drawable.ic_home, R.string.tab_home),
    ToolbarTab<NavKey>(BoardsKey, R.drawable.ic_boards, R.string.tab_boards),
    ToolbarTab<NavKey>(ProfileKey, R.drawable.ic_profile, R.string.tab_profile),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PinryAppUi(session: Session, onLogout: () -> Unit) {
    val container = appContainer()
    val username = session.username
    val scope = rememberCoroutineScope()
    // Home is always the root; another tab sits on top of it, so Back from a tab returns Home.
    val backStack = rememberNavBackStack(HomeKey)
    val selectedTab = backStack.lastOrNull { key -> tabs.any { it.key == key } }
    // A quick double tap would otherwise stack the same screen twice.
    val open: (NavKey) -> Unit = { if (backStack.lastOrNull() != it) backStack.add(it) }
    val back: () -> Unit = { backStack.removeLastOrNull() }
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) open(CreateKey(CreateSource.Local(uri.toString())))
    }

    ToolbarScaffold(
        tabs = tabs,
        selected = selectedTab,
        onSelect = { key ->
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            if (key != HomeKey) backStack.add(key)
        },
        showToolbar = tabs.any { it.key == backStack.lastOrNull() },
        fabActions = listOf(
            FabAction(R.drawable.ic_photo, R.string.create_from_gallery) {
                pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            FabAction(R.drawable.ic_link, R.string.create_from_url) { open(CreateKey(CreateSource.Url(""))) },
        ),
    ) {
        SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        NavDisplay(
            backStack = backStack,
            onBack = back,
            sharedTransitionScope = this,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeKey> {
                    FeedScreen(feedViewModel(container, PinFilter.All), onOpen = { open(PinKey(it.id)) })
                }
                entry<BoardsKey> {
                    BoardsScreen(
                        viewModel { BoardsViewModel(container.boards, username) },
                        onOpen = { open(FeedKey(PinFilter.Board(it.id, it.name))) },
                    )
                }
                entry<ProfileKey> {
                    FeedScreen(
                        feedViewModel(container, PinFilter.User(username)),
                        onOpen = { open(PinKey(it.id)) },
                        header = { ProfileHeader(username, onSettings = { open(SettingsKey) }) },
                    )
                }
                entry<FeedKey> { key ->
                    val filter = key.filter
                    if (filter is PinFilter.Board) {
                        BoardFeedScreen(
                            feed = feedViewModel(container, filter),
                            vm = viewModel { BoardViewModel(container.boards, filter.id, username) },
                            fallbackTitle = filter.name,
                            onOpen = { open(PinKey(it.id, fromBoard = BoardName(filter.id, filter.name))) },
                            onBack = back,
                        )
                    } else {
                        FeedScreen(
                            feedViewModel(container, filter),
                            onOpen = { open(PinKey(it.id)) },
                            title = filter.title(),
                            onBack = back,
                        )
                    }
                }
                entry<PinKey> { key ->
                    PinDetailScreen(
                        viewModel { PinViewModel(container.pins, container.boards, key.id, me = username) },
                        fromBoard = key.fromBoard,
                        onBack = back,
                        onEdit = { open(EditPinKey(key.id)) },
                        onOpenImage = { open(ViewerKey(key.id)) },
                        onTag = { open(FeedKey(PinFilter.Tag(it))) },
                        onUser = { open(FeedKey(PinFilter.User(it))) },
                    )
                }
                entry<ViewerKey> { key ->
                    ImageViewerScreen(viewModel { PinViewModel(container.pins, container.boards, key.id, me = username) }, onBack = back)
                }
                entry<CreateKey> { key ->
                    CreatePinScreen(
                        viewModel { container.createPinViewModel(context, username, key.source) },
                        onClose = back,
                        onDone = back,
                    )
                }
                entry<EditPinKey> { key ->
                    EditPinScreen(viewModel { EditPinViewModel(container.pins, container.tags, key.id) }, onBack = back)
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        settings = LocalAppSettings.current,
                        onChange = { transform -> scope.launch { container.settings.update(transform) } },
                        account = stringResource(R.string.settings_account_line, username, session.baseUrl.toHttpUrl().host),
                        onLogout = onLogout,
                        onBack = back,
                    )
                }
            },
        )
        }
        }
    }
}

@Composable
private fun appContainer(): AppContainer = (LocalContext.current.applicationContext as PinryApp).container

@Composable
private fun feedViewModel(container: AppContainer, filter: PinFilter) =
    viewModel { FeedViewModel(container.pins, container.boards, filter) }

private fun PinFilter.title(): String? = when (this) {
    PinFilter.All -> null
    is PinFilter.Tag -> "#$name"
    is PinFilter.User -> username
    is PinFilter.Board -> name
}

@Composable
private fun ProfileHeader(username: String, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(username, style = MaterialTheme.typography.screenTitle, modifier = Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
        }
    }
}
