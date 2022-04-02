/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScore
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreObjective
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreboard
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.Scoreboard

class ScoreboardImpl(val wrapped: Scoreboard) : IScoreboard {
    override fun getPlayersTeam(name: String?): ITeam? = wrapped.getPlayersTeam(name)?.wrap()

    override fun getObjectiveInDisplaySlot(index: Int): IScoreObjective? = wrapped.getObjectiveInDisplaySlot(index)?.wrap()

    override fun getSortedScores(objective: IScoreObjective): Collection<IScore> = WrappedCollection(wrapped.getSortedScores(objective.unwrap()), IScore::unwrap, Score::wrap)

    override fun equals(other: Any?): Boolean {
        return other is ScoreboardImpl && other.wrapped == this.wrapped
    }
}

inline fun IScoreboard.unwrap(): Scoreboard = (this as ScoreboardImpl).wrapped
inline fun Scoreboard.wrap(): IScoreboard = ScoreboardImpl(this)