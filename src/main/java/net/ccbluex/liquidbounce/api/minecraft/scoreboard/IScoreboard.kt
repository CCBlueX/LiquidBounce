/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.scoreboard

interface IScoreboard {
    fun getPlayersTeam(name: String?): ITeam?
    fun getObjectiveInDisplaySlot(index: Int): IScoreObjective?
    fun getSortedScores(objective: IScoreObjective): Collection<IScore>
}