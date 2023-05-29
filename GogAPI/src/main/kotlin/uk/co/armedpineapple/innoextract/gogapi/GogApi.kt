package uk.co.armedpineapple.innoextract.gogapi

/**
 * An API for GOG.
 */
interface GogApi {
    /**
     * Queries the API for details about a game.
     *
     * @param gameId The ID of the game
     *
     * @return A GogGame populated with details about the game.
     */
    suspend fun getGameDetails(gameId: Long): GogGame
}
