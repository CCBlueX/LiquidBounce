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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

fun RestNode.playerRest() {
    get("/player") {
        httpOk(protocolGson.toJsonTree(PlayerData.fromPlayer(player)))
    }

    get("/player/gamemode") {
        httpOk(protocolGson.toJsonTree(interaction.currentGameMode))
    }
}

/**
 * Represents statistics for a player, including health, max health, absorption, armor, food level,
 * experience level, and experience progress.
 *
 * This data class automatically generates an [equals] method which compares the values of all
 * properties declared in the primary constructor. Therefore, instances of [PlayerData] with
 * the same values for all properties are considered equal.
 *
 * @property health The current health of the player.
 * @property maxHealth The maximum health the player can have.
 * @property absorption The absorption amount of the player.
 * @property armor The armor value of the player.
 * @property food The current food level of the player.
 * @property experienceLevel The level of experience the player has.
 * @property experienceProgress The progress towards the next experience level.
 */
data class PlayerData(
    val username: String,
    val textures: SkinTextures? = null,
    val selectedSlot: Int,
    val health: Float,
    val maxHealth: Float,
    val absorption: Float,
    val armor: Int,
    val food: Int,
    val air: Int,
    val maxAir: Int,
    val experienceLevel: Int,
    val experienceProgress: Float,
    val mainHandStack: ItemStack,
    val offHandStack: ItemStack,
    val armorItems: List<ItemStack> = emptyList()
) {

    companion object {
        /**
         * Creates a [PlayerData] instance from a [PlayerEntity].
         *
         * @param player The player entity to extract statistics from.
         * @return A [PlayerData] instance representing the player's statistics.
         */
        fun fromPlayer(player: PlayerEntity) = PlayerData(
            player.nameForScoreboard,
            network.playerList.find { it.profile == player.gameProfile }?.skinTextures,
            player.inventory.selectedSlot,
            player.health,
            player.maxHealth,
            player.absorptionAmount,
            player.armor,
            player.hungerManager.foodLevel,
            player.air,
            player.maxAir,
            player.experienceLevel,
            player.experienceProgress,
            player.mainHandStack,
            player.offHandStack,
            player.armorItems.toList()
        )
    }

}
