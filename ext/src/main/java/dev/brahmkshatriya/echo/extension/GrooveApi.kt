package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GrooveApi {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder()
            builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            builder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            builder.addHeader("Accept-Language", "en-US,en;q=0.9")
            chain.proceed(builder.build())
        }
        .build()
    
    companion object {
        private const val BASE_URL = "https://pagalnew.com"
    }
    suspend fun search(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/search.php?find=$encodedQuery"
        return executeRequest(url)
    }
    suspend fun getSongDetails(songUrl: String): String {
        val url = if (songUrl.startsWith("http")) songUrl else "$BASE_URL$songUrl"
        return executeRequest(url)
    }
    suspend fun getAlbumDetails(albumUrl: String): String {
        val url = if (albumUrl.startsWith("http")) albumUrl else "$BASE_URL$albumUrl"
        return executeRequest(url)
    }
    suspend fun getCategory(categoryUrl: String, page: Int = 1): String {
        val url = if (page == 1) {
            if (categoryUrl.startsWith("http")) categoryUrl else "$BASE_URL$categoryUrl"
        } else {
            if (categoryUrl.startsWith("http")) "$categoryUrl/$page" else "$BASE_URL$categoryUrl/$page"
        }
        return executeRequest(url)
    }
    suspend fun getArtistSongs(artistUrl: String, page: Int = 1): String {
        val url = if (page == 1) {
            if (artistUrl.startsWith("http")) artistUrl else "$BASE_URL$artistUrl"
        } else {
            if (artistUrl.startsWith("http")) {
                val base = artistUrl.replace(".html", "")
                "$base/$page"
            } else {
                "$BASE_URL${artistUrl.replace(".html", "")}/$page"
            }
        }
        return executeRequest(url)
    }
    suspend fun getHomePage(): String {
        return executeRequest(BASE_URL)
    }   
    private suspend fun executeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        } 
        return response.body?.string() ?: throw Exception("Empty response body")
    }
}
