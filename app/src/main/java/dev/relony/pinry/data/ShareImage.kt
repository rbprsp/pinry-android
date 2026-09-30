package dev.relony.pinry.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/** What to pin for a shared or typed URL: the image, and the page it was found on (if any). */
data class ImageSource(val imageUrl: String, val pageUrl: String?)

/**
 * Pinry can only pin direct image URLs. Browsers share page URLs, so for a page the image it
 * advertises for link previews (`og:image`, `twitter:image`) is used instead.
 */
class ImageUrlResolver(private val http: OkHttpClient) {
    /** Null when the URL is neither an image nor a page that names one. */
    suspend fun resolve(url: String): ImageSource? = withContext(Dispatchers.IO) {
        val target = url.trim().toHttpUrlOrNull() ?: return@withContext null
        val request = Request.Builder()
            .url(target)
            // Some sites refuse OkHttp's default user agent.
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,image/*;q=0.9,*/*;q=0.8")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val type = response.body.contentType()
            val page = response.request.url // after redirects, e.g. pin.it → pinterest.com
            when {
                type?.type == "image" -> ImageSource(target.toString(), pageUrl = null)
                type?.subtype == "html" -> findShareImage(response.peekBody(MAX_HTML_BYTES).string(), page)
                    ?.let { ImageSource(it, page.toString()) }
                else -> null
            }
        }
    }

    private companion object {
        const val MAX_HTML_BYTES = 512L * 1024
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"
    }
}

private val MetaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val Attribute = Regex("""([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
private val ImageKeys = listOf("og:image", "og:image:secure_url", "og:image:url", "twitter:image", "twitter:image:src")

/** The link-preview image a page declares, resolved against [pageUrl]. */
fun findShareImage(html: String, pageUrl: HttpUrl): String? {
    val found = mutableMapOf<String, String>()
    for (tag in MetaTag.findAll(html)) {
        val attributes = Attribute.findAll(tag.value)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2].ifEmpty { it.groupValues[3] } }
        val key = (attributes["property"] ?: attributes["name"])?.lowercase() ?: continue
        val content = attributes["content"]?.trim().orEmpty()
        if (content.isNotEmpty()) found.putIfAbsent(key, content)
    }
    val raw = ImageKeys.firstNotNullOfOrNull { found[it] } ?: return null
    return pageUrl.resolve(decodeHtmlEntities(raw))?.toString()
}

private val Entity = Regex("&(#[xX][0-9a-fA-F]+|#\\d+|amp|quot|apos|lt|gt);")

fun decodeHtmlEntities(text: String): String = Entity.replace(text) { match ->
    val name = match.groupValues[1]
    when {
        name.startsWith("#x", ignoreCase = true) -> String(Character.toChars(name.drop(2).toInt(16)))
        name.startsWith("#") -> String(Character.toChars(name.drop(1).toInt()))
        name == "amp" -> "&"
        name == "quot" -> "\""
        name == "apos" -> "'"
        name == "lt" -> "<"
        else -> ">"
    }
}

private val UrlInText = Regex("""https?://[^\s"'<>]+""")

/** Apps often share "Look at this https://…"; takes the first URL out of such text. */
fun firstUrl(text: String): String? = UrlInText.find(text)?.value?.trimEnd('.', ',', ')', ']', '!', '?')
