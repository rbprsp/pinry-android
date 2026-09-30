package io.github.relony.pinry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.relony.pinry.data.SessionState
import io.github.relony.pinry.data.firstUrl
import io.github.relony.pinry.ui.create.CreatePinScreen
import io.github.relony.pinry.ui.create.CreateSource
import io.github.relony.pinry.ui.create.createPinViewModel
import io.github.relony.pinry.ui.theme.PinryTheme

/** Target of Android's share sheet: only the create-pin form, then back to the app that shared. */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val source = sharedSource(intent)
        if (source == null) {
            Toast.makeText(this, R.string.share_unsupported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val container = (application as PinryApp).container
        setContent {
            PinryTheme {
                val state by container.auth.state.collectAsStateWithLifecycle()
                Surface(Modifier.fillMaxSize()) {
                    when (val s = state) {
                        SessionState.Loading -> Unit
                        is SessionState.LoggedOut -> Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.share_logged_out), style = MaterialTheme.typography.titleMedium)
                            Button(onClick = {
                                startActivity(Intent(this@ShareActivity, MainActivity::class.java))
                                finish()
                            }) { Text(stringResource(R.string.open_app)) }
                        }
                        is SessionState.LoggedIn -> CreatePinScreen(
                            viewModel { container.createPinViewModel(this@ShareActivity, s.session.username, source) },
                            onClose = ::finish,
                            onDone = {
                                Toast.makeText(this@ShareActivity, R.string.share_done, Toast.LENGTH_SHORT).show()
                                finish()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun sharedSource(intent: Intent): CreateSource? {
    if (intent.action != Intent.ACTION_SEND) return null
    if (intent.type?.startsWith("image/") == true) {
        return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?.let { CreateSource.Local(it.toString()) }
    }
    return intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::firstUrl)?.let { CreateSource.Url(it) }
}
