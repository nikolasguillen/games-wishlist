package com.example.gameswishlist.core.common

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType

/**
 * Calculates a relevance score for a game based on a weighted popularity and content-type algorithm.
 *
 * The algorithm combines several components:
 * 1. Bayesian Average: Balances rating and rating count.
 * 2. Hype Factor: Boosts anticipated games.
 * 3. Type Penalty/Bonus: Prioritizes MAIN_GAME and deprioritizes noisy types.
 * 4. Keyword Noise Penalty: Deprioritizes editions/packs that clutter search results.
 * 5. Text Match Multiplier: Boosts games that match the search query closely.
 */
fun calculateGameRelevanceScore(game: Game, query: String = ""): Double {
    // Component 1: Bayesian Average
    val voteCount = game.ratingCount.toDouble()
    val averageRating = game.rating
    val minVotesThreshold = 10.0
    val globalMeanRating = 50.0
    
    val bayesianScore = if (voteCount > 0) {
        (voteCount * averageRating + minVotesThreshold * globalMeanRating) / (voteCount + minVotesThreshold)
    } else {
        0.0
    }

    // Component 2: Hype Factor
    val hypeMultiplier = 2.0
    val hypeBonus = game.hypes.toDouble() * hypeMultiplier

    // Component 3: Type Weight
    val typeWeight = when (game.gameType) {
        GameType.MAIN_GAME -> 1.5
        GameType.REMAKE, GameType.REMASTER -> 1.2
        GameType.DLC_ADDON, GameType.EXPANSION, GameType.STANDALONE_EXPANSION -> 0.8
        else -> 0.5 // Penalty for BUNDLE, PACK, UPDATE, etc.
    }

    // Component 4: Keyword Noise Penalty
    // Many "Main Games" in IGDB are actually collectors editions or bundles labeled incorrectly.
    val noiseKeywords = listOf(
        "Edition", "Pack", "Port", "Bundle", "Collection", "Legacy", 
        "Complete", "Goty", "Game of the Year", "Special", "Collector", "Variety"
    )
    
    var noisePenalty = 1.0
    if (noiseKeywords.any { game.name.contains(it, ignoreCase = true) }) {
        noisePenalty = 0.6 // Reduce score by 40% if it looks like noise
    }

    // Component 5: Text Match Multiplier
    var matchMultiplier = 1.0
    if (query.isNotBlank()) {
        val name = game.name.lowercase()
        val q = query.lowercase().trim()
        
        when {
            name == q -> matchMultiplier = 8.0
            name.startsWith(q) -> matchMultiplier = 4.0
            name.contains(q) -> matchMultiplier = 2.0
        }
        
        // Bonus for exact word match if it's not just "contained"
        // e.g. "Halo" in "Halo Infinite" vs "Halogen"
        if (name.split(" ", "-", ":").any { it == q }) {
            matchMultiplier *= 1.5
        }
    }

    return (bayesianScore + hypeBonus) * typeWeight * noisePenalty * matchMultiplier
}
