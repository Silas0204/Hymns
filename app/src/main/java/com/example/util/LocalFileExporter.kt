package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.SongEntity
import java.io.File
import java.io.FileOutputStream

object LocalFileExporter {
    private const val TAG = "LocalFileExporter"

    /**
     * Gets or creates the 'HOS' synchronized documents directory in the app's external files directory.
     * This directory is public to user file explorers under:
     * Android/data/com.example/files/HOS/
     */
    fun getHOSDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "HOS")
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.d(TAG, "Created HOS storage directory: $created at ${dir.absolutePath}")
        }
        return dir
    }

    /**
     * Clears illegal filename characters to prevent saving failures.
     */
    fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
    }

    /**
     * Saves a song sheet to the local device storage as:
     * 1. A Word-compatible Rich Document (.doc) using standard formatted HTML.
     * 2. A plain text chord sheet (.txt) with brackets in-tact.
     */
    fun saveSongToDevice(context: Context, song: SongEntity): List<File> {
        val storageDir = getHOSDirectory(context)
        val baseName = sanitizeFileName("${song.title}_${song.artist}")
        
        val docFile = File(storageDir, "$baseName.doc") // .doc extension opens cleanly in Word as HTML
        val docxFile = File(storageDir, "$baseName.docx") // Saving as .docx as well so users see Word icon
        val txtFile = File(storageDir, "$baseName.txt")

        val generatedFiles = mutableListOf<File>()

        try {
            // 1. Generate Word Document (.doc / html frame)
            val docHtmlContent = buildDocHtml(song)
            FileOutputStream(docFile).use { fos ->
                fos.write(docHtmlContent.toByteArray(Charsets.UTF_8))
            }
            generatedFiles.add(docFile)

            // Also make a copy with .docx extension because users requested a Word document!
            FileOutputStream(docxFile).use { fos ->
                fos.write(docHtmlContent.toByteArray(Charsets.UTF_8))
            }
            generatedFiles.add(docxFile)

            // 2. Generate plain text chord sheet (.txt)
            val txtContent = buildTextSheet(song)
            FileOutputStream(txtFile).use { fos ->
                fos.write(txtContent.toByteArray(Charsets.UTF_8))
            }
            generatedFiles.add(txtFile)

            Log.d(TAG, "Successfully exported: ${docFile.name} and ${txtFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save song to local storage: ${e.message}", e)
        }

        return generatedFiles
    }

    /**
     * Returns the formatted HTML representation that opens as a styled document in Word processors.
     */
    fun buildDocHtml(song: SongEntity): String {
        // Parse brackets and make chords red and bold in Word
        val bodyContentBuilder = StringBuilder()
        song.content.lines().forEach { line ->
            val parts = ChordTransposer.parseLineSimplified(line)
            bodyContentBuilder.append("<div style='margin-bottom: 2px;'>")
            parts.forEach { part ->
                if (part.chord.isNotEmpty()) {
                    bodyContentBuilder.append("<span style='color: #d32f2f; font-weight: bold;'>[${part.chord}]</span>")
                }
                bodyContentBuilder.append("<span>${escapeHtml(part.text)}</span>")
            }
            bodyContentBuilder.append("</div>\n")
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${escapeHtml(song.title)}</title>
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 11pt;
                        line-height: 1.5;
                        color: #1a1a1a;
                        margin: 40px;
                    }
                    .header-panel {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                        border-bottom: 2px solid #e0e0e0;
                        padding-bottom: 12px;
                        margin-bottom: 24px;
                    }
                    .song-title {
                        font-size: 24pt;
                        font-weight: bold;
                        color: #2c3e50;
                        margin: 0 0 4px 0;
                    }
                    .song-artist {
                        font-size: 14pt;
                        color: #7f8c8d;
                        margin: 0 0 8px 0;
                    }
                    .key-badge {
                        display: inline-block;
                        background-color: #e3f2fd;
                        color: #0d47a1;
                        padding: 4px 12px;
                        border-radius: 4px;
                        font-size: 10pt;
                        font-weight: bold;
                    }
                    .sheet-content {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 11pt;
                        white-space: pre-wrap;
                        background-color: #fafafa;
                        border: 1px solid #f0f0f0;
                        padding: 20px;
                        border-radius: 6px;
                    }
                </style>
            </head>
            <body>
                <div class="header-panel">
                    <h1 class="song-title">${escapeHtml(song.title)}</h1>
                    <h2 class="song-artist">by ${escapeHtml(song.artist)}</h2>
                    <span class="key-badge">Original Key: ${escapeHtml(song.originalKey)}</span>
                </div>
                <div class="sheet-content">
                    ${bodyContentBuilder}
                </div>
                <p style="font-family: sans-serif; font-size: 8pt; color: #aaa; text-align: center; margin-top: 40px;">
                    Generated by HOS (Hymns of Silas) on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                </p>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds plain text sheet format
     */
    private fun buildTextSheet(song: SongEntity): String {
        return buildString {
            appendLine("=============================================")
            appendLine("SONG: ${song.title.uppercase()}")
            appendLine("ARTIST: ${song.artist}")
            appendLine("ORIGINAL KEY: ${song.originalKey}")
            appendLine("EXPORT DATE: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("=============================================")
            appendLine()
            appendLine(song.content)
            appendLine()
            appendLine("=============================================")
            appendLine("Generated via HOS (Hymns of Silas)")
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
