package dev.relony.pinry.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
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
import dev.relony.pinry.ui.common.tagLabel

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

private val homeTab = ToolbarTab<NavKey>(HomeKey, R.drawable.ic_home, R.string.tab_home)
private val boardsTab = ToolbarTab<NavKey>(BoardsKey, R.drawable.ic_boards, R.string.tab_boards)
private val profileTab = ToolbarTab<NavKey>(ProfileKey, R.drawable.ic_profile, R.string.tab_profile)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PinryAppUi(session: Session, onLogout: () -> Unit) {
    val container = appContainer()
    // Null when browsing a public instance without an account: read-only, no Boards tab, no "+".
    val username = session.username
    val host = session.baseUrl.toHttpUrl().host
    val tabs = if (username == null) listOf(homeTab, profileTab) else listOf(homeTab, boardsTab, profileTab)
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
    val pop = defaultPopTransitionSpec<NavKey>()

    ToolbarScaffold(
        tabs = tabs,
        selected = selectedTab,
        onSelect = { key ->
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            if (key != HomeKey) backStack.add(key)
        },
        showToolbar = tabs.any { it.key == backStack.lastOrNull() },
        fabActions = if (username == null) emptyList() else listOf(
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
            // The default for the back gesture shrinks the screen without fading it, which leaves an
            // opaque copy of it over the grid while a pin's image flies back on its own. Fade instead,
            // like the back button does.
            predictivePopTransitionSpec = { pop() },
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
                        viewModel { BoardsViewModel(container.boards, checkNotNull(username)) },
                        onOpen = { open(FeedKey(PinFilter.Board(it.id, it.name))) },
                    )
                }
                entry<ProfileKey> {
                    if (username == null) {
                        GuestProfile(host, onLogin = onLogout, onSettings = { open(SettingsKey) })
                    } else {
                        FeedScreen(
                            feedViewModel(container, PinFilter.User(username)),
                            onOpen = { open(PinKey(it.id)) },
                            header = { ProfileHeader(username, onSettings = { open(SettingsKey) }) },
                        )
                    }
                }
                entry<FeedKey> { key ->
                    val filter = key.filter
                    if (filter is PinFilter.Board) {
                        BoardFeedScreen(
                            feed = feedViewModel(container, filter),
                            vm = viewModel { BoardViewModel(container.boards, filter.id, checkNotNull(username)) },
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
                        viewModel { container.createPinViewModel(context, checkNotNull(username), key.source) },
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
                        account = if (username == null) {
                            stringResource(R.string.settings_guest_line, host)
                        } else {
                            stringResource(R.string.settings_account_line, username, host)
                        },
                        signedIn = username != null,
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
    is PinFilter.Tag -> tagLabel(name)
    is PinFilter.User -> username
    is PinFilter.Board -> name
}

/** Profile tab without an account: where you are, and the way to log in. */
@Composable
private fun GuestProfile(host: String, onLogin: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.guest_title, host), style = MaterialTheme.typography.screenTitle, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.guest_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onLogin) { Text(stringResource(R.string.login_submit)) }
        TextButton(onClick = onSettings) { Text(stringResource(R.string.settings)) }
    }
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
