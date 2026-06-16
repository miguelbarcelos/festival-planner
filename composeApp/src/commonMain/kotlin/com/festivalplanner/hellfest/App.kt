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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private enum class Screen(val label: String, val icon: ImageVector) {
    Schedule("Schedule", Icons.Default.List),
    Plan("My Plan", Icons.Default.Favorite),
    Friends("Friends", Icons.Default.Groups),
    Settings("Settings", Icons.Default.Settings),
}

private enum class ScheduleView {
    List,
    Grid,
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FestivalPlannerApp(storage: PlanStorage) {
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
            loadError = it.message ?: "Could not load the bundled schedule."
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

    MaterialTheme(colorScheme = HellfestColors) {
        Scaffold(
            topBar = {
                if (!scheduleFullscreen) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HELLFEST 2026", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(screen.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = HellAmber)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!scheduleFullscreen) {
                    NavigationBar(containerColor = Color(0xFF1B1614)) {
                        Screen.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = screen == destination,
                                onClick = { screen = destination },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label, maxLines = 1) },
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
                    allSets.isEmpty() -> EmptyState("Loading the pit...")
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
    val dayOptions = listOf(
        null to "All days",
        "2026-06-18" to "Thu 18",
        "2026-06-19" to "Fri 19",
        "2026-06-20" to "Sat 20",
        "2026-06-21" to "Sun 21",
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
                placeholder = { Text("Search artist") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, "Clear search") }
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
                        label = { Text(stage ?: "All stages") },
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
                "${filtered.size} sets · ${selectedIds.size} selected · ${mustSeeIds.size} must-see",
                style = MaterialTheme.typography.labelLarge,
                color = HellAmber,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ViewButton(
                    selected = scheduleView == ScheduleView.List,
                    icon = Icons.Default.List,
                    description = "List view",
                ) { scheduleView = ScheduleView.List }
                ViewButton(
                    selected = scheduleView == ScheduleView.Grid,
                    icon = Icons.Default.GridView,
                    description = "Timetable view",
                ) {
                    scheduleView = ScheduleView.Grid
                    if (selectedDay == null) selectedDay = "2026-06-18"
                }
                if (scheduleView == ScheduleView.Grid) {
                    ViewButton(
                        selected = fullscreen,
                        icon = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        description = "Fullscreen",
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
                    ) { Icon(Icons.Default.ZoomOut, "Zoom out") }
                    IconButton(
                        onClick = { gridZoom = (gridZoom + 0.15f).coerceAtMost(MaxGridZoom) },
                        enabled = gridZoom < MaxGridZoom,
                    ) { Icon(Icons.Default.ZoomIn, "Zoom in") }
                }
            }
        }
    }

    if (filtered.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            controls()
            EmptyState("No sets match those filters.")
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
                StatusCard("NOW", current?.artist ?: "No set now", current?.let(::compactSetLine), Modifier.weight(1f))
                StatusCard("NEXT", next?.artist ?: "Plan complete", next?.let(::compactSetLine), Modifier.weight(1f))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${selected.size} selected · ${mustSeeIds.size} must-see · ${clashes.size} clashes", color = if (clashes.isEmpty()) HellAmber else ClashRed)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = todayOnly,
                    onClick = { todayOnly = !todayOnly },
                    label = { Text("Today") },
                )
                IconButton(
                    onClick = onShareImage,
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .size(40.dp),
                ) {
                    Icon(Icons.Default.Share, "Share selected schedule")
                }
            }
        }
        if (selected.isEmpty()) {
            EmptyState("Your plan is empty.\nTap hearts in Schedule to build it.")
        } else if (visible.isEmpty()) {
            EmptyState("Nothing selected for today.")
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
    var userName by remember { mutableStateOf("My plan") }
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
            SectionCard("EXPORT MY PLAN") {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your name") },
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
                    Text(if (copied) "Copied" else "Copy plan")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { storage.sharePlanImage(allSets, selectedIds, mustSeeIds) },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share schedule image")
                }
                Spacer(Modifier.height(8.dp))
                Text("Compact JSON preview", style = MaterialTheme.typography.bodySmall)
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
            SectionCard("IMPORT A FRIEND") {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste their exported plan") },
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
                            importError = it.message ?: "That plan could not be read."
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compare plans")
                }
            }
        }
        comparison?.let { result ->
            item { ComparisonSummary(friendPlan!!.userName, result) }
            item { ComparisonList("BOTH SELECTED", result.both) }
            item { ComparisonList("ONLY ME", result.onlyMine) }
            item { ComparisonList("ONLY ${friendPlan!!.userName.uppercase()}", result.onlyFriend) }
            if (result.crossPlanClashes.isNotEmpty()) {
                item {
                    SectionCard("CROSS-PLAN CLASHES", ClashRed) {
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
    var showClearDialog by remember { mutableStateOf(false) }
    var spotifyClientId by remember { mutableStateOf(storage.loadSpotifyClientId()) }
    var spotifyStatus by remember {
        mutableStateOf(if (storage.hasSpotifyToken()) "Spotify ligado. Podes sincronizar." else "Spotify não ligado.")
    }
    var syncingSpotify by remember { mutableStateOf(false) }
    var creatingPlaylist by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("APP STATUS") {
            StatRow("Bundled sets", totalSets.toString())
            StatRow("Selected sets", selectedCount.toString())
            StatRow("Version", "0.1.0")
            StatRow("Build", buildDate())
            StatRow("Timezone", "Europe/Paris")
        }
        OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) {
            Text("Reimport bundled JSON")
        }
        SectionCard("SPOTIFY MATCH") {
            OutlinedTextField(
                value = spotifyClientId,
                onValueChange = {
                    spotifyClientId = it.trim()
                    storage.saveSpotifyClientId(spotifyClientId)
                },
                label = { Text("Spotify client_id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        storage.saveSpotifyClientId(spotifyClientId)
                        storage.startSpotifyLogin(spotifyClientId)
                        spotifyStatus = "A abrir login do Spotify..."
                    },
                    enabled = spotifyClientId.isNotBlank(),
                ) {
                    Text(if (storage.hasSpotifyToken()) "Reconnect" else "Connect")
                }
                OutlinedButton(
                    onClick = {
                        syncingSpotify = true
                        spotifyStatus = "A sincronizar Spotify..."
                        storage.syncSpotifyMatches(allSets) { result ->
                            syncingSpotify = false
                            result.onSuccess {
                                onSpotifyMatches(it.matches)
                                spotifyStatus = "Encontradas ${it.matches.allSetIds.size} bandas: ${it.followedArtists} seguidas, ${it.topArtists} top, ${it.libraryArtists} na biblioteca."
                            }.onFailure {
                                spotifyStatus = it.message ?: "Erro ao sincronizar Spotify."
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
                        spotifyStatus = "Spotify desligado."
                    },
                ) {
                    Text("Disconnect Spotify")
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
                    Text("Make my planner from Spotify")
                }
                Spacer(Modifier.height(6.dp))
            }
            if (storage.hasSpotifyToken()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            creatingPlaylist = true
                            spotifyStatus = "A criar playlist do teu plano..."
                            storage.createSpotifyPlaylist(
                                playlistName = "Hellfest 2026 - My Plan",
                                sets = allSets.filter { it.id in selectedIds }.sortedBy { it.start },
                            ) { result ->
                                creatingPlaylist = false
                                spotifyStatus = playlistStatus(result)
                            }
                        },
                        enabled = selectedIds.isNotEmpty() && !creatingPlaylist,
                    ) {
                        Text("Playlist plan")
                    }
                    OutlinedButton(
                        onClick = {
                            creatingPlaylist = true
                            spotifyStatus = "A criar playlist do que ficou fora..."
                            storage.createSpotifyPlaylist(
                                playlistName = "Hellfest 2026 - Not My Picks",
                                sets = allSets.filter { it.id !in selectedIds }.sortedBy { it.start },
                            ) { result ->
                                creatingPlaylist = false
                                spotifyStatus = playlistStatus(result)
                            }
                        },
                        enabled = allSets.any { it.id !in selectedIds } && !creatingPlaylist,
                    ) {
                        Text("Playlist not picks")
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (spotifyMatches.allSetIds.isNotEmpty()) {
                Text(
                    "${spotifyMatches.followedSetIds.size} seguidas · ${spotifyMatches.topSetIds.size} top · ${spotifyMatches.librarySetIds.size} biblioteca · ${spotifyMatches.syncedAt}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "No Spotify Dashboard usa redirect URI: hellfestplanner://spotify-auth. Para biblioteca, reconecta uma vez para aprovar user-library-read.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        OutlinedButton(onClick = { showClearDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear all selections", color = ClashRed)
        }
        Text(
            "Schedule source: Clashfinder. Times and lineup can change; regenerate the bundled JSON before the festival.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear your plan?") },
            text = { Text("This removes every selected set from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showClearDialog = false
                }) { Text("Clear", color = ClashRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
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
                        Icon(Icons.Default.Warning, "Clash", tint = ClashRed, modifier = Modifier.size(19.dp))
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
                    contentDescription = if (mustSee) "Remove must-see" else "Mark must-see",
                    tint = if (mustSee) MustSeeMint else HellAmber,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (selected) "Remove from plan" else "Add to plan",
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
    SectionCard("COMPARED WITH ${friendName.uppercase()}") {
        StatRow("Both selected", result.both.size.toString())
        StatRow("Only me", result.onlyMine.size.toString())
        StatRow("Only $friendName", result.onlyFriend.size.toString())
        StatRow("Cross-plan clashes", result.crossPlanClashes.size.toString(), result.crossPlanClashes.isNotEmpty())
    }
}

@Composable
private fun ComparisonList(title: String, sets: List<FestivalSet>) {
    SectionCard(title) {
        if (sets.isEmpty()) {
            Text("None", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Warning, null, tint = ClashRed, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(12.dp))
        Text(message)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
    }
}

private fun compactSetLine(set: FestivalSet): String =
    "${dayLabel(set.day)} · ${formatTime(set.start)} · ${set.stage}"

private fun SpotifyMatches.badgeFor(setId: String): String? {
    val rank = topRankBySetId[setId]
    return when {
        setId in followedSetIds && rank != null -> "Spotify: followed · top #$rank"
        setId in followedSetIds && setId in librarySetIds -> "Spotify: followed · saved"
        setId in followedSetIds -> "Spotify: followed"
        rank != null -> "Spotify: top #$rank"
        setId in librarySetIds -> "Spotify: saved"
        else -> null
    }
}

private fun playlistStatus(result: Result<SpotifyPlaylistResult>): String =
    result.fold(
        onSuccess = {
            "Playlist '${it.playlistName}' criada com ${it.trackCount} músicas. Faltaram ${it.missingArtists.size} artistas."
        },
        onFailure = { it.message ?: "Erro ao criar playlist Spotify." },
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

private fun dayLabel(day: String): String = when (day) {
    "2026-06-18" -> "Thu 18 Jun"
    "2026-06-19" -> "Fri 19 Jun"
    "2026-06-20" -> "Sat 20 Jun"
    "2026-06-21" -> "Sun 21 Jun"
    else -> day
}
