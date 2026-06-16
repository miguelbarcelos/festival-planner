package com.festivalplanner.hellfest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var storage: AndroidPlanStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = AndroidPlanStorage(this)
        storage.handleSpotifyRedirect(intent)
        setContent {
            FestivalPlannerApp(storage)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        storage.handleSpotifyRedirect(intent)
    }
}

class AndroidPlanStorage(private val context: Context) : PlanStorage {
    private val preferences = context.getSharedPreferences("hellfest_plan", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun loadSelectedIds(): Set<String> =
        preferences.getStringSet("selected_ids", emptySet()).orEmpty().toSet()

    override fun saveSelectedIds(ids: Set<String>) {
        preferences.edit().putStringSet("selected_ids", ids).apply()
    }

    override fun loadMustSeeIds(): Set<String> =
        preferences.getStringSet("must_see_ids", emptySet()).orEmpty().toSet()

    override fun saveMustSeeIds(ids: Set<String>) {
        preferences.edit().putStringSet("must_see_ids", ids).apply()
    }

    override fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun sharePlanImage(allSets: List<FestivalSet>, selectedIds: Set<String>, mustSeeIds: Set<String>) {
        val schedule = allSets.sortedBy { it.start }
        if (schedule.isEmpty()) {
            Toast.makeText(context, "Sem horário para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val outputDir = File(context.cacheDir, "shared")
        outputDir.mkdirs()
        outputDir.listFiles { file -> file.extension == "png" }?.forEach { it.delete() }
        val uris = PlanImageRenderer.renderByDay(schedule, selectedIds, mustSeeIds).map { (day, bitmap) ->
            val output = File(outputDir, "hellfest-2026-plan-$day.png")
            FileOutputStream(output).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
        }

        val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, "O meu horário Hellfest 2026")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Partilhar horário"))
    }

    override fun loadSchedulePrefs(): SchedulePrefs =
        preferences.getString("schedule_prefs", null)?.let {
            runCatching { json.decodeFromString<SchedulePrefs>(it) }.getOrNull()
        } ?: SchedulePrefs()

    override fun saveSchedulePrefs(prefs: SchedulePrefs) {
        preferences.edit().putString("schedule_prefs", json.encodeToString(prefs)).apply()
    }

    override fun loadSpotifyClientId(): String =
        preferences.getString("spotify_client_id", "").orEmpty()

    override fun saveSpotifyClientId(clientId: String) {
        preferences.edit().putString("spotify_client_id", clientId.trim()).apply()
    }

    override fun hasSpotifyToken(): Boolean =
        preferences.getString("spotify_access_token", null) != null ||
            preferences.getString("spotify_refresh_token", null) != null

    override fun startSpotifyLogin(clientId: String) {
        val cleanClientId = clientId.trim()
        if (cleanClientId.isBlank()) {
            Toast.makeText(context, "Falta o Spotify client_id.", Toast.LENGTH_SHORT).show()
            return
        }
        saveSpotifyClientId(cleanClientId)
        val verifier = randomPkceVerifier()
        preferences.edit().putString("spotify_code_verifier", verifier).apply()
        val challenge = pkceChallenge(verifier)
        val uri = Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .path("/authorize")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", cleanClientId)
            .appendQueryParameter("scope", "user-follow-read user-top-read user-library-read playlist-modify-private user-read-private")
            .appendQueryParameter("redirect_uri", SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun handleSpotifyRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "hellfestplanner" || data.host != "spotify-auth") return
        val error = data.getQueryParameter("error")
        if (error != null) {
            Toast.makeText(context, "Spotify: $error", Toast.LENGTH_LONG).show()
            return
        }
        val code = data.getQueryParameter("code") ?: return
        val verifier = preferences.getString("spotify_code_verifier", null) ?: return
        val clientId = loadSpotifyClientId()
        Thread {
            runCatching {
                val response = postForm(
                    "https://accounts.spotify.com/api/token",
                    mapOf(
                        "client_id" to clientId,
                        "grant_type" to "authorization_code",
                        "code" to code,
                        "redirect_uri" to SPOTIFY_REDIRECT_URI,
                        "code_verifier" to verifier,
                    ),
                    accessToken = null,
                )
                saveTokenResponse(JSONObject(response))
            }.onSuccess {
                mainHandler.post {
                    Toast.makeText(context, "Spotify ligado. Agora toca em Sync.", Toast.LENGTH_LONG).show()
                }
            }.onFailure {
                mainHandler.post {
                    Toast.makeText(context, "Erro Spotify: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun disconnectSpotify() {
        preferences.edit()
            .remove("spotify_access_token")
            .remove("spotify_refresh_token")
            .remove("spotify_expires_at")
            .remove("spotify_code_verifier")
            .remove("spotify_matches")
            .apply()
    }

    override fun loadSpotifyMatches(): SpotifyMatches =
        preferences.getString("spotify_matches", null)?.let {
            runCatching { json.decodeFromString<SpotifyMatches>(it) }.getOrNull()
        } ?: SpotifyMatches()

    override fun syncSpotifyMatches(
        allSets: List<FestivalSet>,
        onResult: (Result<SpotifySyncResult>) -> Unit,
    ) {
        Thread {
            val result = runCatching {
                val token = validSpotifyToken()
                val followed = fetchFollowedArtists(token)
                val top = fetchTopArtists(token)
                val library = fetchSavedTrackArtists(token)
                val followedSetIds = matchArtistNames(allSets, followed.map { it.name }).toSet()
                val topRankBySetId = matchTopArtists(allSets, top)
                val librarySetIds = matchArtistNames(allSets, library.map { it.name }).toSet()
                val matches = SpotifyMatches(
                    followedSetIds = followedSetIds,
                    topSetIds = topRankBySetId.keys,
                    librarySetIds = librarySetIds,
                    topRankBySetId = topRankBySetId,
                    syncedAt = OffsetDateTime.now(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
                preferences.edit().putString("spotify_matches", json.encodeToString(matches)).apply()
                SpotifySyncResult(matches, followed.size, top.size, library.size)
            }
            mainHandler.post { onResult(result) }
        }.start()
    }

    override fun createSpotifyPlaylist(
        playlistName: String,
        sets: List<FestivalSet>,
        onResult: (Result<SpotifyPlaylistResult>) -> Unit,
    ) {
        Thread {
            val result = runCatching {
                if (sets.isEmpty()) throw IllegalStateException("Não há bandas para criar playlist.")
                val token = validSpotifyToken()
                val playlist = createPrivatePlaylist(token, playlistName)
                val trackUris = mutableListOf<String>()
                val missing = mutableListOf<String>()
                sets.distinctBy { spotifyNormalize(it.artist) }.forEach { set ->
                    val uri = searchTrackForArtist(token, set.artist)
                    if (uri == null) missing += set.artist else trackUris += uri
                }
                trackUris.chunked(100).forEach { addItemsToPlaylist(token, playlist.id, it) }
                SpotifyPlaylistResult(
                    playlistName = playlistName,
                    playlistUrl = playlist.url,
                    trackCount = trackUris.size,
                    missingArtists = missing,
                )
            }
            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun validSpotifyToken(): String {
        val access = preferences.getString("spotify_access_token", null)
        val expiresAt = preferences.getLong("spotify_expires_at", 0L)
        if (access != null && System.currentTimeMillis() < expiresAt - 60_000L) return access

        val refresh = preferences.getString("spotify_refresh_token", null)
            ?: throw IllegalStateException("Liga o Spotify primeiro.")
        val response = postForm(
            "https://accounts.spotify.com/api/token",
            mapOf(
                "client_id" to loadSpotifyClientId(),
                "grant_type" to "refresh_token",
                "refresh_token" to refresh,
            ),
            accessToken = null,
        )
        saveTokenResponse(JSONObject(response))
        return preferences.getString("spotify_access_token", null)
            ?: throw IllegalStateException("Não recebi token do Spotify.")
    }

    private fun saveTokenResponse(response: JSONObject) {
        val expiresIn = response.optLong("expires_in", 3600L)
        val editor = preferences.edit()
            .putString("spotify_access_token", response.getString("access_token"))
            .putLong("spotify_expires_at", System.currentTimeMillis() + expiresIn * 1000L)
        response.optString("refresh_token").takeIf { it.isNotBlank() }?.let {
            editor.putString("spotify_refresh_token", it)
        }
        editor.apply()
    }

    private fun fetchFollowedArtists(token: String): List<SpotifyArtist> {
        val artists = mutableListOf<SpotifyArtist>()
        var after: String? = null
        while (true) {
            val url = buildString {
                append("https://api.spotify.com/v1/me/following?type=artist&limit=50")
                if (after != null) append("&after=").append(urlEncode(after!!))
            }
            val root = JSONObject(get(url, token)).getJSONObject("artists")
            val items = root.getJSONArray("items")
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                artists += SpotifyArtist(item.getString("name"), 0)
            }
            after = root.optJSONObject("cursors")?.optString("after")?.takeIf { it.isNotBlank() }
            if (rootIsDone(rootNext = root.optString("next").takeIf { it.isNotBlank() }, after = after)) break
        }
        return artists
    }

    private fun fetchTopArtists(token: String): List<SpotifyArtist> {
        val result = linkedMapOf<String, SpotifyArtist>()
        listOf("short_term", "medium_term", "long_term").forEach { range ->
            listOf(0, 50, 100).forEach { offset ->
                val root = JSONObject(get("https://api.spotify.com/v1/me/top/artists?time_range=$range&limit=50&offset=$offset", token))
                val items = root.getJSONArray("items")
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    result.putIfAbsent(item.getString("name"), SpotifyArtist(item.getString("name"), result.size + 1))
                }
                if (items.length() < 50) return@forEach
            }
        }
        return result.values.toList()
    }

    private fun fetchSavedTrackArtists(token: String): List<SpotifyArtist> {
        val result = linkedMapOf<String, SpotifyArtist>()
        var next: String? = "https://api.spotify.com/v1/me/tracks?limit=50"
        var pages = 0
        while (next != null && pages < 40) {
            val root = JSONObject(get(next, token))
            val items = root.getJSONArray("items")
            for (index in 0 until items.length()) {
                val track = items.getJSONObject(index).getJSONObject("track")
                val artists = track.getJSONArray("artists")
                for (artistIndex in 0 until artists.length()) {
                    val name = artists.getJSONObject(artistIndex).getString("name")
                    result.putIfAbsent(name, SpotifyArtist(name, result.size + 1))
                }
            }
            next = root.optString("next").takeIf { it.isNotBlank() && it != "null" }
            pages++
        }
        return result.values.toList()
    }

    private fun createPrivatePlaylist(token: String, name: String): CreatedPlaylist {
        val body = JSONObject()
            .put("name", name)
            .put("public", false)
            .put("description", "Gerada pelo Hellfest Planner 2026")
            .toString()
        val root = JSONObject(postJson("https://api.spotify.com/v1/me/playlists", body, token))
        return CreatedPlaylist(
            id = root.getString("id"),
            url = root.getJSONObject("external_urls").getString("spotify"),
        )
    }

    private fun searchTrackForArtist(token: String, artist: String): String? {
        val exactQuery = "artist:\"$artist\""
        val exact = searchTrack(token, exactQuery)
        if (exact != null) return exact
        return searchTrack(token, artist)
    }

    private fun searchTrack(token: String, query: String): String? {
        val root = JSONObject(get("https://api.spotify.com/v1/search?type=track&limit=1&q=${urlEncode(query)}", token))
        val items = root.getJSONObject("tracks").getJSONArray("items")
        return if (items.length() == 0) null else items.getJSONObject(0).getString("uri")
    }

    private fun addItemsToPlaylist(token: String, playlistId: String, uris: List<String>) {
        val array = org.json.JSONArray()
        uris.forEach { array.put(it) }
        val body = JSONObject().put("uris", array).toString()
        postJson("https://api.spotify.com/v1/playlists/$playlistId/items", body, token)
    }

    private fun matchArtistNames(allSets: List<FestivalSet>, names: List<String>): List<String> {
        val normalizedNames = names.map(::spotifyNormalize).toSet()
        return allSets.filter { spotifyNormalize(it.artist) in normalizedNames }.map { it.id }
    }

    private fun matchTopArtists(allSets: List<FestivalSet>, topArtists: List<SpotifyArtist>): Map<String, Int> {
        val rankByName = topArtists.associate { spotifyNormalize(it.name) to it.rank }
        return allSets.mapNotNull { set ->
            rankByName[spotifyNormalize(set.artist)]?.let { rank -> set.id to rank }
        }.toMap()
    }

    private fun get(url: String, accessToken: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        return readResponse(connection)
    }

    private fun postForm(url: String, values: Map<String, String>, accessToken: String?): String {
        val body = values.entries.joinToString("&") { "${urlEncode(it.key)}=${urlEncode(it.value)}" }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        connection.outputStream.use { it.write(bytes) }
        return readResponse(connection)
    }

    private fun postJson(url: String, body: String, accessToken: String): String {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.outputStream.use { it.write(bytes) }
        return readResponse(connection)
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("Spotify HTTP $code: $body")
        return body
    }

    private fun rootIsDone(rootNext: String?, after: String?): Boolean =
        rootNext.isNullOrBlank() || rootNext == "null" || after.isNullOrBlank()

    private fun randomPkceVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun pkceChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun spotifyNormalize(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("&", " and ")
            .replace("^the\\s+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class SpotifyArtist(val name: String, val rank: Int)

    private data class CreatedPlaylist(val id: String, val url: String)

    private companion object {
        const val SPOTIFY_REDIRECT_URI = "hellfestplanner://spotify-auth"
    }
}

actual fun currentFestivalTime(): String =
    OffsetDateTime.now(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

actual fun buildDate(): String = "2026-06-15"

private object PlanImageRenderer {
    private val stages = listOf("MAINSTAGE 1", "MAINSTAGE 2", "WARZONE", "VALLEY", "ALTAR", "TEMPLE")
    private const val width = 1600
    private const val margin = 44f
    private const val titleHeight = 120f
    private const val dayTitleHeight = 74f
    private const val headerHeight = 62f
    private const val timeWidth = 112f
    private const val hourHeight = 118f
    private const val gap = 28f

    fun renderByDay(
        schedule: List<FestivalSet>,
        selectedIds: Set<String>,
        mustSeeIds: Set<String>,
    ): List<Pair<String, Bitmap>> =
        schedule.groupBy { it.day }.toSortedMap().map { (day, sets) ->
            day to renderDay(day, sets, selectedIds, mustSeeIds)
        }

    private fun renderDay(
        day: String,
        sets: List<FestivalSet>,
        selectedIds: Set<String>,
        mustSeeIds: Set<String>,
    ): Bitmap {
        val start = ((sets.minOf(::festivalStartMinute) / 60) * 60).coerceAtMost(10 * 60)
        val end = ceil(sets.maxOf(::festivalEndMinute) / 60f).toInt() * 60
        val sections = listOf(ImageSection(day, sets, start, end))
        val totalHeight = (
            titleHeight +
                sections.sumOf { section ->
                    (dayTitleHeight + headerHeight + ((section.endMinute - section.startMinute) / 60f) * hourHeight + gap).toInt()
                } +
                margin
            ).toInt().coerceAtLeast(500)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(21, 17, 15))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawTitle(canvas, paint, sets.size, selectedIds.count { id -> sets.any { it.id == id } }, mustSeeIds.count { id -> sets.any { it.id == id } })

        var y = titleHeight
        sections.forEach { section ->
            y = drawSection(canvas, paint, section, y, selectedIds, mustSeeIds)
        }
        return bitmap
    }

    private fun drawTitle(canvas: Canvas, paint: Paint, totalCount: Int, selectedCount: Int, mustSeeCount: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 106, 0)
        paint.textSize = 44f
        paint.isFakeBoldText = true
        canvas.drawText("HELLFEST 2026", margin, 58f, paint)
        paint.color = Color.rgb(255, 176, 0)
        paint.textSize = 28f
        paint.isFakeBoldText = false
        canvas.drawText("$totalCount bandas · $selectedCount selecionadas · $mustSeeCount imperdíveis", margin, 98f, paint)
    }

    private fun drawSection(
        canvas: Canvas,
        paint: Paint,
        section: ImageSection,
        top: Float,
        selectedIds: Set<String>,
        mustSeeIds: Set<String>,
    ): Float {
        val gridLeft = margin
        val gridTop = top + dayTitleHeight
        val stageWidth = (width - margin * 2 - timeWidth) / stages.size
        val sectionHeight = headerHeight + ((section.endMinute - section.startMinute) / 60f) * hourHeight

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 106, 0)
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText(dayLabelPt(section.day), margin, top + 44f, paint)

        paint.color = Color.rgb(36, 30, 27)
        canvas.drawRoundRect(RectF(gridLeft, gridTop, width - margin, gridTop + sectionHeight), 18f, 18f, paint)

        paint.color = Color.rgb(53, 44, 39)
        canvas.drawRect(gridLeft, gridTop, width - margin, gridTop + headerHeight, paint)
        stages.forEachIndexed { index, stage ->
            val left = gridLeft + timeWidth + stageWidth * index
            paint.color = Color.rgb(248, 240, 232)
            paint.textSize = 18f
            paint.isFakeBoldText = true
            drawCenteredText(canvas, paint, stage, left + stageWidth / 2f, gridTop + 38f, stageWidth - 10f)
        }

        for (minute in section.startMinute..section.endMinute step 30) {
            val y = gridTop + headerHeight + ((minute - section.startMinute) / 60f) * hourHeight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (minute % 60 == 0) 2f else 1f
            paint.color = if (minute % 60 == 0) Color.rgb(75, 66, 60) else Color.rgb(45, 38, 34)
            canvas.drawLine(gridLeft, y, width - margin, y, paint)
            paint.style = Paint.Style.FILL
            if (minute % 60 == 0) {
                paint.color = Color.rgb(255, 176, 0)
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText(minuteLabel(minute), gridLeft + 16f, y + 7f, paint)
            }
        }

        section.sets.forEach { set ->
            val stageIndex = stages.indexOf(set.stage)
            if (stageIndex >= 0) {
                val start = festivalStartMinute(set)
                val duration = max(15, festivalEndMinute(set) - start)
                val left = gridLeft + timeWidth + stageWidth * stageIndex + 5f
                val right = left + stageWidth - 10f
                val y = gridTop + headerHeight + ((start - section.startMinute) / 60f) * hourHeight + 4f
                val bottom = y + (duration / 60f) * hourHeight - 8f
                val mustSee = set.id in mustSeeIds
                val selected = set.id in selectedIds
                paint.style = Paint.Style.FILL
                paint.color = when {
                    mustSee -> Color.rgb(0, 220, 180)
                    selected -> Color.rgb(255, 106, 0)
                    else -> Color.rgb(70, 58, 52)
                }
                canvas.drawRoundRect(RectF(left, y, right, bottom), 10f, 10f, paint)
                paint.color = if (selected || mustSee) Color.rgb(0, 0, 0) else Color.rgb(245, 238, 230)
                paint.textSize = 20f
                paint.isFakeBoldText = true
                drawMultiline(canvas, paint, set.artist, left + 10f, y + 26f, right - left - 20f, bottom - y - 34f)
                paint.textSize = 16f
                paint.isFakeBoldText = false
                canvas.drawText("${set.start.substring(11, 16)}-${set.end.substring(11, 16)}", left + 10f, bottom - 12f, paint)
            }
        }

        return gridTop + sectionHeight + gap
    }

    private fun drawCenteredText(canvas: Canvas, paint: Paint, text: String, centerX: Float, baseline: Float, maxWidth: Float) {
        val trimmed = ellipsize(text, paint, maxWidth)
        canvas.drawText(trimmed, centerX - paint.measureText(trimmed) / 2f, baseline, paint)
    }

    private fun drawMultiline(canvas: Canvas, paint: Paint, text: String, x: Float, y: Float, maxWidth: Float, maxHeight: Float) {
        val lineHeight = paint.textSize * 1.15f
        val maxLines = max(1, (maxHeight / lineHeight).toInt())
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotBlank()) lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        lines.take(maxLines).forEachIndexed { index, line ->
            val value = if (index == maxLines - 1 && lines.size > maxLines) ellipsize(line, paint, maxWidth) else line
            canvas.drawText(value, x, y + lineHeight * index, paint)
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var value = text
        while (value.length > 1 && paint.measureText("$value...") > maxWidth) {
            value = value.dropLast(1)
        }
        return "$value..."
    }

    private fun festivalStartMinute(set: FestivalSet): Int = festivalMinute(set.start, set.day)

    private fun festivalEndMinute(set: FestivalSet): Int = festivalMinute(set.end, set.day)

    private fun festivalMinute(iso: String, festivalDay: String): Int {
        val hour = iso.substring(11, 13).toInt()
        val minute = iso.substring(14, 16).toInt()
        val dayOffset = if (iso.take(10) > festivalDay) 24 * 60 else 0
        return dayOffset + hour * 60 + minute
    }

    private fun minuteLabel(minute: Int): String {
        val normalized = minute % (24 * 60)
        return "${(normalized / 60).toString().padStart(2, '0')}:00"
    }

    private fun dayLabelPt(day: String): String = when (day) {
        "2026-06-18" -> "Quinta 18 Jun"
        "2026-06-19" -> "Sexta 19 Jun"
        "2026-06-20" -> "Sabado 20 Jun"
        "2026-06-21" -> "Domingo 21 Jun"
        else -> day
    }

    private data class ImageSection(
        val day: String,
        val sets: List<FestivalSet>,
        val startMinute: Int,
        val endMinute: Int,
    )
}
