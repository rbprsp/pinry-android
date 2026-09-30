@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.relony.pinry.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.relony.pinry.R

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, @DrawableRes icon: Int = R.drawable.ic_arrow_back) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(icon), contentDescription = stringResource(R.string.back))
            }
        },
    )
}
