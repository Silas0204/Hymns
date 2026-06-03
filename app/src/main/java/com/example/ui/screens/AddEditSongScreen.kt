package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSongScreen(
    viewModel: SongViewModel,
    songId: Int? = null,
    onBack: () -> Unit
) {
    val songs by viewModel.savedSongs.collectAsState()
    val isEditMode = songId != null
    val targetSong = if (songId != null) songs.find { it.id == songId } else null

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var originalKey by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }

    // Keyboard insertion helper: Common chords
    val quickChords = listOf("C", "G", "D", "A", "E", "F", "Am", "Dm", "Em", "Bm", "Bb", "F#m")

    LaunchedEffect(targetSong) {
        if (targetSong != null) {
            title = targetSong.title
            artist = targetSong.artist
            originalKey = targetSong.originalKey
            contentValue = TextFieldValue(targetSong.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Song" else "New Song", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    placeholder = { Text("e.g. Blowin' in the Wind") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("edit_title_input"),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Artist
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artist / Band") },
                        placeholder = { Text("e.g. Bob Dylan") },
                        modifier = Modifier
                            .weight(2f)
                            .padding(bottom = 12.dp)
                            .testTag("edit_artist_input"),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )

                    // Estimated key
                    OutlinedTextField(
                        value = originalKey,
                        onValueChange = { originalKey = it },
                        label = { Text("Original Key") },
                        placeholder = { Text("e.g. G") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                            .testTag("edit_key_input"),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                Text(
                    text = "Lyrics & Chords (Bracket format)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Insert chords directly in-line using brackets prior to the lyric syllables, e.g. 'Amazing [G]Grace how [C]sweet...'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Quick Chord Assistant row (above the Editor sheet textbox)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChords.forEach { chord ->
                        Button(
                            onClick = {
                                val text = contentValue.text
                                val sel = contentValue.selection
                                val start = sel.start
                                val end = sel.end
                                val newText = text.substring(0, start) + "[$chord]" + text.substring(end)
                                val newSel = TextRange(start + chord.length + 2)
                                contentValue = TextFieldValue(newText, newSel)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("helper_chord_${chord}")
                        ) {
                            Text(chord, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Main Song sheet editor area
                OutlinedTextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    placeholder = { 
                        Text("[G]Amazing grace! How [C]sweet the [G]sound...\nThat [G]saved a wretch like [D]me!\n\nWrite your chords and lyrics here.") 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .testTag("edit_content_input"),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Save Buttons
            Button(
                onClick = {
                    if (isEditMode && targetSong != null) {
                        viewModel.updateSong(
                            id = targetSong.id,
                            title = title,
                            artist = artist,
                            originalKey = originalKey,
                            content = contentValue.text
                        )
                    } else {
                        viewModel.saveSong(
                            title = title,
                            artist = artist,
                            originalKey = originalKey,
                            content = contentValue.text
                        )
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp)
                    .testTag("save_song_button"),
                shape = MaterialTheme.shapes.large,
                enabled = title.isNotBlank() && contentValue.text.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Update Song" else "Save to Library Structure")
            }
        }
    }
}
