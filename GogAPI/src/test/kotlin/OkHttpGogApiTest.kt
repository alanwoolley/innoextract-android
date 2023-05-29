import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import uk.co.armedpineapple.innoextract.gogapi.OkHttpGogApi
import kotlin.test.Test
import kotlin.test.assertEquals

class OkHttpGogApiTest {

    @Test
    fun `getGameDetails should return GogGame`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(ExampleResponses.products))

        server.start()
        val baseUrl = server.url("/products")
        val api = OkHttpGogApi(baseUrl.toUrl(), OkHttpClient())
        val id = 1207659026L;
        val result = runBlocking { api.getGameDetails(id) }

        assertEquals("Theme Hospital", result.title)
        assertEquals(
            "https://images-3.gog-statics.com/e4c3461737eed20948cb84089d716694782584beba983fb09f36c14212b75afb.jpg",
            result.backgroundImg.toString()
        )
        assertEquals(
            "https://images-4.gog-statics.com/ee3234decbd0dcec371d441a3ab5e161a8353aed589efbe5b1ba464dea83d68b.png",
            result.logoImg.toString()
        )
    }
}