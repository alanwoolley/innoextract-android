package uk.co.armedpineapple.innoextract.gogapi

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import java.net.URL

/**
 * An API for GOG that uses OKHttp.
 *
 * @property endpoint The API endpoint.
 * @property client An OkHttpClient.
 */
class OkHttpGogApi(
    private val endpoint: URL = URL("https://api.gog.com/products/"),
    private val client: OkHttpClient
) : GogApi {

    constructor(endpoint: URL = URL("https://api.gog.com/products/"), context: Context) : this(
        endpoint,
        OkHttpClient.Builder().cache(Cache(context.cacheDir, maxSize = 1024 * 1024)).build()
    )

    private val protoPrefix = "https:"


    override suspend fun getGameDetails(gameId: Long): GogGame {
        val request = Request.Builder().url("$endpoint$gameId").build()

        val result = client.newCall(request).await()
        result.use { response ->
            if (!response.isSuccessful) throw GogApiException()

            response.body.use { body ->
                val parsed = Gson().fromJson(body.string(), JsonObject::class.java)
                val images = parsed["images"] as JsonObject
                val background = images["background"].asString
                val logo = images["icon"].asString
                val name = parsed["title"].asString
                return GogGame(
                    backgroundImg = URL(protoPrefix + background),
                    logoImg = URL(protoPrefix + logo),
                    title = name
                )
            }
        }
    }
}
