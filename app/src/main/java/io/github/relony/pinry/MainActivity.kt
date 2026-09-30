package io.github.relony.pinry

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
import io.github.relony.pinry.data.SessionState
import io.github.relony.pinry.ui.PinryAppUi
import io.github.relony.pinry.ui.login.LoginScreen
import io.github.relony.pinry.ui.login.LoginViewModel
import io.github.relony.pinry.ui.theme.PinryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val auth = (application as PinryApp).container.auth
        setContent {
            PinryTheme {
                val state by auth.state.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize()) {
                    when (val s = state) {
                        SessionState.Loading -> Unit
                        is SessionState.LoggedOut -> LoginScreen(
                            viewModel { LoginViewModel(auth, s.lastServer) }
                        )
                        is SessionState.LoggedIn -> PinryAppUi(
                            username = s.session.username,
                            onLogout = { scope.launch { auth.logout() } },
                        )
                    }
                }
            }
        }
    }
}
