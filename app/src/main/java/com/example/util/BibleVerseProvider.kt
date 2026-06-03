package com.example.util

data class BibleVerse(val citation: String, val text: String)

object BibleVerseProvider {
    val VERSES = listOf(
        BibleVerse("Colossians 3:16", "Let the word of Christ dwell in you richly, teaching and admonishing one another in all wisdom, singing psalms and hymns and spiritual songs, with thankfulness in your hearts to God."),
        BibleVerse("Psalm 105:2", "Sing to him, sing praises to him; tell of all his wondrous works!"),
        BibleVerse("Psalm 95:1", "Oh come, let us sing to the Lord; let us make a joyful noise to the rock of our salvation!"),
        BibleVerse("Ephesians 5:19", "Addressing one another in psalms and hymns and spiritual songs, singing and making melody to the Lord with your heart."),
        BibleVerse("Psalm 104:33", "I will sing to the Lord as long as I live; I will sing praise to my God while I have my being."),
        BibleVerse("Psalm 47:6", "Sing praises to God, sing praises! Sing praises to our King, sing praises!"),
        BibleVerse("Psalm 98:4", "Make a joyful noise to the Lord, all the earth; break forth into joyous song and sing praises!"),
        BibleVerse("Psalm 28:7", "The Lord is my strength and my shield; in him my heart trusts, and I am helped; my heart exults, and with my song I give thanks to him."),
        BibleVerse("Psalm 57:7", "My heart is steadfast, O God, my heart is steadfast! I will sing and make melody!"),
        BibleVerse("Psalm 147:1", "Praise the Lord! For it is good to sing praises to our God; for it is pleasant, and a song of praise is fitting.")
    )

    fun getRandomVerse(): BibleVerse {
        return VERSES.random()
    }
}
