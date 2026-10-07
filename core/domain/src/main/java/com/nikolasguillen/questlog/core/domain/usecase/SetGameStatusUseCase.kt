package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.release.canSetStatus
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Sets or clears a game's [GameStatus]. Status changes go through here rather than [UpdateGameUseCase] so
 * the release rule is enforced in one place instead of by whichever screen offers the choice.
 *
 * Picking a status that [GameStatus.requiresRelease] on a game that is not out yet is ignored. Clearing is
 * always allowed, including a status the game no longer qualifies for because its release slipped after
 * the user picked it.
 */
class SetGameStatusUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * @param status The new status, or `null` to clear it.
     */
    suspend operator fun invoke(game: Game, status: GameStatus?, now: Instant = Clock.System.now()) {
        if (status != null && !game.canSetStatus(status, now)) return
        repository.updateGameDetails(game.copy(status = status))
    }
}
