package io.github.relony.pinry.data

import io.github.relony.pinry.data.api.NewPin
import io.github.relony.pinry.data.api.createPinryApi
import java.io.File
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PinRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val server = MockWebServer()
    private lateinit var repository: PinRepository

    @Before
    fun setUp() {
        server.start()
        val api = createPinryApi(server.url("/"), OkHttpClient())
        repository = PinRepository({ api }, { server.url("/") })
    }

    @After fun tearDown() = server.close()

    private fun json(body: String) = MockResponse.Builder().code(201).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun uploadSendsTheFileAsMultipartImageFieldAndReportsProgress() = runBlocking {
        val file = File(tmp.root, "image-1.jpg").apply { writeBytes(ByteArray(200_000) { 7 }) }
        server.enqueue(json("""{"id":9,"image":"http://x/media/a.jpg","width":1,"height":1}"""))
        val progress = mutableListOf<Float>()

        val image = repository.upload(LocalImage(file, "image/jpeg")) { progress += it }

        assertEquals(9, image.id)
        val request = server.takeRequest()
        assertEquals("/api/v2/images/", request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body.contains("""Content-Disposition: form-data; name="image"; filename="image-1.jpg""""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
        assertEquals(1f, progress.last())
    }

    @Test
    fun createdPinIsAnnouncedWithRebasedMedia() = runBlocking {
        server.enqueue(json(PIN_JSON))
        val announced = async(start = CoroutineStart.UNDISPATCHED) { repository.changes.first() }

        repository.create(NewPin(imageId = 9, tags = listOf("cats")))

        val pin = (announced.await() as PinChange.Created).pin
        assertEquals(server.url("/media/a.jpg").toString(), pin.image.image)
        assertEquals(pin, repository.cached(5))
    }

    private companion object {
        const val PIN_JSON = """{"id":5,"private":false,"submitter":{"username":"dev"},
            "image":{"id":9,"image":"http://internal:8000/media/a.jpg","width":10,"height":10},"tags":["cats"]}"""
    }
}
