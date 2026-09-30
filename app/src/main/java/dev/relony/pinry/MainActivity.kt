package dev.relony.pinry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.relony.pinry.data.SessionState
import dev.relony.pinry.ui.PinryAppUi
import dev.relony.pinry.ui.login.LoginScreen
import dev.relony.pinry.ui.login.LoginViewModel
import dev.relony.pinry.ui.theme.PinryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PinryApp).container
        val auth = container.auth
        setContent {
            // Until settings are read (a few ms) the window background shows, not a wrong theme.
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            PinryTheme(settings ?: return@setContent) {
                val state by auth.state.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize()) {
                    when (val s = state) {
                        SessionState.Loading -> Unit
                        is SessionState.LoggedOut -> LoginScreen(
                            viewModel { LoginViewModel(auth, s.lastServer) }
                        )
                        is SessionState.LoggedIn -> PinryAppUi(
                            session = s.session,
                            onLogout = { scope.launch { auth.logout() } },
                        )
                    }
                }
            }
        }
    }
}
