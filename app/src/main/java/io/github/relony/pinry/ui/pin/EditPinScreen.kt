package io.github.relony.pinry.ui.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.relony.pinry.R
import io.github.relony.pinry.ui.common.BackTopBar
import io.github.relony.pinry.ui.common.PinryLoadingIndicator
import io.github.relony.pinry.ui.create.PinFormFields

@Composable
fun EditPinScreen(vm: EditPinViewModel, onBack: () -> Unit) {
    LaunchedEffect(vm.done) { if (vm.done) onBack() }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackTopBar(stringResource(R.string.edit_title), onBack)
        val form = vm.form
        if (form == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: PinryLoadingIndicator()
            }
            return@Column
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PinFormFields(form, vm.allTags)
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = !vm.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (vm.saving) R.string.create_saving else R.string.save))
            }
        }
    }
}
