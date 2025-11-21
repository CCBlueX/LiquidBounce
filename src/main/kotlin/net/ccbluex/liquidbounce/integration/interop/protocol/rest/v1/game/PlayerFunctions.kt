/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.hideShieldSlot
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.shouldHideOffhand
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinInGameHudAccessor
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.EnderChestInventoryTracker
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
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
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
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
fun getCrosshairData(requestObject: RequestObject) = nullableResponse(mc.crosshairTarget)

@JvmRecord
data class PlayerData(
    val username: String,
    val uuid: String,
    val dimension: Identifier,
    val position: Vec3d,
    val netherPosition: Vec3d,
    val blockPosition: BlockPos,
    val velocity: Vec3d,
    val selectedSlot: Int,
    val gameMode: GameMode = GameMode.DEFAULT,
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
    val effects: List<StatusEffectInstance>,
    val mainHandStack: ItemStack,
    val offHandStack: ItemStack,
    val armorItems: List<ItemStack> = emptyList(),
    val scoreboard: ScoreboardData? = null,
) {

    companion object {

        @JvmStatic
        fun fromPlayer(player: PlayerEntity) = PlayerData(
            ModuleNameProtect.replace(player.nameForScoreboard),
            player.uuidAsString,
            player.world.registryKey.value,
            player.pos,
            player.netherPosition,
            player.blockPos,
            player.velocity,
            player.inventory.selectedSlot,
            if (mc.player === player) interaction.currentGameMode else GameMode.DEFAULT,
            player.health.fixNaN(),
            player.getActualHealth().fixNaN(),
            player.maxHealth.fixNaN(),
            if (player.hasHealthScoreboard()) 0f else player.absorptionAmount.fixNaN(),
            player.yaw.fixNaN(),
            player.pitch.fixNaN(),
            player.armor.coerceAtMost(20),
            min(player.hungerManager.foodLevel, 20),
            player.air,
            player.maxAir,
            player.experienceLevel,
            player.experienceProgress.fixNaN(),
            player.ping,
            player.statusEffects.toList(),
            player.mainHandStack,
            if (player == mc.player && shouldHideOffhand() && hideShieldSlot) ItemStack.EMPTY else player.offHandStack,
            player.armorItems.toList(),
            if (mc.player === player) ScoreboardData.fromScoreboard(player.scoreboard) else null
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
        fun fromPlayer(player: PlayerEntity) = PlayerInventoryData(
            armor = player.armorItems.map(ItemStack::copy),
            main = player.inventory.mainStacks.map(ItemStack::copy),
            crafting = player.playerScreenHandler.craftingInput.heldStacks.map(ItemStack::copy),
            /** player.enderChestInventory.getHeldStacks().map(ItemStack::copy) */
            enderChest = EnderChestInventoryTracker.stacks,
        )
    }

}

@JvmRecord
data class ScoreboardData(val header: Text, val entries: List<SidebarEntry?>) {

    @JvmRecord
    data class SidebarEntry(val name: Text, val score: Text)

    companion object {

        /**
         * Creates a [ScoreboardData] from the [player]'s scoreboard
         *
         * Taken from the Minecraft source code
         *
         * @see net.minecraft.client.gui.hud.InGameHud.renderScoreboardSidebar
         */
        @JvmStatic
        fun fromScoreboard(scoreboard: Scoreboard?): ScoreboardData? {
            scoreboard ?: return null

            val team = mc.player?.let { player ->
                scoreboard.getScoreHolderTeam(player.nameForScoreboard)
            }

            val objective = team?.let {
                ScoreboardDisplaySlot.fromFormatting(team.color)?.let { scoreboard.getObjectiveForSlot(it) }
            } ?: scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return null

            val objectiveScoreboard: Scoreboard = objective.scoreboard
            val numberFormat: NumberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED)

            val sidebarEntries = objectiveScoreboard.getScoreboardEntries(objective)
                .filter { score: ScoreboardEntry -> !score.hidden() }
                .sortedWith(MixinInGameHudAccessor.getScoreboardEntryComparator())
                .take(15)
                .mapToArray { scoreboardEntry: ScoreboardEntry ->
                    val team = objectiveScoreboard.getScoreHolderTeam(scoreboardEntry.owner())
                    val entryName = scoreboardEntry.name()
                    val entryWithDecoration: Text = Team.decorateName(team, entryName)
                    val entryValue: Text = scoreboardEntry.formatted(numberFormat)

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
