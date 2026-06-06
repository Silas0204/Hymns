package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class GeminiSongOutput(
    val title: String,
    val artist: String,
    val originalKey: String,
    val content: String // Lyrics with inline chords in bracket format [C] [G] etc.
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    val songAdapter by lazy {
        moshi.adapter(GeminiSongOutput::class.java)
    }
}

object GeminiRepository {

    private suspend fun fetchHtmlContent(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }
                val request = okhttp3.Request.Builder()
                    .url(cleanUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .addHeader("Accept-Language", "en-US,en;q=0.5")
                    .build()
                RetrofitClient.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        // Strip styling and scripts to reduce tokens
                        val plainText = html
                            .replace("<script[\\s\\S]*?>[\\s\\S]*?</script>".toRegex(), "")
                            .replace("<style[\\s\\S]*?>[\\s\\S]*?</style>".toRegex(), "")
                            .replace("<[^>]*>".toRegex(), " ")
                            .replace("\\s+".toRegex(), " ")
                        plainText.take(18000) // Get the first 18,000 characters for rich content
                    } else {
                        "HTTP_ERROR_${response.code}"
                    }
                }
            } catch (e: Exception) {
                "FETCH_FAILED_ERROR: ${e.message}"
            }
        }
    }

    suspend fun fetchSongChords(query: String, searchModeUrl: Boolean = false): GeminiSongOutput? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio.")
        }

        var webScrapeText = ""
        val searchTypeMessage = if (searchModeUrl) {
            val isUrl = query.contains(".") || query.startsWith("http://") || query.startsWith("https://")
            if (isUrl) {
                webScrapeText = fetchHtmlContent(query)
                if (webScrapeText.startsWith("HTTP_ERROR_") || webScrapeText.startsWith("FETCH_FAILED_ERROR:")) {
                    "extract and reconstruct chords for the song/page at '$query'. (Note: Scraper failed to fetch because of browser blocks, so please rely on your vast musical knowledge of this URL or song to provide the perfect lyrics and chords)."
                } else {
                    "parse the scraped web content provided below and extract the lyrics and exact aligned guitar chords for the song represented at URL '$query'.\n\nSCRAPED CONTENT FROM CURRENT PAGE:\n$webScrapeText"
                }
            } else {
                "find and transcribe lyrics and chords for: '$query'."
            }
        } else {
            "find and structure the lyrics and perfect guitar chords for: '$query'. If this song is rare, obscure, or custom, use your advanced musicology intelligence to reconstruct its standard structure, lyrics, and typical musician chord progression."
        }

        val prompt = """
            Please $searchTypeMessage 
            
            Format the response as a solid, valid JSON object. Do not wrap the JSON output inside markdown block tags (like ```json).
            
            The JSON object MUST have exactly these property names:
            - "title": The title of the song (e.g. "Hotel California").
            - "artist": The artist or band name (e.g. "Eagles").
            - "originalKey": The estimated key of the song (e.g. "Bm", "C", "G", "Am").
            - "content": The lyrics with chords inline inside square brackets, rendered precisely above or at the beginning of the word/syllable they apply to. 
              Example content style:
              "[Am]On a dark desert highway, [E7]cool wind in my hair
              [G]Warm smell of colitas, [D]rising up through the air"

            Chords Formatting Rules:
            1. Ensure guitar chords are highly accurate, standard musician notations (e.g. C, G, Am, F, C#m, Bb, Dmaj7).
            2. Match lyrics perfectly.
            3. Do not truncate. Provide the full complete song, verses, chorus, bridge, and outro.
            4. If the page is from other sites or scraping contains noise, filter it out completely and deliver only clean lyrics and chords.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an expert musicologist, guitarist, and professional chord sheet editor specializing in Ultimate Guitar transcribing."))
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        
        return if (!textResponse.isNullOrBlank()) {
            try {
                RetrofitClient.songAdapter.fromJson(textResponse)
            } catch (e: Exception) {
                // Look for first '{' and last '}' in case of extra text wrappers
                val trimmed = textResponse.trim()
                val startIndex = trimmed.indexOf('{')
                val endIndex = trimmed.lastIndexOf('}')
                if (startIndex in 0 until endIndex) {
                    val cleanedJson = trimmed.substring(startIndex, endIndex + 1)
                    RetrofitClient.songAdapter.fromJson(cleanedJson)
                } else {
                    val cleanedText = trimmed.removeSurrounding("```json", "```").trim()
                    RetrofitClient.songAdapter.fromJson(cleanedText)
                }
            }
        } else {
            null
        }
    }
}
