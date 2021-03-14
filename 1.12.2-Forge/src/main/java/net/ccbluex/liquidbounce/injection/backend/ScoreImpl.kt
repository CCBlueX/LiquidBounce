/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScore
import net.minecraft.scoreboard.Score

class ScoreImpl(val wrapped: Score) : IScore
{
	override val scorePoints: Int
		get() = wrapped.scorePoints
	override val playerName: String
		get() = wrapped.playerName

	override fun equals(other: Any?): Boolean = other is ScoreImpl && other.wrapped == wrapped
}

fun IScore.unwrap(): Score = (this as ScoreImpl).wrapped
fun Score.wrap(): IScore = ScoreImpl(this)
