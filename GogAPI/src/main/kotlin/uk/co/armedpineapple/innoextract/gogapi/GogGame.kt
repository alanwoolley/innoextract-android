package uk.co.armedpineapple.innoextract.gogapi

import java.net.URL

/**
 * Details about a game on GOG.
 */
data class GogGame(
    /**
     * Background image URL.
     */
    val backgroundImg: URL,
    /**
     * Logo image URL.
     */
    val logoImg: URL,
    /**
     * The game title.
     */
    val title: String
)
