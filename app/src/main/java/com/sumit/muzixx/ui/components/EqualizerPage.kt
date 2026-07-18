package com.sumit.muzixx.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.glassEffect
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerPage(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val isSettingsInit = viewModel.isSettingsInitialized()

    val isEqEnabled = if (isSettingsInit) viewModel.settings.eqEnabled else false
    val bassEnabled = if (isSettingsInit) viewModel.settings.bassEnabled else false
    val bassBoost = if (isSettingsInit) viewModel.settings.bassStrength else 0.0f
    val selectedPresetIndex = if (isSettingsInit) viewModel.settings.eqPresetIndex else 0

    val standardBands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    val bandValues = remember { mutableStateListOf<Float>() }

    LaunchedEffect(isSettingsInit, viewModel.settings.eqBands.size) {
        if (isSettingsInit && viewModel.settings.eqBands.isNotEmpty()) {
            bandValues.clear()
            bandValues.addAll(viewModel.settings.eqBands)
        } else if (bandValues.isEmpty()) {
            bandValues.addAll(listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
        }
    }

    val presets = listOf("Default", "Classic", "Pop", "Rock", "Jazz", "Custom")
    var selectedPreset by remember(selectedPresetIndex) {
        mutableStateOf(if (selectedPresetIndex in presets.indices && selectedPresetIndex != -1) presets[selectedPresetIndex] else "Custom")
    }

    val accentColor = MaterialTheme.colorScheme.primary

    var localBassBoost by remember(bassBoost) { mutableFloatStateOf(bassBoost) }
    var loudness by remember { mutableFloatStateOf(0.0f) }
    var loudnessEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(localBassBoost) {
        if (localBassBoost == bassBoost) return@LaunchedEffect
        delay(30.milliseconds)
        viewModel.setBassBoostStrength(localBassBoost)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isEqEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEqEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isEqEnabled,
                            onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(28.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        bandValues.forEach { valDb ->
                            Text(
                                text = String.format("%+.1f", valDb),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isEqEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        val unselectedDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isEqEnabled, bandValues.size) {
                                    if (!isEqEnabled || bandValues.isEmpty()) return@pointerInput
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        selectedPreset = "Custom"
                                        val colWidth = size.width / standardBands.size
                                        val idx = (change.position.x / colWidth).toInt().coerceIn(0, standardBands.size - 1)
                                        val pctY = (change.position.y / size.height).coerceIn(0f, 1f)

                                        val dbTarget = 15f - (pctY * 30f)
                                        if (idx in bandValues.indices) {
                                            bandValues[idx] = dbTarget
                                            viewModel.setBandLevel(idx, dbTarget)
                                        }
                                    }
                                }
                        ) {
                            val stepX = size.width / (standardBands.size + 1)
                            val pts = mutableListOf<Offset>()

                            for (i in standardBands.indices) {
                                if (i < bandValues.size) {
                                    val x = stepX * (i + 1)
                                    val pctY = (15f - bandValues[i]) / 30f
                                    pts.add(Offset(x, pctY * size.height))
                                }
                            }

                            pts.forEach { pt ->
                                drawLine(gridLineColor, Offset(pt.x, 0f), Offset(pt.x, size.height), strokeWidth = 2f)
                            }

                            if (pts.isNotEmpty() && isEqEnabled) {
                                val path = Path().apply { moveTo(pts.first().x, pts.first().y) }
                                for (i in 0 until pts.size - 1) {
                                    val p1 = pts[i]
                                    val p2 = pts[i + 1]
                                    path.cubicTo((p1.x + p2.x) / 2, p1.y, (p1.x + p2.x) / 2, p2.y, p2.x, p2.y)
                                }
                                drawPath(path, accentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }

                            pts.forEach { pt ->
                                drawCircle(if (isEqEnabled) accentColor else unselectedDotColor, radius = 5.dp.toPx(), center = pt)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        standardBands.forEach { b ->
                            Text(b, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presets.size) { index ->
                    val presetName = presets[index]
                    FilterChip(
                        selected = selectedPreset == presetName,
                        onClick = {
                            selectedPreset = presetName
                            val structuralValues = when (presetName) {
                                "Default" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                                "Classic" -> listOf(4.5f, 3.0f, 2.0f, 0f, -1.0f, -1.0f, 0f, 1.5f, 3.0f, 4.0f)
                                "Pop" -> listOf(-1.5f, -1.0f, 0f, 2.0f, 4.0f, 4.5f, 3.0f, 1.0f, -1.0f, -1.5f)
                                "Rock" -> listOf(5.0f, 4.0f, 3.0f, 1.5f, -0.5f, -1.0f, 0.5f, 2.5f, 4.0f, 5.0f)
                                "Jazz" -> listOf(3.0f, 2.0f, 1.0f, 1.5f, -1.5f, -1.5f, 0f, 1.5f, 3.0f, 3.5f)
                                else -> null
                            }
                            structuralValues?.let { updatedProfile ->
                                bandValues.clear()
                                bandValues.addAll(updatedProfile)
                                updatedProfile.forEachIndexed { bandIdx, dbVal ->
                                    viewModel.setBandLevel(bandIdx, dbVal)
                                }
                                viewModel.setEqualizerPresetLive(index.toShort())
                            }
                        },
                        label = { Text(presetName, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedPreset == presetName,
                            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            selectedBorderColor = accentColor.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(244.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpressiveVerticalSliderCard(
                    title = "Bass Boost",
                    value = localBassBoost,
                    isEnabled = bassEnabled,
                    accent = accentColor,
                    onValChange = { localBassBoost = it },
                    onToggle = { viewModel.setBassBoostEnabled(it) },
                    modifier = Modifier.weight(1f)
                )
                ExpressiveVerticalSliderCard(
                    title = "Loudness",
                    value = loudness,
                    isEnabled = loudnessEnabled,
                    accent = accentColor,
                    onValChange = { loudness = it },
                    onToggle = { loudnessEnabled = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = viewModel.currentVolume,
                            onValueChange = { viewModel.setMasterVolume(it) },
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                        )
                    }
                    Text(
                        text = "${(viewModel.currentVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveVerticalSliderCard(
    title: String,
    value: Float,
    isEnabled: Boolean,
    accent: Color,
    onValChange: (Float) -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .glassEffect(shape = RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = value,
                    onValueChange = onValChange,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    modifier = Modifier
                        .rotate(270f)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(
                                    -placeable.width / 2 + placeable.height / 2,
                                    -placeable.height / 2 + placeable.width / 2
                                )
                            }
                        }
                        .width(130.dp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}