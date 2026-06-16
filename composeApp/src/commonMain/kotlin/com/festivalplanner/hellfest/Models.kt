package com.festivalplanner.hellfest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val FESTIVAL_ID = "hellfest-2026"

@Serializable
data class FestivalSet(
    val id: String,
    val artist: String,
    val normalizedArtist: String = artist.lowercase(),
    val stage: String,
    val day: String,
    val start: String,
    val end: String,
    val source: String = "clashfinder",
    val sourceUrl: String = "https://clashfinder.com/s/rohellfest/",
)

@Serializable
data class UserPlan(
    val userName: String,
    val festival: String = FESTIVAL_ID,
    val exportedAt: String,
    val selectedSetIds: Set<String>,
)

data class FriendComparison(
    val both: List<FestivalSet>,
    val onlyMine: List<FestivalSet>,
    val onlyFriend: List<FestivalSet>,
    val crossPlanClashes: List<Pair<FestivalSet, FestivalSet>>,
)

@Serializable
data class SpotifyMatches(
    val followedSetIds: Set<String> = emptySet(),
    val topSetIds: Set<String> = emptySet(),
    val librarySetIds: Set<String> = emptySet(),
    val topRankBySetId: Map<String, Int> = emptyMap(),
    val syncedAt: String = "",
) {
    val allSetIds: Set<String>
        get() = followedSetIds + topSetIds + librarySetIds
}

data class SpotifySyncResult(
    val matches: SpotifyMatches,
    val followedArtists: Int,
    val topArtists: Int,
    val libraryArtists: Int,
)

@Serializable
data class SchedulePrefs(
    val viewMode: String = "list",
    val selectedDay: String? = null,
    val selectedStage: String? = null,
    val zoom: Float = 0.7f,
)

data class SpotifyPlaylistResult(
    val playlistName: String,
    val playlistUrl: String,
    val trackCount: Int,
    val missingArtists: List<String>,
)

object PlanCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(userName: String, selectedIds: Set<String>, exportedAt: String): String =
        json.encodeToString(
            UserPlan(
                userName = userName.ifBlank { "Hellfest friend" },
                exportedAt = exportedAt,
                selectedSetIds = selectedIds,
            )
        )

    fun import(text: String): Result<UserPlan> = runCatching {
        json.decodeFromString<UserPlan>(text.trim()).also {
            require(it.festival == FESTIVAL_ID) { "This plan is for ${it.festival}, not Hellfest 2026." }
        }
    }
}

fun FestivalSet.conflictsWith(other: FestivalSet): Boolean =
    id != other.id && start < other.end && other.start < end

fun conflictingIds(sets: List<FestivalSet>): Set<String> = buildSet {
    sets.forEachIndexed { index, first ->
        sets.drop(index + 1).forEach { second ->
            if (first.conflictsWith(second)) {
                add(first.id)
                add(second.id)
            }
        }
    }
}

fun comparePlans(
    allSets: List<FestivalSet>,
    mine: Set<String>,
    friend: Set<String>,
): FriendComparison {
    val byId = allSets.associateBy { it.id }
    val bothIds = mine intersect friend
    val mineSets = (mine - friend).mapNotNull(byId::get).sortedBy { it.start }
    val friendSets = (friend - mine).mapNotNull(byId::get).sortedBy { it.start }
    return FriendComparison(
        both = bothIds.mapNotNull(byId::get).sortedBy { it.start },
        onlyMine = mineSets,
        onlyFriend = friendSets,
        crossPlanClashes = mineSets.flatMap { mySet ->
            friendSets.filter(mySet::conflictsWith).map { mySet to it }
        },
    )
}

interface PlanStorage {
    fun loadSelectedIds(): Set<String>
    fun saveSelectedIds(ids: Set<String>)
    fun loadMustSeeIds(): Set<String>
    fun saveMustSeeIds(ids: Set<String>)
    fun copyText(label: String, text: String)
    fun sharePlanImage(allSets: List<FestivalSet>, selectedIds: Set<String>, mustSeeIds: Set<String>)
    fun loadSchedulePrefs(): SchedulePrefs
    fun saveSchedulePrefs(prefs: SchedulePrefs)
    fun loadSpotifyClientId(): String
    fun saveSpotifyClientId(clientId: String)
    fun hasBundledSpotifyClientId(): Boolean
    fun hasSpotifyToken(): Boolean
    fun startSpotifyLogin(clientId: String)
    fun disconnectSpotify()
    fun loadSpotifyMatches(): SpotifyMatches
    fun syncSpotifyMatches(
        allSets: List<FestivalSet>,
        onResult: (Result<SpotifySyncResult>) -> Unit,
    )
    fun createSpotifyPlaylist(
        playlistName: String,
        sets: List<FestivalSet>,
        onResult: (Result<SpotifyPlaylistResult>) -> Unit,
    )
}

expect fun currentFestivalTime(): String
expect fun buildDate(): String
