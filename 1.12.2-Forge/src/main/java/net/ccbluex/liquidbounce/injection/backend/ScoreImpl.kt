/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScore
import net.minecraft.scoreboard.Score

class ScoreImpl(val wrapped: Score) : IScore {
    override val scorePoints: Int
        get() = wrapped.scorePoints
    override val playerName: String
        get() = wrapped.playerName


    override fun equals(other: Any?): Boolean {
        return other is ScoreImpl && other.wrapped == this.wrapped
    }
}

inline fun IScore.unwrap(): Score = (this as ScoreImpl).wrapped
inline fun Score.wrap(): IScore = ScoreImpl(this)