package dev.relony.pinry.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.relony.pinry.data.api.PinryJson
import dev.relony.pinry.data.net.AuthInterceptor
import dev.relony.pinry.data.net.HostCookieJar
import dev.relony.pinry.data.net.ServerContext
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val pinry = MockWebServer()
    private val other = MockWebServer()
    private val job = SupervisorJob()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: SessionStore
    private lateinit var app: App

    /** One app process: its own server context, cookie jar and client, sharing the on-disk store. */
    private inner class App {
        val server = ServerContext()
        val cookies = HostCookieJar(server)
        val http: OkHttpClient = OkHttpClient.Builder()
            .cookieJar(cookies)
            .addInterceptor(AuthInterceptor(server))
            .build()
        val auth = AuthRepository(http, server, cookies, store, CoroutineScope(job + Dispatchers.IO))

        fun get(url: okhttp3.HttpUrl) = http.newCall(Request(url)).execute().use { it.code }
    }

    @Before
    fun setUp() {
        pinry.start()
        other.start()
        dataStore = PreferenceDataStoreFactory.create { File(tmp.root, "session.preferences_pb") }
        store = SessionStore(dataStore)
        app = App()
    }

    @After
    fun tearDown() {
        pinry.close()
        other.close()
    }

    private val base get() = pinry.url("/").toString()

    private fun response(body: String, code: Int = 200, type: String = "application/json", vararg headers: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", type).body(body)
            .apply { headers.forEach { addHeader(it) } }
            .build()

    private fun enqueueSuccessfulLogin(on: MockWebServer = pinry) {
        on.enqueue(response("[]", headers = arrayOf("Set-Cookie: csrftoken=csrf123; Path=/")))
        on.enqueue(
            response(
                """{"username":"me","email":"me@example.com","gravatar":"abc","token":"tok","resource_link":"x"}""",
                headers = arrayOf("Set-Cookie: sessionid=sess456; Path=/; HttpOnly"),
            )
        )
    }

    private fun loginAndDrainRequests() = runBlocking {
        enqueueSuccessfulLogin()
        app.auth.login(base, "me", "pw")
        pinry.takeRequest()
        pinry.takeRequest()
    }

    /** Waits for the fire-and-forget store writes launched by AuthRepository. */
    private fun awaitBackgroundWork() = runBlocking { job.children.forEach { it.join() } }

    @Test
    fun loginSendsCsrfTokenAndRefererThenStoresSession() = runBlocking {
        enqueueSuccessfulLogin()

        app.auth.login(base, "me", "pw")

        val probe = pinry.takeRequest()
        assertEquals("/api/v2/profile/users/", probe.url.encodedPath)
        val login = pinry.takeRequest()
        assertEquals("POST", login.method)
        assertEquals("/api/v2/profile/login/", login.url.encodedPath)
        assertEquals("csrf123", login.headers["X-CSRFToken"])
        assertEquals(base, login.headers["Referer"])
        assertTrue(login.headers["Cookie"]!!.contains("csrftoken=csrf123"))
        val body = PinryJson.parseToJsonElement(login.body!!.utf8()).jsonObject
        assertEquals("me", body["username"]!!.jsonPrimitive.content)
        assertEquals("pw", body["password"]!!.jsonPrimitive.content)

        val state = app.auth.state.value as SessionState.LoggedIn
        assertEquals("tok", state.session.token)
        val saved = store.read()!!
        assertEquals("tok", saved.token)
        assertTrue(saved.cookies.any { it.startsWith("sessionid=sess456") })
    }

    @Test
    fun tokenAndCookiesGoOnlyToTheServer() {
        loginAndDrainRequests()
        pinry.enqueue(response("""{"count":0,"results":[]}"""))
        other.enqueue(response("", type = "image/jpeg"))

        app.get(pinry.url("/api/v2/pins/"))
        app.get(other.url("/media/cat.jpg"))

        val own = pinry.takeRequest()
        assertEquals("Token tok", own.headers["Authorization"])
        assertTrue(own.headers["Cookie"]!!.contains("sessionid=sess456"))
        val foreign = other.takeRequest()
        assertNull(foreign.headers["Authorization"])
        assertNull(foreign.headers["Cookie"])
    }

    @Test
    fun wrongPasswordReportsTheServersFieldError() = runBlocking {
        pinry.enqueue(response("[]", headers = arrayOf("Set-Cookie: csrftoken=csrf123; Path=/")))
        // Pinry's login view sends its JSON error labelled as text/html.
        pinry.enqueue(response("""{"password": "username and password doesn't match"}""", code = 400, type = "text/html"))

        try {
            app.auth.login(base, "me", "wrong")
            fail("expected LoginException")
        } catch (e: LoginException) {
            assertEquals("username and password doesn't match", e.fields["password"])
        }
    }

    @Test
    fun nonPinryServerIsRejected() = runBlocking {
        pinry.enqueue(response("<html>nope</html>", code = 404, type = "text/html"))

        try {
            app.auth.login(base, "me", "pw")
            fail("expected LoginException")
        } catch (e: LoginException) {
            assertTrue(e.fields.containsKey(AuthRepository.FIELD_SERVER))
        }
    }

    @Test
    fun redirectToAnotherOriginLogsInThere() = runBlocking {
        // Stands in for nginx's http → https redirect.
        pinry.enqueue(MockResponse.Builder().code(301).addHeader("Location", other.url("/api/v2/profile/users/")).build())
        other.enqueue(response("[]")) // answers the redirected request; its cookies belong to the old origin
        enqueueSuccessfulLogin(on = other)

        app.auth.login(base, "me", "pw")

        val state = app.auth.state.value as SessionState.LoggedIn
        assertEquals(other.url("/").toString(), state.session.baseUrl)
        other.takeRequest()
        other.takeRequest()
        val login = other.takeRequest()
        assertEquals("POST", login.method)
        assertEquals("csrf123", login.headers["X-CSRFToken"])
    }

    @Test
    fun middlewareForbiddenEndsTheSessionButDrfForbiddenDoesNot() {
        loginAndDrainRequests()

        pinry.enqueue(response("""{"detail":"You do not have permission to perform this action."}""", code = 403))
        app.get(pinry.url("/api/v2/pins/1/"))
        assertTrue(app.auth.state.value is SessionState.LoggedIn)

        // PUBLIC = False instance: Django middleware returns an empty text/html 403.
        pinry.enqueue(response("", code = 403, type = "text/html"))
        app.get(pinry.url("/api/v2/pins/"))
        assertEquals(SessionState.LoggedOut(base), app.auth.state.value)
        awaitBackgroundWork()
        assertNull(runBlocking { store.read() })
    }

    @Test
    fun restoreBringsTheSessionBackAfterRestart() = runBlocking {
        loginAndDrainRequests()
        awaitBackgroundWork()

        val restarted = App()
        restarted.auth.restore()
        pinry.enqueue(response("""{"count":0,"results":[]}"""))
        restarted.get(pinry.url("/api/v2/pins/"))

        assertTrue(restarted.auth.state.value is SessionState.LoggedIn)
        val request = pinry.takeRequest()
        assertEquals("Token tok", request.headers["Authorization"])
        assertTrue(request.headers["Cookie"]!!.contains("sessionid=sess456"))
    }

    @Test
    fun logoutClearsSessionButRemembersServer() = runBlocking {
        loginAndDrainRequests()
        pinry.enqueue(response("", type = "text/html"))

        app.auth.logout()

        assertEquals(SessionState.LoggedOut(base), app.auth.state.value)
        assertNull(store.read())
        assertEquals(base, store.lastServer())
        assertEquals("/api/v2/profile/logout/", pinry.takeRequest().url.encodedPath)
    }

    @Test
    fun serverAddressIsNormalised() {
        assertEquals("https://pinry.example.com/", parseServerUrl(" pinry.example.com ").toString())
        assertEquals("http://nas:8080/", parseServerUrl("http://nas:8080").toString())
        assertEquals("https://host/pinry/", parseServerUrl("https://host/pinry?x=1").toString())
        assertNull(parseServerUrl(""))
        assertNull(parseServerUrl("https://bad host"))
    }
}
