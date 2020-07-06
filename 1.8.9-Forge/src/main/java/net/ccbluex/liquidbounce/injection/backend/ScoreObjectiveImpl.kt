/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreObjective
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreboard
import net.minecraft.scoreboard.ScoreObjective

class ScoreObjectiveImpl(val wrapped: ScoreObjective) : IScoreObjective {
    override val displayName: String
        get() = wrapped.displayName
    override val scoreboard: IScoreboard
        get() = wrapped.scoreboard.wrap()


    override fun equals(other: Any?): Boolean {
        return other is ScoreObjectiveImpl && other.wrapped == this.wrapped
    }
}

inline fun IScoreObjective.unwrap(): ScoreObjective = (this as ScoreObjectiveImpl).wrapped
inline fun ScoreObjective.wrap(): IScoreObjective = ScoreObjectiveImpl(this)