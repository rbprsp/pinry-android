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

    fun submit() {
        if (!canSubmit) return
        busy = true
        errors = emptyMap()
        viewModelScope.launch {
            try {
                auth.login(server, username.trim(), password)
            } catch (e: LoginException) {
                errors = e.fields
            } finally {
                busy = false
            }
        }
    }
}
