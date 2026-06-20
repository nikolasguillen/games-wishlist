package com.example.gameswishlist.core.common

import com.example.gameswishlist.core.model.Game

/**
 * Calculates a relevance score for a game based on a weighted popularity algorithm.
 *
 * The algorithm combines two main components:
 * 1. Bayesian Average: Balances the average rating with the number of ratings to ensure
 *    that games with high quality and high engagement are prioritized over those with
 *    few, potentially biased, ratings.
 * 2. Hype Factor: Adds a bonus for games that are highly anticipated (indicated by the 'hypes' count),
 *    allowing new or upcoming titles to rank higher even if they lack ratings.
 *
 * @param game The game model containing rating, ratingCount, and hypes.
 * @return A double representing the final relevance score.
 */
fun calculateGameRelevanceScore(game: Game): Double {
    // Component 1: Bayesian Average (Simplified)
    // Formula: (v * r + m * C) / (v + m)
    // v: number of ratings for the game (ratingCount)
    // r: average rating for the game (rating)
    // m: minimum ratings required to be considered (threshold, e.g., 10)
    // C: the average rating across the whole database (global mean, e.g., 50.0)
    
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
    // Multiplier for anticipated games to give them a boost in search results.
    val hypeMultiplier = 2.0
    val hypeBonus = game.hypes.toDouble() * hypeMultiplier

    return bayesianScore + hypeBonus
}
