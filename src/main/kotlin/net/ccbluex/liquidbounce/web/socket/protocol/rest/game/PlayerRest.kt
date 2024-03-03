/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardEntry
import net.minecraft.scoreboard.Team
import net.minecraft.scoreboard.number.NumberFormat
import net.minecraft.scoreboard.number.StyledNumberFormat
import net.minecraft.text.Text
import net.minecraft.world.GameMode

fun RestNode.playerRest() {
    get("/player") {
        httpOk(protocolGson.toJsonTree(PlayerData.fromPlayer(player)))
    }
}

data class PlayerData(
    val username: String,
    val textures: SkinTextures? = null,
    val selectedSlot: Int,
    val gameMode: GameMode = GameMode.DEFAULT,
    val health: Float,
    val maxHealth: Float,
    val absorption: Float,
    val armor: Int,
    val food: Int,
    val air: Int,
    val maxAir: Int,
    val experienceLevel: Int,
    val experienceProgress: Float,
    val effects: List<StatusEffectInstance>,
    val mainHandStack: ItemStack,
    val offHandStack: ItemStack,
    val armorItems: List<ItemStack> = emptyList(),
    val scoreboard: ScoreboardData? = null
) {

    companion object {

        fun fromPlayer(player: PlayerEntity) = PlayerData(
            player.nameForScoreboard,
            network.playerList.find { it.profile == player.gameProfile }?.skinTextures,
            player.inventory.selectedSlot,
            if (mc.player == player) interaction.currentGameMode else GameMode.DEFAULT,
            player.health,
            player.maxHealth,
            player.absorptionAmount,
            player.armor,
            player.hungerManager.foodLevel,
            player.air,
            player.maxAir,
            player.experienceLevel,
            player.experienceProgress,
            player.statusEffects.toList(),
            player.mainHandStack,
            player.offHandStack,
            player.armorItems.toList(),
            if (mc.player == player) ScoreboardData.fromScoreboard(player.scoreboard) else null
        )
    }

}

data class SidebarEntry(val name: Text, val score: Text)

data class ScoreboardData(val header: Text, val entries: Array<SidebarEntry?>) {

    companion object {

        /**
         * Creates a [ScoreboardData] from the [player]'s scoreboard
         *
         * Taken from the Minecraft source code
         */
        fun fromScoreboard(scoreboard: Scoreboard): ScoreboardData? {
            val team = scoreboard.getScoreHolderTeam(player.nameForScoreboard)

            val objective = team?.let {
                ScoreboardDisplaySlot.fromFormatting(team.color)?.let { scoreboard.getObjectiveForSlot(it) }
            } ?: scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return null

            val objectiveScoreboard: Scoreboard = objective.scoreboard
            val numberFormat: NumberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED)

            val entryComparator = Comparator
                .comparing { scoreboardEntry: ScoreboardEntry -> scoreboardEntry.value() }
                .reversed()
                .thenComparing({ it.owner() }, String.CASE_INSENSITIVE_ORDER)

            val sidebarEntries = objectiveScoreboard.getScoreboardEntries(objective)
                .stream()
                .filter { score: ScoreboardEntry -> !score.hidden() }
                .sorted(entryComparator)
                .limit(15L)
                .map { scoreboardEntry: ScoreboardEntry ->
                    val team = objectiveScoreboard.getScoreHolderTeam(scoreboardEntry.owner())
                    val entryName = scoreboardEntry.name()
                    val entryWithDecoration: Text = Team.decorateName(team, entryName)
                    val entryValue: Text = scoreboardEntry.formatted(numberFormat)

                    SidebarEntry(entryWithDecoration, entryValue)
                }
                .toArray { arrayOfNulls<SidebarEntry>(it) }

            return ScoreboardData(objective.displayName, sidebarEntries)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScoreboardData

        if (header != other.header) return false
        if (!entries.contentEquals(other.entries)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + entries.contentHashCode()
        return result
    }

}
