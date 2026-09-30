package io.github.relony.pinry.data

import io.github.relony.pinry.data.api.PinUpdate
import io.github.relony.pinry.data.api.PinryJson
import io.github.relony.pinry.data.api.createPinryApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinryApiTest {
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private val api by lazy { createPinryApi(server.url("/"), OkHttpClient()) }

    private fun json(body: String) =
        MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun pinPatchAlwaysCarriesTags() = runBlocking {
        server.enqueue(json(PIN_JSON))

        api.updatePin(5, PinUpdate(tags = emptyList(), description = "new text"))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v2/pins/5/", request.url.encodedPath)
        val body = PinryJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertTrue(body["tags"]!!.jsonArray.isEmpty())
        assertEquals("new text", body["description"].toString().trim('"'))
        assertFalse("unset fields are left out", body.containsKey("private"))
    }

    @Test
    fun pinsPageParsesPinryResponse() = runBlocking {
        server.enqueue(json("""{"count":1,"next":null,"previous":null,"results":[$PIN_JSON]}"""))

        val page = api.pins(offset = 0, tag = "cats")

        val request = server.takeRequest()
        assertEquals("cats", request.url.queryParameter("tags__name"))
        assertEquals("-id", request.url.queryParameter("ordering"))
        assertNull(request.url.queryParameter("pins__id"))
        val pin = page.results.single()
        assertEquals(listOf("cats", "art"), pin.tags)
        assertEquals(240, pin.image.thumbnail!!.width)
        assertNull(pin.submitter.token)
        assertNull(page.next)
    }

    private companion object {
        // Shape of core.serializers.PinSerializer output.
        const val PIN_JSON = """{"resource_link":"http://x/api/v2/pins/5/","private":false,"id":5,
            "submitter":{"username":"me","token":null,"email":"me@example.com","gravatar":"abc","resource_link":"http://x/u/"},
            "url":"https://src.example/a.jpg","description":"hi","referer":"https://src.example/",
            "image":{"id":7,"image":"http://x/media/a.jpg","width":800,"height":600,
              "standard":{"image":"http://x/media/s.jpg","width":600,"height":450},
              "thumbnail":{"image":"http://x/media/t.jpg","width":240,"height":180},
              "square":{"image":"http://x/media/q.jpg","width":125,"height":125}},
            "tags":["cats","art"]}"""
    }
}
