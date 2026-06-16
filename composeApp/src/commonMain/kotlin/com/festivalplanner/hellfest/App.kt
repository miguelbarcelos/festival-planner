package com.festivalplanner.hellfest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import festivalplanner.composeapp.generated.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val HellBlack = Color(0xFF15110F)
private val HellSurface = Color(0xFF241E1B)
private val HellOrange = Color(0xFFFF6A00)
private val HellAmber = Color(0xFFFFB000)
private val MustSeeMint = Color(0xFF00DCB4)
private val ClashRed = Color(0xFFFF4D4D)
private const val MinGridZoom = 0.35f
private const val MaxGridZoom = 1.60f

private val HellfestColors = darkColorScheme(
    primary = HellOrange,
    secondary = HellAmber,
    background = HellBlack,
    surface = HellSurface,
    surfaceVariant = Color(0xFF352C27),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF8F0E8),
    onSurface = Color(0xFFF8F0E8),
)

private enum class Screen(val icon: ImageVector) {
    Schedule(Icons.Default.List),
    Plan(Icons.Default.Favorite),
    Friends(Icons.Default.Groups),
    Settings(Icons.Default.Settings),
}

private enum class ScheduleView {
    List,
    Grid,
}

private data class UiStrings(
    val schedule: String,
    val myPlan: String,
    val friends: String,
    val settings: String,
    val loadScheduleError: String,
    val loading: String,
    val allDays: String,
    val allStages: String,
    val searchArtist: String,
    val clearSearch: String,
    val listView: String,
    val timetableView: String,
    val fullscreen: String,
    val zoomOut: String,
    val zoomIn: String,
    val noSetsMatch: String,
    val sets: String,
    val selected: String,
    val mustSee: String,
    val clashes: String,
    val now: String,
    val next: String,
    val noSetNow: String,
    val planComplete: String,
    val today: String,
    val shareSelectedSchedule: String,
    val emptyPlan: String,
    val nothingSelectedToday: String,
    val exportMyPlan: String,
    val yourName: String,
    val copied: String,
    val copyPlan: String,
    val shareScheduleImage: String,
    val compactJsonPreview: String,
    val importFriend: String,
    val pasteExportedPlan: String,
    val unreadablePlan: String,
    val comparePlans: String,
    val bothSelected: String,
    val onlyMe: String,
    val onlyFriendPrefix: String,
    val crossPlanClashes: String,
    val comparedWith: String,
    val none: String,
    val appStatus: String,
    val bundledSets: String,
    val selectedSets: String,
    val version: String,
    val build: String,
    val timezone: String,
    val reimportBundledJson: String,
    val spotifyMatch: String,
    val spotifyConfigured: String,
    val spotifyClientId: String,
    val spotifyClientIdOptional: String,
    val spotifyConnected: String,
    val spotifyDisconnected: String,
    val openingSpotifyLogin: String,
    val syncingSpotify: String,
    val spotifySyncError: String,
    val disconnectSpotify: String,
    val makePlannerFromSpotify: String,
    val creatingPlanPlaylist: String,
    val creatingNotPicksPlaylist: String,
    val playlistPlan: String,
    val playlistNotPicks: String,
    val spotifyDashboardHint: String,
    val clearAllSelections: String,
    val scheduleSourceHint: String,
    val clearPlanTitle: String,
    val clearPlanText: String,
    val clear: String,
    val cancel: String,
    val clash: String,
    val removeMustSee: String,
    val markMustSee: String,
    val removeFromPlan: String,
    val addToPlan: String,
    val retry: String,
    val followed: String,
    val saved: String,
    val library: String,
    val bands: String,
    val top: String,
    val playlistCreatedSuffix: String,
    val playlistCreateError: String,
    val about: String,
    val developedBy: String,
    val vibeCoding: String,
    val github: String,
) {
    fun screenLabel(screen: Screen): String = when (screen) {
        Screen.Schedule -> schedule
        Screen.Plan -> myPlan
        Screen.Friends -> friends
        Screen.Settings -> settings
    }

    fun counts(setsCount: Int, selectedCount: Int, mustSeeCount: Int): String =
        "$setsCount $sets · $selectedCount $selected · $mustSeeCount $mustSee"

    fun planCounts(selectedCount: Int, mustSeeCount: Int, clashesCount: Int): String =
        "$selectedCount $selected · $mustSeeCount $mustSee · $clashesCount $clashes"

    fun spotifyFound(total: Int, followedArtists: Int, topArtists: Int, libraryArtists: Int): String =
        if (this === PtStrings) {
            "Encontradas $total bandas: $followedArtists seguidas, $topArtists top, $libraryArtists na biblioteca."
        } else {
            "Found $total bands: $followedArtists followed, $topArtists top, $libraryArtists in your library."
        }

    fun spotifyStats(followedCount: Int, topCount: Int, libraryCount: Int, syncedAt: String): String =
        "$followedCount $followed · $topCount $top · $libraryCount $library · $syncedAt"

    fun playlistCreated(result: SpotifyPlaylistResult): String =
        if (this === PtStrings) {
            "Playlist '${result.playlistName}' criada com ${result.trackCount} músicas. Faltaram ${result.missingArtists.size} artistas."
        } else {
            "Playlist '${result.playlistName}' created with ${result.trackCount} tracks. Missing ${result.missingArtists.size} artists."
        }
}

private val EnStrings = UiStrings(
    schedule = "Schedule",
    myPlan = "My Plan",
    friends = "Friends",
    settings = "Settings",
    loadScheduleError = "Could not load the bundled schedule.",
    loading = "Loading the pit...",
    allDays = "All days",
    allStages = "All stages",
    searchArtist = "Search artist",
    clearSearch = "Clear search",
    listView = "List view",
    timetableView = "Timetable view",
    fullscreen = "Fullscreen",
    zoomOut = "Zoom out",
    zoomIn = "Zoom in",
    noSetsMatch = "No sets match those filters.",
    sets = "sets",
    selected = "selected",
    mustSee = "must-see",
    clashes = "clashes",
    now = "NOW",
    next = "NEXT",
    noSetNow = "No set now",
    planComplete = "Plan complete",
    today = "Today",
    shareSelectedSchedule = "Share selected schedule",
    emptyPlan = "Your plan is empty.\nTap hearts in Schedule to build it.",
    nothingSelectedToday = "Nothing selected for today.",
    exportMyPlan = "EXPORT MY PLAN",
    yourName = "Your name",
    copied = "Copied",
    copyPlan = "Copy plan",
    shareScheduleImage = "Share schedule image",
    compactJsonPreview = "Compact JSON preview",
    importFriend = "IMPORT A FRIEND",
    pasteExportedPlan = "Paste their exported plan",
    unreadablePlan = "That plan could not be read.",
    comparePlans = "Compare plans",
    bothSelected = "BOTH SELECTED",
    onlyMe = "ONLY ME",
    onlyFriendPrefix = "ONLY",
    crossPlanClashes = "CROSS-PLAN CLASHES",
    comparedWith = "COMPARED WITH",
    none = "None",
    appStatus = "APP STATUS",
    bundledSets = "Bundled sets",
    selectedSets = "Selected sets",
    version = "Version",
    build = "Build",
    timezone = "Timezone",
    reimportBundledJson = "Reimport bundled JSON",
    spotifyMatch = "SPOTIFY MATCH",
    spotifyConfigured = "Spotify client_id configured. Just connect your account.",
    spotifyClientId = "Spotify client_id",
    spotifyClientIdOptional = "Optional if the APK is already configured.",
    spotifyConnected = "Spotify connected. You can sync now.",
    spotifyDisconnected = "Spotify disconnected.",
    openingSpotifyLogin = "Opening Spotify login...",
    syncingSpotify = "Syncing Spotify...",
    spotifySyncError = "Error syncing Spotify.",
    disconnectSpotify = "Disconnect Spotify",
    makePlannerFromSpotify = "Make my planner from Spotify",
    creatingPlanPlaylist = "Creating your plan playlist...",
    creatingNotPicksPlaylist = "Creating playlist for what was left out...",
    playlistPlan = "Playlist plan",
    playlistNotPicks = "Playlist not picks",
    spotifyDashboardHint = "In the Spotify Dashboard use redirect URI: hellfestplanner://spotify-auth. For library access, reconnect once to approve user-library-read.",
    clearAllSelections = "Clear all selections",
    scheduleSourceHint = "Schedule source: Clashfinder. Times and lineup can change; regenerate the bundled JSON before the festival.",
    clearPlanTitle = "Clear your plan?",
    clearPlanText = "This removes every selected set from this device.",
    clear = "Clear",
    cancel = "Cancel",
    clash = "Clash",
    removeMustSee = "Remove must-see",
    markMustSee = "Mark must-see",
    removeFromPlan = "Remove from plan",
    addToPlan = "Add to plan",
    retry = "Retry",
    followed = "followed",
    saved = "saved",
    library = "library",
    bands = "bands",
    top = "top",
    playlistCreatedSuffix = "",
    playlistCreateError = "Error creating Spotify playlist.",
    about = "ABOUT",
    developedBy = "Developed by Miguel Barcelos",
    vibeCoding = "Vibe coding, 2026",
    github = "GitHub: https://github.com/miguelbarcelos",
)

private val PtStrings = EnStrings.copy(
    schedule = "Horario",
    myPlan = "O meu plano",
    friends = "Amigos",
    settings = "Definicoes",
    loadScheduleError = "Nao foi possivel carregar o horario incluido.",
    loading = "A carregar o pit...",
    allDays = "Todos os dias",
    allStages = "Todos os palcos",
    searchArtist = "Procurar banda",
    clearSearch = "Limpar pesquisa",
    listView = "Vista em lista",
    timetableView = "Vista em grelha",
    fullscreen = "Ecra completo",
    zoomOut = "Diminuir zoom",
    zoomIn = "Aumentar zoom",
    noSetsMatch = "Nenhum concerto corresponde aos filtros.",
    sets = "concertos",
    selected = "selecionados",
    mustSee = "imperdiveis",
    clashes = "conflitos",
    now = "AGORA",
    next = "A SEGUIR",
    noSetNow = "Sem concerto agora",
    planComplete = "Plano completo",
    today = "Hoje",
    shareSelectedSchedule = "Partilhar horario selecionado",
    emptyPlan = "O teu plano esta vazio.\nToca nos coracoes no Horario para o construir.",
    nothingSelectedToday = "Nada selecionado para hoje.",
    exportMyPlan = "EXPORTAR O MEU PLANO",
    yourName = "O teu nome",
    copied = "Copiado",
    copyPlan = "Copiar plano",
    shareScheduleImage = "Partilhar imagem do horario",
    compactJsonPreview = "Pre-visualizacao JSON compacta",
    importFriend = "IMPORTAR AMIGO",
    pasteExportedPlan = "Cola o plano exportado",
    unreadablePlan = "Nao foi possivel ler esse plano.",
    comparePlans = "Comparar planos",
    bothSelected = "AMBOS SELECIONARAM",
    onlyMe = "SO EU",
    onlyFriendPrefix = "SO",
    crossPlanClashes = "CONFLITOS ENTRE PLANOS",
    comparedWith = "COMPARADO COM",
    none = "Nenhum",
    appStatus = "ESTADO DA APP",
    bundledSets = "Concertos incluidos",
    selectedSets = "Concertos selecionados",
    version = "Versao",
    build = "Build",
    timezone = "Fuso horario",
    reimportBundledJson = "Reimportar JSON incluido",
    spotifyMatch = "SPOTIFY",
    spotifyConfigured = "Spotify client_id configurado. So tens de ligar a tua conta.",
    spotifyClientIdOptional = "Opcional se o APK ja vier configurado.",
    spotifyConnected = "Spotify ligado. Podes sincronizar.",
    openingSpotifyLogin = "A abrir login do Spotify...",
    syncingSpotify = "A sincronizar Spotify...",
    spotifySyncError = "Erro ao sincronizar Spotify.",
    disconnectSpotify = "Desligar Spotify",
    makePlannerFromSpotify = "Fazer o meu plano pelo Spotify",
    creatingPlanPlaylist = "A criar playlist do teu plano...",
    creatingNotPicksPlaylist = "A criar playlist do que ficou fora...",
    playlistPlan = "Playlist plano",
    playlistNotPicks = "Playlist nao escolhidos",
    spotifyDashboardHint = "No Spotify Dashboard usa redirect URI: hellfestplanner://spotify-auth. Para biblioteca, reconecta uma vez para aprovar user-library-read.",
    clearAllSelections = "Limpar todas as selecoes",
    scheduleSourceHint = "Fonte do horario: Clashfinder. Horas e alinhamento podem mudar; regenera o JSON incluido antes do festival.",
    clearPlanTitle = "Limpar o teu plano?",
    clearPlanText = "Isto remove todos os concertos selecionados deste dispositivo.",
    clear = "Limpar",
    cancel = "Cancelar",
    clash = "Conflito",
    removeMustSee = "Remover imperdivel",
    markMustSee = "Marcar imperdivel",
    removeFromPlan = "Remover do plano",
    addToPlan = "Adicionar ao plano",
    retry = "Tentar novamente",
    followed = "seguidas",
    saved = "guardadas",
    library = "biblioteca",
    bands = "bandas",
    playlistCreateError = "Erro ao criar playlist Spotify.",
    about = "SOBRE",
    developedBy = "Desenvolvido por Miguel Barcelos",
    vibeCoding = "Vibe coding, 2026",
    github = "GitHub: https://github.com/miguelbarcelos",
)

private val LocalStrings = staticCompositionLocalOf { EnStrings }

@Composable
private fun currentUiStrings(): UiStrings =
    if (Locale.current.language.lowercase() == "pt") PtStrings else EnStrings

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FestivalPlannerApp(storage: PlanStorage) {
    val strings = currentUiStrings()
    var allSets by remember { mutableStateOf<List<FestivalSet>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(storage.loadSelectedIds()) }
    var mustSeeIds by remember { mutableStateOf(storage.loadMustSeeIds()) }
    var screen by remember { mutableStateOf(Screen.Schedule) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var spotifyMatches by remember { mutableStateOf(storage.loadSpotifyMatches()) }
    var scheduleFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        runCatching {
            val content = Res.readBytes("files/hellfest_2026.json").decodeToString()
            Json { ignoreUnknownKeys = true }.decodeFromString<List<FestivalSet>>(content)
        }.onSuccess {
            allSets = it.sortedBy(FestivalSet::start)
            loadError = null
        }.onFailure {
            loadError = it.message ?: strings.loadScheduleError
        }
    }

    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (id !in selectedIds) {
            mustSeeIds = mustSeeIds - id
            storage.saveMustSeeIds(mustSeeIds)
        }
        storage.saveSelectedIds(selectedIds)
    }

    fun toggleMustSee(id: String) {
        mustSeeIds = if (id in mustSeeIds) mustSeeIds - id else mustSeeIds + id
        selectedIds = selectedIds + id
        storage.saveMustSeeIds(mustSeeIds)
        storage.saveSelectedIds(selectedIds)
    }

    fun cycleSelection(id: String) {
        when {
            id in mustSeeIds -> {
                mustSeeIds = mustSeeIds - id
                selectedIds = selectedIds - id
            }
            id in selectedIds -> {
                mustSeeIds = mustSeeIds + id
            }
            else -> {
                selectedIds = selectedIds + id
            }
        }
        storage.saveMustSeeIds(mustSeeIds)
        storage.saveSelectedIds(selectedIds)
    }

    CompositionLocalProvider(LocalStrings provides strings) {
    MaterialTheme(colorScheme = HellfestColors) {
        Scaffold(
            topBar = {
                if (!scheduleFullscreen) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HELLFEST 2026", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(strings.screenLabel(screen).uppercase(), style = MaterialTheme.typography.labelSmall, color = HellAmber)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!scheduleFullscreen) {
                    NavigationBar(containerColor = Color(0xFF1B1614)) {
                        Screen.entries.forEach { destination ->
                            val label = strings.screenLabel(destination)
                            NavigationBarItem(
                                selected = screen == destination,
                                onClick = { screen = destination },
                                icon = { Icon(destination.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = HellOrange,
                                    indicatorColor = HellOrange,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    loadError != null -> ErrorState(loadError!!) { reloadKey++ }
                    allSets.isEmpty() -> EmptyState(strings.loading)
                    screen == Screen.Schedule -> ScheduleScreen(
                        allSets = allSets,
                        selectedIds = selectedIds,
                        mustSeeIds = mustSeeIds,
                        spotifyMatches = spotifyMatches,
                        storage = storage,
                        fullscreen = scheduleFullscreen,
                        onFullscreenChange = { scheduleFullscreen = it },
                        onToggle = ::toggle,
                        onCycleSelection = ::cycleSelection,
                        onToggleMustSee = ::toggleMustSee,
                    )
                    screen == Screen.Plan -> PlanScreen(
                        allSets = allSets,
                        selectedIds = selectedIds,
                        mustSeeIds = mustSeeIds,
                        onToggle = ::toggle,
                        onToggleMustSee = ::toggleMustSee,
                        onShareImage = { storage.sharePlanImage(allSets, selectedIds, mustSeeIds) },
                    )
                    screen == Screen.Friends -> FriendsScreen(allSets, selectedIds, mustSeeIds, storage)
                    screen == Screen.Settings -> SettingsScreen(
                        allSets = allSets,
                        selectedIds = selectedIds,
                        totalSets = allSets.size,
                        selectedCount = selectedIds.size,
                        storage = storage,
                        spotifyMatches = spotifyMatches,
                        onSpotifyMatches = { spotifyMatches = it },
                        onAddSpotifyToPlan = {
                            selectedIds = selectedIds + spotifyMatches.allSetIds
                            storage.saveSelectedIds(selectedIds)
                        },
                        onClear = {
                            selectedIds = emptySet()
                            mustSeeIds = emptySet()
                            storage.saveSelectedIds(emptySet())
                            storage.saveMustSeeIds(emptySet())
                        },
                        onReload = { reloadKey++ },
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ScheduleScreen(
    allSets: List<FestivalSet>,
    selectedIds: Set<String>,
    mustSeeIds: Set<String>,
    spotifyMatches: SpotifyMatches,
    storage: PlanStorage,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onToggle: (String) -> Unit,
    onCycleSelection: (String) -> Unit,
    onToggleMustSee: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val pt = strings === PtStrings
    val dayOptions = listOf(
        null to strings.allDays,
        "2026-06-18" to if (pt) "Qui 18" else "Thu 18",
        "2026-06-19" to if (pt) "Sex 19" else "Fri 19",
        "2026-06-20" to if (pt) "Sab 20" else "Sat 20",
        "2026-06-21" to if (pt) "Dom 21" else "Sun 21",
    )
    val stages = listOf<String?>(null) + allSets.map { it.stage }.distinct()
    val savedPrefs = remember { storage.loadSchedulePrefs() }
    var selectedDay by remember { mutableStateOf(savedPrefs.selectedDay) }
    var selectedStage by remember { mutableStateOf(savedPrefs.selectedStage) }
    var search by remember { mutableStateOf("") }
    var scheduleView by remember { mutableStateOf(if (savedPrefs.viewMode == "grid") ScheduleView.Grid else ScheduleView.List) }
    var gridZoom by remember { mutableStateOf(savedPrefs.zoom.coerceIn(MinGridZoom, MaxGridZoom)) }
    var spotifyOnly by remember { mutableStateOf(false) }
    val effectiveDay = if (scheduleView == ScheduleView.Grid) selectedDay ?: "2026-06-18" else selectedDay

    LaunchedEffect(scheduleView, selectedDay, selectedStage, gridZoom) {
        storage.saveSchedulePrefs(
            SchedulePrefs(
                viewMode = if (scheduleView == ScheduleView.Grid) "grid" else "list",
                selectedDay = selectedDay,
                selectedStage = selectedStage,
                zoom = gridZoom,
            )
        )
    }

    val filtered = allSets.filter { set ->
        (effectiveDay == null || set.day == effectiveDay) &&
            (selectedStage == null || set.stage == selectedStage) &&
            (!spotifyOnly || set.id in spotifyMatches.allSetIds) &&
            (search.isBlank() || set.normalizedArtist.contains(search.trim(), ignoreCase = true) ||
                set.artist.contains(search.trim(), ignoreCase = true))
    }

    val controls: @Composable () -> Unit = {
        if (!fullscreen) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text(strings.searchArtist) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, strings.clearSearch) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            ChipRow {
                dayOptions.forEach { (day, label) ->
                    FilterChip(
                        selected = if (scheduleView == ScheduleView.Grid) effectiveDay == day else selectedDay == day,
                        onClick = { selectedDay = if (scheduleView == ScheduleView.Grid && day == null) "2026-06-18" else day },
                        label = { Text(label) },
                    )
                }
            }
            ChipRow {
                FilterChip(
                    selected = spotifyOnly,
                    onClick = { spotifyOnly = !spotifyOnly },
                    label = { Text("Spotify") },
                    enabled = spotifyMatches.allSetIds.isNotEmpty(),
                )
                stages.forEach { stage ->
                    FilterChip(
                        selected = selectedStage == stage,
                        onClick = { selectedStage = stage },
                        label = { Text(stage ?: strings.allStages) },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                strings.counts(filtered.size, selectedIds.size, mustSeeIds.size),
                style = MaterialTheme.typography.labelLarge,
                color = HellAmber,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ViewButton(
                    selected = scheduleView == ScheduleView.List,
                    icon = Icons.Default.List,
                    description = strings.listView,
                ) { scheduleView = ScheduleView.List }
                ViewButton(
                    selected = scheduleView == ScheduleView.Grid,
                    icon = Icons.Default.GridView,
                    description = strings.timetableView,
                ) {
                    scheduleView = ScheduleView.Grid
                    if (selectedDay == null) selectedDay = "2026-06-18"
                }
                if (scheduleView == ScheduleView.Grid) {
                    ViewButton(
                        selected = fullscreen,
                        icon = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        description = strings.fullscreen,
                    ) { onFullscreenChange(!fullscreen) }
                }
            }
        }
        if (scheduleView == ScheduleView.Grid) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Zoom ${(gridZoom * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Row {
                    IconButton(
                        onClick = { gridZoom = (gridZoom - 0.15f).coerceAtLeast(MinGridZoom) },
                        enabled = gridZoom > MinGridZoom,
                    ) { Icon(Icons.Default.ZoomOut, strings.zoomOut) }
                    IconButton(
                        onClick = { gridZoom = (gridZoom + 0.15f).coerceAtMost(MaxGridZoom) },
                        enabled = gridZoom < MaxGridZoom,
                    ) { Icon(Icons.Default.ZoomIn, strings.zoomIn) }
                }
            }
        }
    }

    if (filtered.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            controls()
            EmptyState(strings.noSetsMatch)
        }
    } else if (scheduleView == ScheduleView.Grid) {
        if (fullscreen) {
            Column(Modifier.fillMaxSize()) {
                controls()
                TimetableGrid(
                    sets = filtered,
                    selectedIds = selectedIds,
                    mustSeeIds = mustSeeIds,
                    spotifyMatches = spotifyMatches,
                    zoom = gridZoom,
                onZoomChange = { gridZoom = it },
                onToggle = onCycleSelection,
                onToggleMustSee = onToggleMustSee,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item { controls() }
                item {
                    Box(Modifier.fillMaxWidth().height(820.dp)) {
                        TimetableGrid(
                            sets = filtered,
                            selectedIds = selectedIds,
                            mustSeeIds = mustSeeIds,
                            spotifyMatches = spotifyMatches,
                            zoom = gridZoom,
                            onZoomChange = { gridZoom = it },
                            onToggle = onCycleSelection,
                            onToggleMustSee = onToggleMustSee,
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { controls() }
            filtered.groupBy { it.day }.forEach { (day, daySets) ->
                item(key = "header-$day") {
                    Text(
                        dayLabel(day).uppercase(),
                        color = HellOrange,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(daySets, key = FestivalSet::id) { set ->
                    SetCard(
                        set = set,
                        selected = set.id in selectedIds,
                        mustSee = set.id in mustSeeIds,
                        conflicting = false,
                        spotifyBadge = spotifyMatches.badgeFor(set.id),
                        onToggle = { onToggle(set.id) },
                        onToggleMustSee = { onToggleMustSee(set.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewButton(
    selected: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(
                if (selected) HellOrange else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp),
            )
            .size(40.dp),
    ) {
        Icon(icon, description, tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TimetableGrid(
    sets: List<FestivalSet>,
    selectedIds: Set<String>,
    mustSeeIds: Set<String>,
    spotifyMatches: SpotifyMatches,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onToggle: (String) -> Unit,
    onToggleMustSee: (String) -> Unit,
) {
    val stageOrder = listOf("MAINSTAGE 1", "MAINSTAGE 2", "WARZONE", "VALLEY", "ALTAR", "TEMPLE")
    val stages = stageOrder.filter { stage -> sets.any { it.stage == stage } }
    val startMinute = ((sets.minOf(::festivalStartMinute) / 60) * 60).coerceAtMost(10 * 60)
    val endMinute = (((sets.maxOf(::festivalEndMinute) + 59) / 60) * 60)
    val baseHourHeight = 82.dp
    val baseStageWidth = 132.dp
    val baseTimeWidth = 52.dp
    val headerHeight = 48.dp
    val density = LocalDensity.current
    var scrollXPx by remember { mutableFloatStateOf(0f) }
    var scrollYPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxSize().background(HellBlack)) {
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val baseTotalWidthPx = with(density) { (baseTimeWidth + baseStageWidth * stages.size).toPx() }
        val fitWidthZoom = if (rootWidthPx > 0f && baseTotalWidthPx > 0f) {
            (rootWidthPx / baseTotalWidthPx).coerceIn(MinGridZoom, MaxGridZoom)
        } else {
            MinGridZoom
        }
        val effectiveZoom = zoom.coerceIn(fitWidthZoom, MaxGridZoom)
        LaunchedEffect(effectiveZoom, rootWidthPx) {
            if (zoom != effectiveZoom && rootWidthPx > 0f) {
                onZoomChange(effectiveZoom)
            }
        }

        val hourHeight = baseHourHeight * effectiveZoom
        val stageWidth = baseStageWidth * effectiveZoom
        val timeWidth = baseTimeWidth * effectiveZoom
        val viewportWidth = (maxWidth - timeWidth).coerceAtLeast(0.dp)
        val viewportHeight = (maxHeight - headerHeight).coerceAtLeast(0.dp)
        val bodyHeight = hourHeight * ((endMinute - startMinute) / 60f)
        val bodyWidth = stageWidth * stages.size
        val bodyWidthPx = with(density) { bodyWidth.toPx() }
        val bodyHeightPx = with(density) { bodyHeight.toPx() }
        val viewportWidthPx = with(density) { viewportWidth.toPx() }
        val viewportHeightPx = with(density) { viewportHeight.toPx() }
        val maxScrollX = (bodyWidthPx - viewportWidthPx).coerceAtLeast(0f)
        val maxScrollY = (bodyHeightPx - viewportHeightPx).coerceAtLeast(0f)
        scrollXPx = scrollXPx.coerceIn(0f, maxScrollX)
        scrollYPx = scrollYPx.coerceIn(0f, maxScrollY)
        val scrollXDp = with(density) { scrollXPx.toDp() }
        val scrollYDp = with(density) { scrollYPx.toDp() }
        val gridLineWidth = bodyWidth + viewportWidth

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(effectiveZoom, fitWidthZoom, maxScrollX, maxScrollY) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        if (zoomChange != 1f) {
                            onZoomChange((effectiveZoom * zoomChange).coerceIn(fitWidthZoom, MaxGridZoom))
                        }
                        scrollXPx = (scrollXPx - pan.x).coerceIn(0f, maxScrollX)
                        scrollYPx = (scrollYPx - pan.y).coerceIn(0f, maxScrollY)
                    }
                },
        ) {
            Box(
                Modifier
                    .offset(x = timeWidth, y = headerHeight)
                    .width(viewportWidth)
                    .height(viewportHeight)
                    .clipToBounds()
            ) {
                Box(Modifier.fillMaxSize().background(HellBlack))
                for (minute in startMinute..endMinute step 30) {
                    val y = hourHeight * ((minute - startMinute) / 60f) - scrollYDp
                    Box(
                        Modifier
                            .offset(y = y)
                            .width(viewportWidth)
                            .height(1.dp)
                            .background(if (minute % 60 == 0) Color.DarkGray else Color(0xFF2B2522))
                    )
                }
                Box(
                    Modifier
                        .offset(x = -scrollXDp, y = -scrollYDp)
                        .width(gridLineWidth)
                        .height(bodyHeight)
                ) {
                    stages.forEachIndexed { index, _ ->
                        Box(
                            Modifier
                                .offset(x = stageWidth * index)
                                .width(1.dp)
                                .height(bodyHeight)
                                .background(Color(0xFF2B2522))
                        )
                    }

                    sets.forEach { set ->
                        val stageIndex = stages.indexOf(set.stage)
                        if (stageIndex >= 0) {
                            val start = festivalStartMinute(set)
                            val duration = (festivalEndMinute(set) - start).coerceAtLeast(15)
                            val x = stageWidth * stageIndex + 2.dp
                            val y = hourHeight * ((start - startMinute) / 60f) + 1.dp
                            val blockHeight = (hourHeight * (duration / 60f) - 2.dp).coerceAtLeast(30.dp)
                            val selected = set.id in selectedIds
                            val mustSee = set.id in mustSeeIds
                            val spotify = set.id in spotifyMatches.allSetIds
                            val cellShape = RoundedCornerShape(5.dp)
                            Box(
                                Modifier
                                    .offset(x = x, y = y)
                                    .width((stageWidth - 4.dp).coerceAtLeast(36.dp))
                                    .height(blockHeight)
                                    .clip(cellShape)
                                    .background(
                                        when {
                                            mustSee -> Color(0xFF007F68)
                                            selected -> Color(0xFF8A4300)
                                            else -> Color(0xFF44362E)
                                        },
                                        cellShape,
                                    )
                                    .border(
                                        if (spotify) 2.dp else 1.dp,
                                        when {
                                            spotify -> Color(0xFF1DB954)
                                            mustSee -> MustSeeMint
                                            selected -> HellOrange
                                            else -> Color(0xFF66564E)
                                        },
                                        cellShape,
                                    )
                                    .clickable { onToggle(set.id) }
                            ) {
                                Text(
                                    set.artist,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 4.dp, end = 4.dp, top = 3.dp),
                                    color = Color.White,
                                    fontSize = (8.5.sp * effectiveZoom.coerceIn(0.85f, 1.15f)),
                                    lineHeight = (9.5.sp * effectiveZoom.coerceIn(0.85f, 1.15f)),
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${formatTime(set.start)}-${formatTime(set.end)}",
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 4.dp, end = 4.dp, bottom = 3.dp),
                                    color = HellAmber,
                                    fontSize = (7.5.sp * effectiveZoom.coerceIn(0.9f, 1.15f)),
                                    lineHeight = (8.sp * effectiveZoom.coerceIn(0.9f, 1.15f)),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .offset(x = timeWidth)
                    .width(viewportWidth)
                    .height(headerHeight)
                    .clipToBounds(),
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xFF352C27)))
                Box(
                    Modifier
                        .offset(x = -scrollXDp)
                        .width(gridLineWidth)
                        .height(headerHeight)
                ) {
                    stages.forEachIndexed { index, stage ->
                        Text(
                            stage,
                            modifier = Modifier
                                .offset(x = stageWidth * index)
                                .width(stageWidth)
                                .height(headerHeight)
                                .background(Color(0xFF352C27))
                                .border(0.5.dp, Color.DarkGray)
                                .padding(horizontal = 3.dp, vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = (10.sp * effectiveZoom.coerceAtLeast(0.75f)),
                            lineHeight = (11.sp * effectiveZoom.coerceAtLeast(0.75f)),
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .offset(y = headerHeight)
                    .width(timeWidth)
                    .height(viewportHeight)
                    .clipToBounds()
                    .background(HellBlack),
            ) {
                Box(Modifier.offset(y = -scrollYDp).height(bodyHeight).width(timeWidth)) {
                    for (minute in startMinute..endMinute step 60) {
                        val y = hourHeight * ((minute - startMinute) / 60f) - 8.dp
                        Text(
                            minuteLabel(minute),
                            modifier = Modifier.offset(y = y).width(timeWidth),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = (10.sp * effectiveZoom.coerceAtLeast(0.75f)),
                            color = HellAmber,
                        )
                    }
                }
            }

            Text(
                "TIME",
                modifier = Modifier
                    .width(timeWidth)
                    .height(headerHeight)
                    .background(Color(0xFF352C27))
                    .border(0.5.dp, Color.DarkGray)
                    .padding(top = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = (11.sp * effectiveZoom.coerceAtLeast(0.75f)),
                fontWeight = FontWeight.Bold,
                color = HellAmber,
            )
        }
    }
}

@Composable
private fun PlanScreen(
    allSets: List<FestivalSet>,
    selectedIds: Set<String>,
    mustSeeIds: Set<String>,
    onToggle: (String) -> Unit,
    onToggleMustSee: (String) -> Unit,
    onShareImage: () -> Unit,
) {
    val strings = LocalStrings.current
    val now = currentFestivalTime()
    val selected = allSets.filter { it.id in selectedIds }.sortedBy { it.start }
    val clashes = conflictingIds(selected)
    val current = selected.firstOrNull { it.start <= now && now < it.end }
    val next = selected.firstOrNull { it.start > now }
    var todayOnly by remember { mutableStateOf(false) }
    val currentDay = now.take(10)
    val visible = if (todayOnly) selected.filter { it.start.take(10) == currentDay } else selected

    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusCard(strings.now, current?.artist ?: strings.noSetNow, current?.let { compactSetLine(it) }, Modifier.weight(1f))
                StatusCard(strings.next, next?.artist ?: strings.planComplete, next?.let { compactSetLine(it) }, Modifier.weight(1f))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.planCounts(selected.size, mustSeeIds.size, clashes.size), color = if (clashes.isEmpty()) HellAmber else ClashRed)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = todayOnly,
                    onClick = { todayOnly = !todayOnly },
                    label = { Text(strings.today) },
                )
                IconButton(
                    onClick = onShareImage,
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .size(40.dp),
                ) {
                    Icon(Icons.Default.Share, strings.shareSelectedSchedule)
                }
            }
        }
        if (selected.isEmpty()) {
            EmptyState(strings.emptyPlan)
        } else if (visible.isEmpty()) {
            EmptyState(strings.nothingSelectedToday)
        } else {
            SetList(visible, selectedIds, mustSeeIds, clashes, SpotifyMatches(), onToggle, onToggleMustSee)
        }
    }
}

@Composable
private fun FriendsScreen(
    allSets: List<FestivalSet>,
    selectedIds: Set<String>,
    mustSeeIds: Set<String>,
    storage: PlanStorage,
) {
    val strings = LocalStrings.current
    var userName by remember { mutableStateOf(strings.myPlan) }
    var importText by remember { mutableStateOf("") }
    var friendPlan by remember { mutableStateOf<UserPlan?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val exported = remember(userName, selectedIds) {
        PlanCodec.export(userName, selectedIds, currentFestivalTime())
    }
    val comparison = friendPlan?.let { comparePlans(allSets, selectedIds, it.selectedSetIds) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.exportMyPlan) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text(strings.yourName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        storage.copyText("Hellfest 2026 plan", exported)
                        copied = true
                    },
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (copied) strings.copied else strings.copyPlan)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { storage.sharePlanImage(allSets, selectedIds, mustSeeIds) },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareScheduleImage)
                }
                Spacer(Modifier.height(8.dp))
                Text(strings.compactJsonPreview, style = MaterialTheme.typography.bodySmall)
                SelectionContainer {
                    Text(
                        exported,
                        modifier = Modifier.fillMaxWidth().background(HellBlack, RoundedCornerShape(8.dp)).padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            SectionCard(strings.importFriend) {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text(strings.pasteExportedPlan) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                importError?.let { Text(it, color = ClashRed, modifier = Modifier.padding(top = 6.dp)) }
                Button(
                    onClick = {
                        PlanCodec.import(importText).onSuccess {
                            friendPlan = it
                            importError = null
                        }.onFailure {
                            importError = it.message ?: strings.unreadablePlan
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.comparePlans)
                }
            }
        }
        comparison?.let { result ->
            item { ComparisonSummary(friendPlan!!.userName, result) }
            item { ComparisonList(strings.bothSelected, result.both) }
            item { ComparisonList(strings.onlyMe, result.onlyMine) }
            item { ComparisonList("${strings.onlyFriendPrefix} ${friendPlan!!.userName.uppercase()}", result.onlyFriend) }
            if (result.crossPlanClashes.isNotEmpty()) {
                item {
                    SectionCard(strings.crossPlanClashes, ClashRed) {
                        result.crossPlanClashes.forEach { (mine, friend) ->
                            Text("${mine.artist} ↔ ${friend.artist}", fontWeight = FontWeight.Bold)
                            Text("${formatTime(mine.start)}–${formatTime(mine.end)} / ${formatTime(friend.start)}–${formatTime(friend.end)}")
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    allSets: List<FestivalSet>,
    selectedIds: Set<String>,
    totalSets: Int,
    selectedCount: Int,
    storage: PlanStorage,
    spotifyMatches: SpotifyMatches,
    onSpotifyMatches: (SpotifyMatches) -> Unit,
    onAddSpotifyToPlan: () -> Unit,
    onClear: () -> Unit,
    onReload: () -> Unit,
) {
    val strings = LocalStrings.current
    var showClearDialog by remember { mutableStateOf(false) }
    var spotifyClientId by remember { mutableStateOf(storage.loadSpotifyClientId()) }
    var spotifyStatus by remember {
        mutableStateOf(if (storage.hasSpotifyToken()) strings.spotifyConnected else strings.spotifyDisconnected)
    }
    var syncingSpotify by remember { mutableStateOf(false) }
    var creatingPlaylist by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(strings.appStatus) {
            StatRow(strings.bundledSets, totalSets.toString())
            StatRow(strings.selectedSets, selectedCount.toString())
            StatRow(strings.version, "0.1.0")
            StatRow(strings.build, buildDate())
            StatRow(strings.timezone, "Europe/Paris")
        }
        OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) {
            Text(strings.reimportBundledJson)
        }
        SectionCard(strings.spotifyMatch) {
            if (spotifyClientId.isNotBlank()) {
                Text(
                    strings.spotifyConfigured,
                    color = HellAmber,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
            }
            OutlinedTextField(
                value = spotifyClientId,
                onValueChange = {
                    spotifyClientId = it.trim()
                    storage.saveSpotifyClientId(spotifyClientId)
                },
                label = { Text(strings.spotifyClientId) },
                supportingText = { Text(strings.spotifyClientIdOptional) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        storage.saveSpotifyClientId(spotifyClientId)
                        storage.startSpotifyLogin(spotifyClientId)
                        spotifyStatus = strings.openingSpotifyLogin
                    },
                    enabled = spotifyClientId.isNotBlank(),
                ) {
                    Text(if (storage.hasSpotifyToken()) "Reconnect" else "Connect")
                }
                OutlinedButton(
                    onClick = {
                        syncingSpotify = true
                        spotifyStatus = strings.syncingSpotify
                        storage.syncSpotifyMatches(allSets) { result ->
                            syncingSpotify = false
                            result.onSuccess {
                                onSpotifyMatches(it.matches)
                                spotifyStatus = strings.spotifyFound(it.matches.allSetIds.size, it.followedArtists, it.topArtists, it.libraryArtists)
                            }.onFailure {
                                spotifyStatus = it.message ?: strings.spotifySyncError
                            }
                        }
                    },
                    enabled = storage.hasSpotifyToken() && !syncingSpotify,
                ) {
                    Icon(Icons.Default.Sync, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (syncingSpotify) "Sync..." else "Sync")
                }
            }
            if (storage.hasSpotifyToken()) {
                TextButton(
                    onClick = {
                        storage.disconnectSpotify()
                        onSpotifyMatches(SpotifyMatches())
                        spotifyStatus = strings.spotifyDisconnected
                    },
                ) {
                    Text(strings.disconnectSpotify)
                }
            }
            Text(spotifyStatus, color = HellAmber, style = MaterialTheme.typography.bodySmall)
            if (spotifyMatches.allSetIds.isNotEmpty()) {
                Button(
                    onClick = onAddSpotifyToPlan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Favorite, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.makePlannerFromSpotify)
                }
                Spacer(Modifier.height(6.dp))
            }
            if (storage.hasSpotifyToken()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            creatingPlaylist = true
                            spotifyStatus = strings.creatingPlanPlaylist
                            storage.createSpotifyPlaylist(
                                playlistName = "Hellfest 2026 - My Plan",
                                sets = allSets.filter { it.id in selectedIds }.sortedBy { it.start },
                            ) { result ->
                                creatingPlaylist = false
                                spotifyStatus = playlistStatus(result, strings)
                            }
                        },
                        enabled = selectedIds.isNotEmpty() && !creatingPlaylist,
                    ) {
                        Text(strings.playlistPlan)
                    }
                    OutlinedButton(
                        onClick = {
                            creatingPlaylist = true
                            spotifyStatus = strings.creatingNotPicksPlaylist
                            storage.createSpotifyPlaylist(
                                playlistName = "Hellfest 2026 - Not My Picks",
                                sets = allSets.filter { it.id !in selectedIds }.sortedBy { it.start },
                            ) { result ->
                                creatingPlaylist = false
                                spotifyStatus = playlistStatus(result, strings)
                            }
                        },
                        enabled = allSets.any { it.id !in selectedIds } && !creatingPlaylist,
                    ) {
                        Text(strings.playlistNotPicks)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (spotifyMatches.allSetIds.isNotEmpty()) {
                Text(
                    strings.spotifyStats(spotifyMatches.followedSetIds.size, spotifyMatches.topSetIds.size, spotifyMatches.librarySetIds.size, spotifyMatches.syncedAt),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                strings.spotifyDashboardHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        OutlinedButton(onClick = { showClearDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(strings.clearAllSelections, color = ClashRed)
        }
        Text(
            strings.scheduleSourceHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        SectionCard(strings.about) {
            Text(strings.developedBy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(strings.vibeCoding, color = HellAmber)
            Spacer(Modifier.height(4.dp))
            Text(strings.github, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(strings.clearPlanTitle) },
            text = { Text(strings.clearPlanText) },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showClearDialog = false
                }) { Text(strings.clear, color = ClashRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(strings.cancel) }
            },
        )
    }
}

@Composable
private fun SetList(
    sets: List<FestivalSet>,
    selectedIds: Set<String>,
    mustSeeIds: Set<String>,
    conflictIds: Set<String>,
    spotifyMatches: SpotifyMatches = SpotifyMatches(),
    onToggle: (String) -> Unit,
    onToggleMustSee: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sets.groupBy { it.day }.forEach { (day, daySets) ->
            item(key = "header-$day") {
                Text(
                    dayLabel(day).uppercase(),
                    color = HellOrange,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(daySets, key = FestivalSet::id) { set ->
                SetCard(
                    set = set,
                    selected = set.id in selectedIds,
                    mustSee = set.id in mustSeeIds,
                    conflicting = set.id in conflictIds,
                    spotifyBadge = spotifyMatches.badgeFor(set.id),
                    onToggle = { onToggle(set.id) },
                    onToggleMustSee = { onToggleMustSee(set.id) },
                )
            }
        }
    }
}

@Composable
private fun SetCard(
    set: FestivalSet,
    selected: Boolean,
    mustSee: Boolean,
    conflicting: Boolean,
    spotifyBadge: String?,
    onToggle: () -> Unit,
    onToggleMustSee: () -> Unit,
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = when {
                conflicting -> Color(0xFF4A2020)
                mustSee -> Color(0xFF123F38)
                selected -> Color(0xFF44301D)
                else -> HellSurface
            }
        ),
        border = if (selected || mustSee) androidx.compose.foundation.BorderStroke(1.dp, if (mustSee) MustSeeMint else if (conflicting) ClashRed else HellOrange) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        set.artist,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (conflicting) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Warning, strings.clash, tint = ClashRed, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "${formatTime(set.start)}–${formatTime(set.end)}  ·  ${set.stage}",
                    color = HellAmber,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(dayLabel(set.day), style = MaterialTheme.typography.bodySmall)
                if (spotifyBadge != null) {
                    Text(spotifyBadge, color = HellAmber, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onToggleMustSee) {
                Icon(
                    if (mustSee) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (mustSee) strings.removeMustSee else strings.markMustSee,
                    tint = if (mustSee) MustSeeMint else HellAmber,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (selected) strings.removeFromPlan else strings.addToPlan,
                    tint = if (selected) HellOrange else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun StatusCard(label: String, title: String, detail: String?, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF35251B))) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = HellOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = HellAmber) }
        }
    }
}

@Composable
private fun SectionCard(title: String, titleColor: Color = HellOrange, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = HellSurface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ComparisonSummary(friendName: String, result: FriendComparison) {
    val strings = LocalStrings.current
    SectionCard("${strings.comparedWith} ${friendName.uppercase()}") {
        StatRow(strings.bothSelected, result.both.size.toString())
        StatRow(strings.onlyMe, result.onlyMine.size.toString())
        StatRow("${strings.onlyFriendPrefix} $friendName", result.onlyFriend.size.toString())
        StatRow(strings.crossPlanClashes, result.crossPlanClashes.size.toString(), result.crossPlanClashes.isNotEmpty())
    }
}

@Composable
private fun ComparisonList(title: String, sets: List<FestivalSet>) {
    val strings = LocalStrings.current
    SectionCard(title) {
        if (sets.isEmpty()) {
            Text(strings.none, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            sets.forEachIndexed { index, set ->
                Text(set.artist, fontWeight = FontWeight.Bold)
                Text(compactSetLine(set), color = HellAmber, style = MaterialTheme.typography.bodySmall)
                if (index != sets.lastIndex) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, warning: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = if (warning) ClashRed else HellAmber, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, modifier = Modifier.padding(32.dp), color = HellAmber)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Warning, null, tint = ClashRed, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(12.dp))
        Text(message)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text(strings.retry) }
    }
}

@Composable
private fun compactSetLine(set: FestivalSet): String =
    "${dayLabel(set.day)} · ${formatTime(set.start)} · ${set.stage}"

@Composable
private fun SpotifyMatches.badgeFor(setId: String): String? {
    val strings = LocalStrings.current
    val rank = topRankBySetId[setId]
    return when {
        setId in followedSetIds && rank != null -> "Spotify: ${strings.followed} · ${strings.top} #$rank"
        setId in followedSetIds && setId in librarySetIds -> "Spotify: ${strings.followed} · ${strings.saved}"
        setId in followedSetIds -> "Spotify: ${strings.followed}"
        rank != null -> "Spotify: ${strings.top} #$rank"
        setId in librarySetIds -> "Spotify: ${strings.saved}"
        else -> null
    }
}

private fun playlistStatus(result: Result<SpotifyPlaylistResult>, strings: UiStrings): String =
    result.fold(
        onSuccess = { strings.playlistCreated(it) },
        onFailure = { it.message ?: strings.playlistCreateError },
    )

private fun festivalStartMinute(set: FestivalSet): Int = festivalMinute(set.start, set.day)

private fun festivalEndMinute(set: FestivalSet): Int = festivalMinute(set.end, set.day)

private fun festivalMinute(iso: String, festivalDay: String): Int {
    val hour = iso.substring(11, 13).toInt()
    val minute = iso.substring(14, 16).toInt()
    val dayOffset = if (iso.take(10) > festivalDay) 24 * 60 else 0
    return dayOffset + hour * 60 + minute
}

private fun formatTime(iso: String): String = iso.substring(11, 16)

private fun minuteLabel(minute: Int): String {
    val normalized = minute % (24 * 60)
    return "${(normalized / 60).toString().padStart(2, '0')}:00"
}

@Composable
private fun dayLabel(day: String): String {
    val pt = LocalStrings.current === PtStrings
    return when (day) {
    "2026-06-18" -> if (pt) "Qui 18 Jun" else "Thu 18 Jun"
    "2026-06-19" -> if (pt) "Sex 19 Jun" else "Fri 19 Jun"
    "2026-06-20" -> if (pt) "Sab 20 Jun" else "Sat 20 Jun"
    "2026-06-21" -> if (pt) "Dom 21 Jun" else "Sun 21 Jun"
    else -> day
    }
}
