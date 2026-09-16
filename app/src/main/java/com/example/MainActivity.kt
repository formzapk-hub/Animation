package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import android.widget.Toast
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AnimationProject
import com.example.ui.FrameDrawer
import com.example.ui.ProjectViewModel
import com.example.ui.RenderState
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KineticTextStudioApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

enum class StudioTab(val label: String) {
    EDITOR("Editor"),
    EXPORT("Ekspor"),
    COLLAB("Kolaborasi"),
    PROJECTS("Proyek")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KineticTextStudioApp(
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(StudioTab.EDITOR) }
    val activeProject by viewModel.activeProject.collectAsState()
    val localProjects by viewModel.localProjects.collectAsState()

    var showProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeProject?.name ?: "Kinetic Text Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeProject?.isCloudSynced == true) "Awan Kolaboratif" else "Penyimpanan Lokal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showProjectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat Proyek Baru"
                        )
                    }
                    if (activeProject?.isCloudSynced == true) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Koneksi Awan Aktif",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Penyimpanan Lokal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar {
                StudioTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = {
                            when (tab) {
                                StudioTab.EDITOR -> Icon(
                                    imageVector = if (selectedTab == tab) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                                    contentDescription = "Edit Kanvas"
                                )
                                StudioTab.EXPORT -> Icon(
                                    imageVector = Icons.Outlined.Videocam,
                                    contentDescription = "Ekspor MP4"
                                )
                                StudioTab.COLLAB -> Icon(
                                    imageVector = if (selectedTab == tab) Icons.Filled.Group else Icons.Default.Group,
                                    contentDescription = "Diskusi Kolaborasi"
                                )
                                StudioTab.PROJECTS -> Icon(
                                    imageVector = if (selectedTab == tab) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Proyek Tersimpan"
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                StudioTab.EDITOR -> EditorTabContent(viewModel = viewModel)
                StudioTab.EXPORT -> ExportTabContent(viewModel = viewModel)
                StudioTab.COLLAB -> CollaborationTabContent(viewModel = viewModel)
                StudioTab.PROJECTS -> ProjectsTabContent(
                    viewModel = viewModel,
                    onCreateProjectClick = { showProjectDialog = true }
                )
            }
        }
    }

    // New Project Dialog
    if (showProjectDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDialog = false },
            title = { Text("Buat Proyek Animasi Baru") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Nama Proyek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createNewProject(newProjectName)
                            newProjectName = ""
                            showProjectDialog = false
                            selectedTab = StudioTab.EDITOR
                        }
                    }
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showProjectDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorTabContent(viewModel: ProjectViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    var isPlaying by remember { mutableStateOf(true) }
    var isLoopEnabled by remember { mutableStateOf(true) }
    var frameIdx by remember { mutableFloatStateOf(0f) }

    // Dynamic FPS controller for real-time live preview matching Speed Multiplier
    LaunchedEffect(isPlaying, isLoopEnabled, project?.speed, project?.animType) {
        if (!isPlaying) return@LaunchedEffect
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { time ->
                val deltaSec = (time - lastTime) / 1_000_000_000f
                lastTime = time
                val speed = project?.speed ?: 1.0f
                val framesToAdvance = deltaSec * 30f * speed
                val nextFrame = frameIdx + framesToAdvance
                if (nextFrame >= 150f) {
                    if (isLoopEnabled) {
                        frameIdx = nextFrame % 150f
                    } else {
                        frameIdx = 149f
                        isPlaying = false
                    }
                } else {
                    frameIdx = nextFrame
                }
            }
        }
    }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Memuat proyek aktif...")
        }
        return
    }

    val currentProj = project!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Canvas Display Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("preview_canvas")
                ) {
                    drawIntoCanvas { composeCanvas ->
                        val nativeCanvas = composeCanvas.nativeCanvas
                        FrameDrawer.drawFrame(
                            canvas = nativeCanvas,
                            width = size.width.toInt(),
                            height = size.height.toInt(),
                            frameIdx = frameIdx.toInt(),
                            text = currentProj.text,
                            animType = currentProj.animType,
                            speedMult = currentProj.speed,
                            textColorHex = currentProj.textColor,
                            bgColorHex = currentProj.backgroundColor,
                            baseFontSize = currentProj.fontSize,
                            fontName = currentProj.fontName,
                            context = context,
                            isShadowEnabled = currentProj.isShadowEnabled,
                            shadowColorHex = currentProj.shadowColor,
                            shadowRadius = currentProj.shadowRadius,
                            shadowDx = currentProj.shadowDx,
                            shadowDy = currentProj.shadowDy,
                            isOutlineEnabled = currentProj.isOutlineEnabled,
                            outlineColorHex = currentProj.outlineColor,
                            outlineWidth = currentProj.outlineWidth
                        )
                    }
                }

                // Controls overlay (Play/Pause, Loop, Replay, Frame index)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { isLoopEnabled = !isLoopEnabled }) {
                        Icon(
                            imageVector = if (isLoopEnabled) Icons.Default.Repeat else Icons.Default.RepeatOne,
                            contentDescription = "Loop Toggle",
                            tint = if (isLoopEnabled) Color(0xFF00FF88) else Color.White
                        )
                    }
                    IconButton(onClick = { 
                        frameIdx = 0f
                        isPlaying = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Reset Frame",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Frame: ${frameIdx.toInt()}/150",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }

        // Row for Text Editor Title and Undo/Redo Controls
        val canUndo by viewModel.canUndo.collectAsState()
        val canRedo by viewModel.canRedo.collectAsState()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kustomisasi Teks & Animasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Text input field with instant database auto-save trigger
        TextField(
            value = currentProj.text,
            onValueChange = { newText ->
                viewModel.updateActiveProject { it.copy(text = newText) }
            },
            label = { Text("Teks Animasi") },
            placeholder = { Text("Masukkan teks di sini...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Dropdown Animation Selector
        var expandedMenu by remember { mutableStateOf(false) }
        val animationStyles = listOf(
            "Teks Melingkar",
            "Teks Efek Ketik",
            "Teks Berjalan",
            "Teks Memudar",
            "Teks Melompat",
            "Teks Zoom In",
            "Teks Bergetar",
            "Teks Bergelombang",
            "Teks Jatuh",
            "Teks Terbelah",
            "Teks Neon Glitch",
            "Teks Rotasi 3D",
            "Teks Putar 3D Silinder",
            "Teks Ledakan Partikel",
            "Teks Tirai Bayangan",
            "Teks Ketukan Bass",
            "Teks Penjelajah Angkasa"
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = currentProj.animType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gaya Animasi") },
                trailingIcon = {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih gaya")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedMenu = true },
                shape = RoundedCornerShape(12.dp)
            )

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                animationStyles.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style) },
                        onClick = {
                            viewModel.updateActiveProject { it.copy(animType = style) }
                            expandedMenu = false
                        }
                    )
                }
            }
        }

        // Dropdown Font Selector
        var expandedFontMenu by remember { mutableStateOf(false) }
        val fontStyles = listOf("Montserrat", "Playfair Display", "Pacifico", "Roboto Mono", "Bebas Neue")

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = currentProj.fontName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gaya Tipografi (Font)") },
                trailingIcon = {
                    IconButton(onClick = { expandedFontMenu = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih tipografi")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFontMenu = true },
                shape = RoundedCornerShape(12.dp)
            )

            DropdownMenu(
                expanded = expandedFontMenu,
                onDismissRequest = { expandedFontMenu = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                fontStyles.forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font) },
                        onClick = {
                            viewModel.updateActiveProject { it.copy(fontName = font) }
                            expandedFontMenu = false
                        }
                    )
                }
            }
        }

        // Speed Adjustment Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kecepatan Animasi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", currentProj.speed)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = currentProj.speed,
                onValueChange = { newValue ->
                    viewModel.updateActiveProject { it.copy(speed = newValue) }
                },
                valueRange = 0.5f..3.0f,
                steps = 25
            )
        }

        // Font Size Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ukuran Huruf",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentProj.fontSize.toInt()} px",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = currentProj.fontSize,
                onValueChange = { newValue ->
                    viewModel.updateActiveProject { it.copy(fontSize = newValue) }
                },
                valueRange = 24f..120f
            )
        }

        // --- TEMA WARNA PROFESIONAL PRESETS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tema Warna Profesional",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Terapkan palet warna seimbang yang dirancang secara profesional untuk keterbacaan tinggi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            val professionalPalettes = listOf(
                Triple("Neon Cyber", "#00E5FF", "#070F2B"),
                Triple("Aura Sunset", "#FF00E6", "#1B1A55"),
                Triple("Sage Emerald", "#E1E5D5", "#1B3636"),
                Triple("Gold Luxe", "#FFE57F", "#000000"),
                Triple("Vintage Amber", "#FF7A00", "#121212"),
                Triple("Aurora Frost", "#00FF90", "#051010"),
                Triple("Tokyo Noir", "#FF007F", "#0B0B0E")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                items(professionalPalettes) { (name, txtHex, bgHex) ->
                    val isSelected = currentProj.textColor.equals(txtHex, true) &&
                            currentProj.backgroundColor.equals(bgHex, true)

                    Card(
                        onClick = {
                            viewModel.updateActiveProject {
                                it.copy(textColor = txtHex, backgroundColor = bgHex)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tiny Preview Capsule
                            Row(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(android.graphics.Color.parseColor(txtHex))))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(android.graphics.Color.parseColor(bgHex))))
                            }

                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Modern Text Color Palette Presets
        val textColors = listOf(
            "#00E5FF" to "Cyan",
            "#FF00E6" to "Magenta",
            "#D4FF00" to "Lemon",
            "#FF6A00" to "Neon",
            "#FFFFFF" to "Putih"
        )
        Column {
            Text(
                text = "Warna Huruf",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                textColors.forEach { (hex, name) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (currentProj.textColor.equals(hex, true)) 3.dp else 1.dp,
                                color = if (currentProj.textColor.equals(hex, true)) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.updateActiveProject { it.copy(textColor = hex) }
                            }
                    )
                }
            }
        }

        // Modern Background Color Palette Presets
        val bgColors = listOf(
            "#121212" to "Charcoal",
            "#070F2B" to "Deep Blue",
            "#000000" to "Pure Black",
            "#1B1A55" to "Royal Purple",
            "#1A3636" to "Emerald"
        )
        Column {
            Text(
                text = "Warna Latar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                bgColors.forEach { (hex, name) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (currentProj.backgroundColor.equals(hex, true)) 3.dp else 1.dp,
                                color = if (currentProj.backgroundColor.equals(hex, true)) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.updateActiveProject { it.copy(backgroundColor = hex) }
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // SECTION: TEXT SHADOW (BAYANGAN)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Efek Bayangan Teks",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Efek kedalaman & kelembutan di belakang teks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = currentProj.isShadowEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateActiveProject { it.copy(isShadowEnabled = isEnabled) }
                    }
                )
            }

            if (currentProj.isShadowEnabled) {
                // Blur Radius Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Radius Blur (Kelembutan)", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", currentProj.shadowRadius)} px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = currentProj.shadowRadius,
                        onValueChange = { newValue ->
                            viewModel.updateActiveProject { it.copy(shadowRadius = newValue) }
                        },
                        valueRange = 1f..25f
                    )
                }

                // Offset DX and DY Sliders
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Geser X", style = MaterialTheme.typography.bodySmall)
                            Text("${currentProj.shadowDx.toInt()} px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = currentProj.shadowDx,
                            onValueChange = { newValue ->
                                viewModel.updateActiveProject { it.copy(shadowDx = newValue) }
                            },
                            valueRange = -20f..20f
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Geser Y", style = MaterialTheme.typography.bodySmall)
                            Text("${currentProj.shadowDy.toInt()} px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = currentProj.shadowDy,
                            onValueChange = { newValue ->
                                viewModel.updateActiveProject { it.copy(shadowDy = newValue) }
                            },
                            valueRange = -20f..20f
                        )
                    }
                }

                // Shadow Color Pickers
                val shadowColors = listOf("#000000", "#333333", "#4A0000", "#001A4A")
                Column {
                    Text("Warna Bayangan", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shadowColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentProj.shadowColor.equals(hex, true)) 2.dp else 1.dp,
                                        color = if (currentProj.shadowColor.equals(hex, true)) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateActiveProject { it.copy(shadowColor = hex) }
                                    }
                            )
                        }
                        
                        // Manual Hex input
                        var customColorText by remember(currentProj.shadowColor) { mutableStateOf(currentProj.shadowColor) }
                        OutlinedTextField(
                            value = customColorText,
                            onValueChange = { valText ->
                                customColorText = valText
                                if (valText.startsWith("#") && (valText.length == 7 || valText.length == 9)) {
                                    try {
                                        android.graphics.Color.parseColor(valText)
                                        viewModel.updateActiveProject { it.copy(shadowColor = valText) }
                                    } catch (e: Exception) {}
                                }
                            },
                            label = { Text("Hex") },
                            maxLines = 1,
                            singleLine = true,
                            modifier = Modifier.width(100.dp).height(50.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // SECTION: TEXT OUTLINE (GARIS TEPI)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Garis Tepi Teks",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kontras tinggi terhadap latar belakang video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = currentProj.isOutlineEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateActiveProject { it.copy(isOutlineEnabled = isEnabled) }
                    }
                )
            }

            if (currentProj.isOutlineEnabled) {
                // Outline Width Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ketebalan Garis Tepi", style = MaterialTheme.typography.bodySmall)
                        Text("${currentProj.outlineWidth.toInt()} px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = currentProj.outlineWidth,
                        onValueChange = { newValue ->
                            viewModel.updateActiveProject { it.copy(outlineWidth = newValue) }
                        },
                        valueRange = 1f..15f
                    )
                }

                // Outline Color Pickers
                val outlineColors = listOf("#000000", "#FFFFFF", "#FF00E6", "#00E5FF", "#00FF90")
                Column {
                    Text("Warna Garis Tepi", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        outlineColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentProj.outlineColor.equals(hex, true)) 2.dp else 1.dp,
                                        color = if (currentProj.outlineColor.equals(hex, true)) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateActiveProject { it.copy(outlineColor = hex) }
                                    }
                            )
                        }
                        
                        // Manual Hex input
                        var customColorText by remember(currentProj.outlineColor) { mutableStateOf(currentProj.outlineColor) }
                        OutlinedTextField(
                            value = customColorText,
                            onValueChange = { valText ->
                                customColorText = valText
                                if (valText.startsWith("#") && (valText.length == 7 || valText.length == 9)) {
                                    try {
                                        android.graphics.Color.parseColor(valText)
                                        viewModel.updateActiveProject { it.copy(outlineColor = valText) }
                                    } catch (e: Exception) {}
                                }
                            },
                            label = { Text("Hex") },
                            maxLines = 1,
                            singleLine = true,
                            modifier = Modifier.width(100.dp).height(50.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

data class AspectRatioPreset(
    val ratio: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val resolutions: List<Triple<Int, Int, String>>
)

@Composable
fun ExportTabContent(viewModel: ProjectViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val renderState by viewModel.renderState.collectAsState()
    val context = LocalContext.current

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Memuat proyek...")
        }
        return
    }

    val currentProj = project!!
    val scrollState = rememberScrollState()

    var selectedResolutionWidth by remember { mutableStateOf(currentProj.resolutionWidth) }
    var selectedResolutionHeight by remember { mutableStateOf(currentProj.resolutionHeight) }

    var selectedAspectRatio by remember {
        mutableStateOf(
            if (currentProj.resolutionWidth == currentProj.resolutionHeight) "1:1"
            else if (currentProj.resolutionWidth > currentProj.resolutionHeight) "16:9"
            else if (currentProj.resolutionWidth * 1.3f < currentProj.resolutionHeight && currentProj.resolutionWidth * 1.5f > currentProj.resolutionHeight) "4:5"
            else "9:16"
        )
    }

    var isWatermarkMode by remember { mutableStateOf(false) }
    var bgVideoPath by remember { mutableStateOf<String?>(null) }
    var bgVideoName by remember { mutableStateOf("Gunakan Pola Mandelbrot Bawaan (Default)") }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val cachedFile = File(context.cacheDir, "user_watermark_bg.mp4")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cachedFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    bgVideoPath = cachedFile.absolutePath
                    bgVideoName = "Video Kustom Terpilih"
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat video: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Setelan Dimensi & Rasio Ekspor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Aspect Ratio Selector Cards (Horizontal row)
        val aspectRatios = listOf(
            AspectRatioPreset("9:16", "TikTok / Reels", Icons.Default.PhoneAndroid, listOf(
                Triple(1080, 1920, "1080p Portrait HD (Sangat Tajam)"),
                Triple(720, 1280, "720p Portrait SD (Disarankan)"),
                Triple(480, 854, "480p Portrait SD (Kecil)")
            )),
            AspectRatioPreset("1:1", "Instagram / Feed", Icons.Default.CropSquare, listOf(
                Triple(1080, 1080, "1080p Square HD (Tajam)"),
                Triple(720, 720, "720p Square SD (Disarankan)"),
                Triple(480, 480, "480p Square SD (Ringan)")
            )),
            AspectRatioPreset("16:9", "YouTube / Landscape", Icons.Default.Tv, listOf(
                Triple(1920, 1080, "1080p Landscape HD (Lebar)"),
                Triple(1280, 720, "720p Landscape SD (Standar)"),
                Triple(854, 480, "480p Landscape SD (Kecil)")
            )),
            AspectRatioPreset("4:5", "Portrait Feed", Icons.Default.Portrait, listOf(
                Triple(1080, 1350, "1080p Portrait HD"),
                Triple(720, 900, "720p Portrait SD"),
                Triple(480, 600, "480p Portrait SD (Ringan)")
            ))
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(aspectRatios) { ratioObj ->
                val isSelected = selectedAspectRatio == ratioObj.ratio
                Card(
                    onClick = {
                        selectedAspectRatio = ratioObj.ratio
                        // Auto-select the default recommended resolution for this aspect ratio (the 2nd item, i.e. 720p)
                        val defaultRes = ratioObj.resolutions[1]
                        selectedResolutionWidth = defaultRes.first
                        selectedResolutionHeight = defaultRes.second
                        viewModel.updateActiveProject { 
                            it.copy(resolutionWidth = defaultRes.first, resolutionHeight = defaultRes.second) 
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ratioObj.icon,
                            contentDescription = ratioObj.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ratioObj.ratio,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ratioObj.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Pilih Resolusi Hasil Render",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val currentPreset = aspectRatios.firstOrNull { it.ratio == selectedAspectRatio } ?: aspectRatios[0]
        val resolutions = currentPreset.resolutions

        resolutions.forEach { (w, h, desc) ->
            val isSelected = selectedResolutionWidth == w && selectedResolutionHeight == h
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedResolutionWidth = w
                        selectedResolutionHeight = h
                        viewModel.updateActiveProject { it.copy(resolutionWidth = w, resolutionHeight = h) }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${w}x${h}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = desc, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isSelected) {
                        Text(
                            text = "Terpilih",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- FITUR WATERMARK VIDEO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Watermark Video",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gabungkan teks animasi di atas video latar belakang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isWatermarkMode,
                        onCheckedChange = { isWatermarkMode = it }
                    )
                }

                if (isWatermarkMode) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    
                    Text(
                        text = "Sumber Video Latar Belakang:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "Pilih Video")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih Video Galeri")
                        }
                        
                        if (bgVideoPath != null) {
                            IconButton(onClick = {
                                bgVideoPath = null
                                bgVideoName = "Gunakan Pola Mandelbrot Bawaan (Default)"
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Pilihan",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (bgVideoPath != null) Icons.Default.MovieFilter else Icons.Default.AutoAwesome,
                                contentDescription = "Status Video",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = bgVideoName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Red "RENDER MP4" button
        Button(
            onClick = {
                viewModel.startVideoExport(
                    width = selectedResolutionWidth,
                    height = selectedResolutionHeight,
                    isWatermarkMode = isWatermarkMode,
                    bgVideoPath = bgVideoPath
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("render_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF3B30), // Professional vibrant red
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = renderState !is RenderState.GeneratingFrames && renderState !is RenderState.CompilingVideo
        ) {
            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "Ekspor MP4")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MULAI RENDER MP4 (Lokal FFmpeg)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Live status container for video compile
        AnimatedVisibility(visible = renderState != RenderState.Idle) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = renderState) {
                        is RenderState.GeneratingFrames -> {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Menghasilkan Bingkai Gambar...",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Frame ${state.current} dari ${state.total}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is RenderState.CompilingVideo -> {
                            CircularProgressIndicator(color = Color(0xFFFF9500))
                            Text(
                                text = "Mengkompilasi Berkas Video (FFmpeg)...",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9500)
                            )
                            Text(
                                text = "Sedang menyatukan gambar menjadi MP4 kualitas tinggi.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        is RenderState.Success -> {
                            Text(
                                text = "Video Berhasil Dirender!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88)
                            )
                            Text(
                                text = "Berkas disimpan di:\n${state.filePath}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // True Native Video Preview Player built into the layout!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .background(Color.Black, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(state.filePath)
                                            val mc = MediaController(ctx)
                                            mc.setAnchorView(this)
                                            setMediaController(mc)
                                            start()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Share / Export file
                            Button(
                                onClick = {
                                    val videoFile = File(state.filePath)
                                    val uri = Uri.parse(videoFile.absolutePath)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Bagikan Hasil Video"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Bagikan")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bagikan Berkas Video MP4")
                            }

                            Button(
                                onClick = { viewModel.resetRenderState() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Render Baru")
                            }
                        }
                        is RenderState.Error -> {
                            Text(
                                text = "Gagal Merender Video",
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { viewModel.resetRenderState() }) {
                                Text("Coba Lagi")
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CollaborationTabContent(viewModel: ProjectViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val messages by viewModel.discussionMessages.collectAsState()
    val notifications by viewModel.teamNotifications.collectAsState()
    
    val userName by viewModel.currentUserName.collectAsState()
    val userEmail by viewModel.currentUserEmail.collectAsState()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Memuat data kolaborasi...")
        }
        return
    }

    val currentProj = project!!
    val cloudId = currentProj.cloudProjectId ?: "proyek_${currentProj.id}"

    var chatMessageText by remember { mutableStateOf("") }
    var showSetupDialog by remember { mutableStateOf(false) }
    var setupCloudId by remember { mutableStateOf(cloudId) }
    var setupTeamName by remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()

    // Auto-scroll to end of chat when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Collaboration setup status panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentProj.isCloudSynced) Icons.Default.CloudDone else Icons.Default.Cloud,
                        contentDescription = "Cloud",
                        tint = if (currentProj.isCloudSynced) Color(0xFF00FF88) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentProj.isCloudSynced) "Kolaborasi Tim Aktif" else "Simulasi Kolaborasi Offline",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = if (currentProj.isCloudSynced) {
                        "Semua setelan teks dan perubahan properti disinkronkan secara real-time ke awan. Bagikan kode ID proyek ini kepada rekan tim Anda."
                    } else {
                        "Koneksi awan berjalan dalam mode simulasi cerdas secara lokal. Anda tetap dapat menguji fitur papan diskusi, sistem notifikasi, dan obrolan tim."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID Proyek: $cloudId",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { showSetupDialog = true }) {
                        Text(if (currentProj.isCloudSynced) "Atur Ulang ID" else "Aktifkan Sinkronisasi")
                    }
                }
            }
        }

        // Two sub-sections: Timeline activity notifications, or Discussion Chat.
        var displayChatTab by remember { mutableStateOf(true) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { displayChatTab = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (displayChatTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (displayChatTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Diskusi Tim")
            }

            Button(
                onClick = { displayChatTab = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!displayChatTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!displayChatTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "Aktivitas")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aktivitas Tim")
            }
        }

        if (displayChatTab) {
            // Live Discussion chat list view
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada diskusi. Kirim pesan pertama untuk berkolaborasi!", textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(messages) { msg ->
                        val isSelf = msg.senderEmail.equals(userEmail, true)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                            ) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = if (isSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Input field and send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatMessageText,
                    onValueChange = { chatMessageText = it },
                    placeholder = { Text("Tulis pesan tim...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (chatMessageText.isNotBlank()) {
                            viewModel.postDiscussionMessage(chatMessageText)
                            chatMessageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim", tint = Color.White)
                }
            }
        } else {
            // Live team notifications timeline view
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (notifications.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada pembaruan aktivitas.", textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(notifications) { notif ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${notif.actorName} ${notif.actionText}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(notif.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    // Collaboration setup dialog
    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = { Text("Atur Sinkronisasi Awan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Nama Profil Kolaborasi",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { viewModel.currentUserName.value = it },
                        label = { Text("Nama Anggota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { viewModel.currentUserEmail.value = it },
                        label = { Text("Surel / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Kode ID Awan Proyek",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = setupCloudId,
                        onValueChange = { setupCloudId = it },
                        label = { Text("Kode ID Awan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = setupTeamName,
                        onValueChange = { setupTeamName = it },
                        label = { Text("Nama Tim / Instansi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setupCloudId.isNotBlank()) {
                            viewModel.enableCloudCollaboration(setupCloudId, setupTeamName)
                            showSetupDialog = false
                        }
                    }
                ) {
                    Text("Hubungkan")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSetupDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProjectsTabContent(
    viewModel: ProjectViewModel,
    onCreateProjectClick: () -> Unit
) {
    val projects by viewModel.localProjects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var projectToDelete by remember { mutableStateOf<AnimationProject?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Belum Ada Proyek",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Semua proyek edit kinetic text Anda akan tersimpan otomatis di sini untuk mencegah hilangnya data.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateProjectClick) {
                    Text("Mulai Proyek Pertama")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Daftar Proyek Tersimpan Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(projects) { proj ->
                    val isActive = activeProject?.id == proj.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProject(proj) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = proj.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (proj.isCloudSynced) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Awan",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "\"${proj.text}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Gaya: ${proj.animType} | Kecepatan: ${proj.speed}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Simpan otomatis: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(proj.lastUpdated)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { projectToDelete = proj }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Proyek",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
                
                // Add spacer so list doesn't get cut by bottom navigation
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // FAB to quickly add new projects
        FloatingActionButton(
            onClick = onCreateProjectClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .padding(bottom = 56.dp) // Offset bottom nav
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Proyek Baru")
        }
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        val proj = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Hapus Proyek") },
            text = { Text("Apakah Anda yakin ingin menghapus proyek '${proj.name}'? Berkas yang disimpan secara lokal akan hilang selamanya.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                Button(
                    onClick = { projectToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
