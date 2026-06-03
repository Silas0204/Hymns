package com.example.util

data class LyricPart(val chord: String, val text: String)

object ChordTransposer {
    private val SHARPS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLATS = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_TO_INDEX: Map<String, Int> = buildMap {
        SHARPS.forEachIndexed { index, note -> put(note, index) }
        FLATS.forEachIndexed { index, note -> put(note, index) }
    }

    /**
     * Transposes a single note string by a given semitone offset.
     * Uses flat or sharp representation based on useFlats.
     */
    fun transposeNote(note: String, semitones: Int, useFlats: Boolean = false): String {
        val index = NOTE_TO_INDEX[note] ?: return note
        val newIndex = (index + semitones).mod(12)
        return if (useFlats) FLATS[newIndex] else SHARPS[newIndex]
    }

    /**
     * Parses and transposes a chord symbol (e.g., "C#m7/G#") by a given semitone offset.
     */
    fun transposeChord(chord: String, semitones: Int, useFlats: Boolean = false): String {
        if (chord.isBlank()) return chord
        
        // Handle slash chords like C/E, Am/G, C#m7/F#
        val parts = chord.split("/")
        if (parts.size > 1) {
            val mainChord = parts[0]
            val slashNote = parts[1]
            val transposedMain = transposeChord(mainChord, semitones, useFlats)
            val transposedSlash = transposeNote(slashNote, semitones, useFlats)
            return "$transposedMain/$transposedSlash"
        }

        // Find the root note. It can be a 2-char note (C#, Db, etc.) or a 1-char note.
        val root: String
        val suffix: String
        if (chord.length >= 2 && NOTE_TO_INDEX.containsKey(chord.substring(0, 2))) {
            root = chord.substring(0, 2)
            suffix = chord.substring(2)
        } else if (chord.isNotEmpty() && NOTE_TO_INDEX.containsKey(chord.substring(0, 1))) {
            root = chord.substring(0, 1)
            suffix = chord.substring(1)
        } else {
            return chord
        }

        val transposedRoot = transposeNote(root, semitones, useFlats)
        return transposedRoot + suffix
    }

    /**
     * Replaces all bracketed chords in the song content with their transposed equivalents.
     */
    fun transposeSongContent(content: String, semitones: Int, useFlats: Boolean = false): String {
        if (semitones == 0) return content
        
        val regex = "\\[([^\\]]+)\\]".toRegex()
        return regex.replace(content) { matchResult ->
            val chord = matchResult.groups[1]?.value ?: ""
            val transposedChord = transposeChord(chord, semitones, useFlats)
            "[$transposedChord]"
        }
    }

    /**
     * Parses a line of lyrics with bracketed inline chords into a structured list of LyricPart.
     * Keeps spacing intact for columns alignment.
     */
    fun parseLineSimplified(line: String): List<LyricPart> {
        if (line.isBlank()) return listOf(LyricPart("", " "))
        val result = mutableListOf<LyricPart>()
        var currentText = java.lang.StringBuilder()
        var currentChord = ""
        var i = 0
        while (i < line.length) {
            if (line[i] == '[') {
                if (currentText.isNotEmpty() || currentChord.isNotEmpty()) {
                    result.add(LyricPart(currentChord, currentText.toString()))
                    currentText = java.lang.StringBuilder()
                }
                val endBracket = line.indexOf(']', i)
                if (endBracket != -1) {
                    currentChord = line.substring(i + 1, endBracket)
                    i = endBracket + 1
                } else {
                    currentText.append(line.substring(i))
                    break
                }
            } else {
                currentText.append(line[i])
                i++
            }
        }
        if (currentText.isNotEmpty() || currentChord.isNotEmpty()) {
            result.add(LyricPart(currentChord, currentText.toString()))
        }
        return result
    }
}
