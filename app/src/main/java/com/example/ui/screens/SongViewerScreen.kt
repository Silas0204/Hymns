package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.SongViewModel
import com.example.util.ChordTransposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongViewerScreen(
    viewModel: SongViewModel,
    songId: Int,
    onBack: () -> Unit,
    onEditSong: (Int) -> Unit
) {
    val songs by viewModel.savedSongs.collectAsState()
    val song = songs.find { it.id == songId }

    val transposeOffset by viewModel.transposeOffset.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()

    // Interactive Stage text sizing
    var fontSize by remember { mutableStateOf(16) }

    if (song == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Song not found")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Back to Library") }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("view_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEditSong(songId) },
                        modifier = Modifier.testTag("app_edit_song_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Song")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header: Artist / Key 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Artist: ${song.artist}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Original Key: ${song.originalKey}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Estimated active key badge
                val currentRepresentedKey = remember(song.originalKey, transposeOffset, useFlats) {
                    ChordTransposer.transposeNote(song.originalKey, transposeOffset, useFlats)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = currentRepresentedKey,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Controllers Panel Card: Transposition and Text size
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("viewer_controls_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Row 1: Transpose controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pitch Shift:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.width(90.dp)
                        )

                        FilledIconButton(
                            onClick = { viewModel.decrementTranspose() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(36.dp).testTag("decrement_transpose_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Slightly low pitch", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = if (transposeOffset >= 0) "+$transposeOffset" else "$transposeOffset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        FilledIconButton(
                            onClick = { viewModel.incrementTranspose() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(36.dp).testTag("increment_transpose_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Slightly high pitch", modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = { viewModel.resetTranspose() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.testTag("reset_transpose_btn")
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }

                        IconButton(
                            onClick = { viewModel.toggleUseFlats() },
                            modifier = Modifier.testTag("accidental_representation_toggle")
                        ) {
                            Icon(
                                imageVector = if (useFlats) Icons.Default.MusicNote else Icons.Default.MusicVideo,
                                contentDescription = "Flat/Sharp toggle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Row 2: Text Sizing stage controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Stage Zoom:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.width(90.dp)
                        )

                        FilledIconButton(
                            onClick = { if (fontSize > 12) fontSize -= 2 },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.size(36.dp).testTag("stage_zoom_out_btn")
                        ) {
                            Icon(Icons.Default.TextFormat, contentDescription = "Decrease size", modifier = Modifier.size(12.dp))
                        }

                        Text(
                            text = "${fontSize}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        FilledIconButton(
                            onClick = { if (fontSize < 28) fontSize += 2 },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.size(36.dp).testTag("stage_zoom_in_btn")
                        ) {
                            Icon(Icons.Default.TextFormat, contentDescription = "Increase size", modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(36.dp).width(110.dp)) // Aligns nicely
                    }
                }
            }

            // Primary Lyrics Sheet Scroll Workspace
            Surface(
                shadowElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val transposedContent = remember(song.content, transposeOffset, useFlats) {
                        ChordTransposer.transposeSongContent(song.content, transposeOffset, useFlats)
                    }

                    transposedContent.lines().forEach { line ->
                        val parts = ChordTransposer.parseLineSimplified(line)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 3.dp)
                        ) {
                            parts.forEach { part ->
                                Column(
                                    modifier = Modifier.padding(end = 4.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    if (part.chord.isNotEmpty()) {
                                        Text(
                                            text = part.chord,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = (fontSize - 1).sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    } else {
                                        Text(
                                            text = " ",
                                            fontSize = (fontSize - 1).sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = part.text.ifEmpty { " " },
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

