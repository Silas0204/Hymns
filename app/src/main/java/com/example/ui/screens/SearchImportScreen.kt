package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.SongViewModel
import com.example.util.ChordTransposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchImportScreen(
    viewModel: SongViewModel,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchModeUrl by viewModel.isSearchModeUrl.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val songPreview by viewModel.searchedSongPreview.collectAsState()

    var previewTransposeOffset by remember { mutableStateOf(0) }
    var useFlatsInPreview by remember { mutableStateOf(false) }

    // Clear previews when entering/exiting this screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearchPreview()
            viewModel.clearSearchError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Chords Finder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Find any chords online or scrape lyrics and chords directly from Chrome browser URLs using Gemini AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tabs/Toggle for mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Topic search mode button
                FilterChip(
                    selected = !isSearchModeUrl,
                    onClick = { viewModel.toggleSearchMode(false) },
                    label = { Text("Search by Keywords") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag("mode_keywords_chip")
                )
                
                // URL extraction mode button
                FilterChip(
                    selected = isSearchModeUrl,
                    onClick = { viewModel.toggleSearchMode(true) },
                    label = { Text("Chrome URL Extractor") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag("mode_url_chip")
                )
            }

            // Input TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { 
                    Text(if (isSearchModeUrl) "Paste chords website URL here..." else "Enter artist and song title...") 
                },
                placeholder = { 
                    Text(if (isSearchModeUrl) "e.g. https://www.ultimate-guitar.com/..." else "e.g. Creep Radiohead") 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("search_input_field"),
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(
                        imageVector = if (isSearchModeUrl) Icons.Default.Link else Icons.Default.MusicNote,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
                singleLine = true
            )

            // Action Button
            Button(
                onClick = { viewModel.searchSong() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("trigger_search_button"),
                enabled = searchQuery.isNotBlank() && !searchLoading,
                shape = MaterialTheme.shapes.large
            ) {
                if (searchLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Searching & Parsing cords...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSearchModeUrl) "Extract with Gemini" else "Search chords with Gemini")
                }
            }

            // Error display
            searchError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, 
                            contentDescription = "Error", 
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Preview Display
            songPreview?.let { preview ->
                // Visual Divider
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "AI Generated Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Title / Artist / Key row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preview.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = preview.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Estim key badge
                            val currentRepresentedKey = ChordTransposer.transposeNote(
                                preview.originalKey, 
                                previewTransposeOffset, 
                                useFlatsInPreview
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Text(
                                        text = currentRepresentedKey,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    "Key",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview Transpose controller bar
                        Text(
                            "Try transposing before saving:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { 
                                    previewTransposeOffset = if (previewTransposeOffset <= -11) 12 else previewTransposeOffset - 1 
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Transpose down")
                            }

                            Text(
                                text = if (previewTransposeOffset >= 0) "+$previewTransposeOffset" else "$previewTransposeOffset",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            FilledIconButton(
                                onClick = { 
                                    previewTransposeOffset = if (previewTransposeOffset >= 12) -11 else previewTransposeOffset + 1 
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Transpose up")
                            }

                            Button(
                                onClick = { 
                                    previewTransposeOffset = 0 
                                    useFlatsInPreview = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            IconButton(onClick = { useFlatsInPreview = !useFlatsInPreview }) {
                                Icon(
                                    imageVector = if (useFlatsInPreview) Icons.Default.MusicNote else Icons.Default.MusicVideo,
                                    contentDescription = "Toggle #/b",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chord preview sheet window block
                        Surface(
                            shadowElevation = 1.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val transposedContent = ChordTransposer.transposeSongContent(
                                    preview.content,
                                    previewTransposeOffset,
                                    useFlatsInPreview
                                )

                                transposedContent.lines().forEach { line ->
                                    val parts = ChordTransposer.parseLineSimplified(line)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 2.dp)
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
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                } else {
                                                    // Placeholder for alignment spacing
                                                    Text(
                                                        text = " ",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                                // Lyrics text syllable
                                                Text(
                                                    text = part.text.ifEmpty { " " },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Final Save Button
                        Button(
                            onClick = {
                                // Save with transposition offset applied and flattened if desired,
                                // or original with preview offset saved. 
                                // Saving with previewTransposeOffset applied directly is highly intuitive!
                                val finalContent = ChordTransposer.transposeSongContent(
                                    preview.content,
                                    previewTransposeOffset,
                                    useFlatsInPreview
                                )
                                val finalKey = ChordTransposer.transposeNote(
                                    preview.originalKey,
                                    previewTransposeOffset,
                                    useFlatsInPreview
                                )
                                
                                viewModel.saveSong(
                                    title = preview.title,
                                    artist = preview.artist,
                                    originalKey = finalKey,
                                    content = finalContent
                                )
                                // Clear preview and go back
                                viewModel.clearSearchPreview()
                                viewModel.setSearchQuery("")
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_preview_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to My Library")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

