package dev.relony.pinry.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.relony.pinry.R
import dev.relony.pinry.ui.common.BackTopBar
import dev.relony.pinry.ui.common.PinryLoadingIndicator
import dev.relony.pinry.ui.common.UploadProgress

@Composable
fun CreatePinScreen(vm: CreatePinViewModel, onClose: () -> Unit, onDone: () -> Unit) {
    LaunchedEffect(vm.done) { if (vm.done) onDone() }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(stringResource(R.string.create_title), onClose, icon = R.drawable.ic_close)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Preview(vm)
            if (vm.fromUrl) {
                OutlinedTextField(
                    value = vm.url,
                    onValueChange = { vm.url = it },
                    label = { Text(stringResource(R.string.create_url)) },
                    supportingText = { Text(stringResource(R.string.create_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PinFormFields(vm.form, vm.allTags)
            BoardPicker(vm.boardList, vm.board, onSelect = { vm.board = it }, onCreate = vm::createBoard)
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            vm.progress?.let { progress -> UploadProgress({ progress }, Modifier.fillMaxWidth()) }
            Button(onClick = vm::submit, enabled = vm.canSubmit, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        when {
                            vm.progress != null -> R.string.create_uploading
                            vm.saving -> R.string.create_saving
                            else -> R.string.create_submit
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun Preview(vm: CreatePinViewModel) {
    val model: Any? = vm.localImage?.file ?: vm.resolved?.imageUrl
    var previewFailed by remember(model) { mutableStateOf(false) }
    var previewLoading by remember(model) { mutableStateOf(model != null) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
            when {
                previewFailed -> Hint(stringResource(R.string.create_preview_failed))
                model != null -> AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onSuccess = { previewLoading = false },
                    onError = { previewFailed = true },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                )
                vm.resolving || (!vm.fromUrl && vm.error == null) -> PinryLoadingIndicator()
                vm.noImageFound -> Hint(stringResource(R.string.create_no_image))
                vm.fromUrl -> Hint(stringResource(R.string.create_enter_url))
            }
            // Remote previews can take a while (the full image is downloaded).
            if (model != null && previewLoading && !previewFailed) PinryLoadingIndicator()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}
