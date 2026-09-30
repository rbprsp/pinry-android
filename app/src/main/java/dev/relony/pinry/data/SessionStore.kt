package dev.relony.pinry.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.relony.pinry.data.api.PinryJson
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val baseUrl: String,
    /** Null together with [token] when browsing a public instance without an account. */
    val username: String? = null,
    val token: String? = null,
    /** Cookies in `Set-Cookie` form, restored with `Cookie.parse(baseUrl, …)`. */
    val cookies: List<String> = emptyList(),
) {
    val isAnonymous: Boolean get() = token == null
}

class SessionStore(private val store: DataStore<Preferences>) {
    suspend fun read(): Session? = store.data.first()[SESSION]
        ?.let { runCatching { PinryJson.decodeFromString<Session>(it) }.getOrNull() }

    /** Kept after logout so the login form can be prefilled. */
    suspend fun lastServer(): String? = store.data.first()[LAST_SERVER]

    suspend fun write(session: Session) {
        store.edit {
            it[SESSION] = PinryJson.encodeToString(session)
            it[LAST_SERVER] = session.baseUrl
        }
    }

    suspend fun clear() {
        store.edit { it.remove(SESSION) }
    }

    private companion object {
        val SESSION = stringPreferencesKey("session")
        val LAST_SERVER = stringPreferencesKey("last_server")
    }
}
