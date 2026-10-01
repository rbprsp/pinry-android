package dev.relony.pinry.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.relony.pinry.PinryApp
import dev.relony.pinry.data.net.LocalNetwork
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Returns a function that asks for the local network permission when the server on a host needs
 * it, and returns once the user has answered (at once when there's nothing to ask). Callers go on
 * either way: if the answer was no, the connection error says what to do.
 */
@Composable
fun rememberLocalNetworkRequest(): suspend (host: String) -> Unit {
    val localNetwork = localNetwork()
    val answers = remember { Channel<Unit>(Channel.CONFLATED) }
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { answers.trySend(Unit) }
    val oneAtATime = remember { Mutex() }
    return remember(localNetwork, launcher) {
        { host ->
            oneAtATime.withLock {
                if (localNetwork.shouldAsk(host)) {
                    answers.tryReceive()
                    launcher.launch(LocalNetwork.PERMISSION)
                    answers.receive()
                }
            }
        }
    }
}

/** Shows [content] once the local network permission has been asked for, if [baseUrl]'s server needs it. */
@Composable
fun LocalNetworkGate(baseUrl: String, content: @Composable () -> Unit) {
    val granted = localNetwork().granted
    val request = rememberLocalNetworkRequest()
    var asked by rememberSaveable(baseUrl) { mutableStateOf(false) }
    if (granted || asked) {
        content()
    } else {
        LaunchedEffect(baseUrl) {
            request(baseUrl.toHttpUrl().host)
            asked = true
        }
    }
}

@Composable
private fun localNetwork(): LocalNetwork = (LocalContext.current.applicationContext as PinryApp).container.localNetwork
