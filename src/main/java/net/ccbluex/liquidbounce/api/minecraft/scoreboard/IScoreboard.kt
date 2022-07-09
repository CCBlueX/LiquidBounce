/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.scoreboard

interface IScoreboard
{
	// <editor-fold desc="Team">
	fun getPlayersTeam(name: String?): ITeam?
	// </editor-fold>

	// <editor-fold desc="Objective">
	fun getObjectivesForEntity(entityName: String): Map<IScoreObjective, IScore>
	fun getObjectiveInDisplaySlot(index: Int): IScoreObjective?
	// </editor-fold>

	// <editor-fold desc="Score">
	fun getSortedScores(objective: IScoreObjective): Collection<IScore>
	// </editor-fold>
}
