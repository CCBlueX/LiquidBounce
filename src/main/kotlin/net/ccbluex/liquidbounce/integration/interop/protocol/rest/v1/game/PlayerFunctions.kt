/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.hideShieldSlot
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.shouldHideOffhand
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinGuiAccessor
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.hasHealthScoreboard
import net.ccbluex.liquidbounce.utils.entity.netherPosition
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.inventory.EnderChestInventoryTracker
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.gui.Gui
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.network.chat.numbers.StyledFormat
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import kotlin.math.min

private fun nullableResponse(item: Any?) = item?.let { httpOk(interopGson.toJsonTree(it)) } ?: httpNoContent()

// GET /api/v1/client/player
@Suppress("UNUSED_PARAMETER")
fun getPlayerData(requestObject: RequestObject) = nullableResponse(mc.player?.let(PlayerData::fromPlayer))

// GET /api/v1/client/player/inventory
@Suppress("UNUSED_PARAMETER")
fun getPlayerInventory(requestObject: RequestObject) = nullableResponse(mc.player?.let(PlayerInventoryData::fromPlayer))

// GET /api/v1/client/crosshair
@Suppress("UNUSED_PARAMETER")
fun getCrosshairData(requestObject: RequestObject) = nullableResponse(mc.hitResult)

@JvmRecord
data class PlayerData(
    val username: String,
    val uuid: String,
    val dimension: Identifier,
    val position: Vec3,
    val netherPosition: Vec3,
    val blockPosition: BlockPos,
    val velocity: Vec3,
    val selectedSlot: Int,
    val gameMode: GameType = GameType.DEFAULT_MODE,
    val health: Float,
    val actualHealth: Float,
    val maxHealth: Float,
    val absorption: Float,
    val yaw: Float,
    val pitch: Float,
    val armor: Int,
    val food: Int,
    val air: Int,
    val maxAir: Int,
    val experienceLevel: Int,
    val experienceProgress: Float,
    val ping: Int,
    val effects: List<MobEffectInstance>,
    val mainHandStack: ItemStack,
    val offHandStack: ItemStack,
    val armorItems: List<ItemStack> = emptyList(),
    val scoreboard: ScoreboardData? = null,
) {

    companion object {

        @JvmStatic
        fun fromPlayer(player: Player) = PlayerData(
            ModuleNameProtect.replace(player.scoreboardName),
            player.stringUUID,
            player.level().dimension().identifier(),
            player.position(),
            player.netherPosition,
            player.blockPosition(),
            player.deltaMovement,
            player.inventory.selectedSlot,
            player.gameMode() ?: GameType.DEFAULT_MODE,
            player.health.fixNaN(),
            player.getActualHealth().fixNaN(),
            player.maxHealth.fixNaN(),
            if (player.hasHealthScoreboard()) 0f else player.absorptionAmount.fixNaN(),
            player.yRot.fixNaN(),
            player.xRot.fixNaN(),
            player.armorValue.coerceAtMost(20),
            min(player.foodData.foodLevel, 20),
            player.airSupply,
            player.maxAirSupply,
            player.experienceLevel,
            player.experienceProgress.fixNaN(),
            player.ping,
            player.activeEffects.toList(),
            player.mainHandItem,
            if (player == mc.player && shouldHideOffhand() && hideShieldSlot) ItemStack.EMPTY else player.offhandItem,
            player.armorItems.toList(),
            ScoreboardData.fromScoreboard(
                player.level().scoreboard
            )
        )
    }

}

@JvmRecord
data class PlayerInventoryData(
    val armor: List<ItemStack>,
    val main: List<ItemStack>,
    val crafting: List<ItemStack>,
    val enderChest: List<ItemStack>,
) {

    companion object {
        @JvmStatic
        fun fromPlayer(player: Player) = PlayerInventoryData(
            armor = player.armorItems.map(ItemStack::copy),
            main = player.inventory.nonEquipmentItems.map(ItemStack::copy),
            crafting = player.inventoryMenu.craftSlots.items.map(ItemStack::copy),
            /** player.enderChestInventory.getHeldStacks().map(ItemStack::copy) */
            enderChest = EnderChestInventoryTracker.stacks,
        )
    }

}

@JvmRecord
data class ScoreboardData(val header: Component, val entries: List<SidebarEntry?>) {

    @JvmRecord
    data class SidebarEntry(val name: Component, val score: Component)

    companion object {

        /**
         * Creates a [ScoreboardData] from the players's scoreboard
         *
         * Taken from the Minecraft source code
         *
         * @see Gui.renderScoreboardSidebar
         */
        @JvmStatic
        fun fromScoreboard(scoreboard: Scoreboard?): ScoreboardData? {
            scoreboard ?: return null

            val team = mc.player?.let { player ->
                scoreboard.getPlayersTeam(player.scoreboardName)
            }

            val objective = team?.let {
                DisplaySlot.teamColorToSlot(team.color)?.let { scoreboard.getDisplayObjective(it) }
            } ?: scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null

            val objectiveScoreboard: Scoreboard = objective.scoreboard
            val numberFormat: NumberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)

            val sidebarEntries = objectiveScoreboard.listPlayerScores(objective)
                .filter { score: PlayerScoreEntry -> !score.isHidden }
                .sortedWith(MixinGuiAccessor.getScoreboardEntryComparator())
                .take(15)
                .mapToArray { scoreboardEntry: PlayerScoreEntry ->
                    val team = objectiveScoreboard.getPlayersTeam(scoreboardEntry.owner())
                    val entryName = scoreboardEntry.ownerName()
                    val entryWithDecoration: Component = PlayerTeam.formatNameForTeam(team, entryName)
                    val entryValue: Component = scoreboardEntry.formatValue(numberFormat)

                    SidebarEntry(entryWithDecoration.sanitizeForeignInput(), entryValue.sanitizeForeignInput())
                }.asList()

            return ScoreboardData(objective.displayName.sanitizeForeignInput(), sidebarEntries)
        }
    }

}

/**
 * GSON is not happy with NaN values, so we fix them to be 0.
 */
private fun Float.fixNaN() = if (isNaN()) 0f else this
