package dev.relony.pinry.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.relony.pinry.R
import dev.relony.pinry.data.AuthRepository

@Composable
fun LoginScreen(vm: LoginViewModel) {
    val known = setOf(AuthRepository.FIELD_SERVER, "username", "password")
    val otherErrors = vm.errors.filterKeys { it !in known }.values

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Field(
                value = vm.server,
                onChange = { vm.server = it },
                label = stringResource(R.string.login_server),
                placeholder = "pinry.example.com",
                error = vm.errors[AuthRepository.FIELD_SERVER],
                hint = if (vm.server.trim().startsWith("http://", ignoreCase = true)) stringResource(R.string.login_http_warning) else null,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            )
            Field(
                value = vm.username,
                onChange = { vm.username = it },
                label = stringResource(R.string.login_username),
                error = vm.errors["username"],
                keyboard = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
            )
            Field(
                value = vm.password,
                onChange = { vm.password = it },
                label = stringResource(R.string.login_password),
                error = vm.errors["password"],
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                password = true,
                onDone = vm::submit,
            )
            otherErrors.forEach {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = vm::submit,
                enabled = vm.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (vm.busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_submit))
                }
            }
            TextButton(onClick = vm::browse, enabled = vm.canBrowse, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login_browse))
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboard: KeyboardOptions,
    hint: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
