package dev.relony.pinry.data.net

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Keeps cookies (Django's `csrftoken` and `sessionid`) for the current server only. */
class HostCookieJar(private val server: ServerContext) : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    /**
     * Called with the full cookie list when a cookie's value changes, so it can be persisted.
     * Django re-sends `csrftoken` with a fresh expiry on every response; that alone isn't a change.
     */
    @Volatile var onChanged: (List<Cookie>) -> Unit = {}

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!server.owns(url)) return
        val before = this.cookies.map { it.name to it.value }
        for (new in cookies) {
            this.cookies.removeAll { it.name == new.name && it.domain == new.domain && it.path == new.path }
            this.cookies += new
        }
        this.cookies.removeAll { it.expiresAt < System.currentTimeMillis() }
        if (this.cookies.map { it.name to it.value } != before) onChanged(this.cookies.toList())
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!server.owns(url)) return emptyList()
        val now = System.currentTimeMillis()
        return cookies.filter { it.expiresAt >= now && it.matches(url) }
    }

    @Synchronized
    fun value(name: String): String? = cookies.lastOrNull { it.name == name }?.value

    @Synchronized
    fun all(): List<Cookie> = cookies.toList()

    @Synchronized
    fun replaceAll(saved: List<Cookie>) {
        cookies.clear()
        cookies += saved
    }
}
