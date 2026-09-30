package dev.relony.pinry.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.data.AuthRepository
import dev.relony.pinry.data.LoginException
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: AuthRepository, lastServer: String?) : ViewModel() {
    var server by mutableStateOf(lastServer.orEmpty())
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var busy by mutableStateOf(false)
        private set
    var errors by mutableStateOf(emptyMap<String, String>())
        private set

    val canSubmit get() = !busy && server.isNotBlank() && username.isNotBlank() && password.isNotEmpty()
    val canBrowse get() = !busy && server.isNotBlank()

    fun submit() {
        if (canSubmit) attempt { auth.login(server, username.trim(), password) }
    }

    fun browse() {
        if (canBrowse) attempt { auth.browse(server) }
    }

    private fun attempt(action: suspend () -> Unit) {
        busy = true
        errors = emptyMap()
        viewModelScope.launch {
            try {
                action()
            } catch (e: LoginException) {
                errors = e.fields
            } finally {
                busy = false
            }
        }
    }
}
