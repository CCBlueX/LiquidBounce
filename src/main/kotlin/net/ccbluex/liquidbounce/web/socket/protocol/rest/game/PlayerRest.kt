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
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

fun RestNode.playerRest() {
    get("/player") {
        httpOk(JsonObject().apply {
            addProperty("username", player.nameForScoreboard)
            addProperty("uuid", player.uuidAsString)
            add("stats", protocolGson.toJsonTree(PlayerStatistics.fromPlayer(player)))
            add("gameMode", protocolGson.toJsonTree(interaction.currentGameMode))
            add("position", JsonObject().apply {
                addProperty("x", player.x)
                addProperty("y", player.y)
                addProperty("z", player.z)
            })
            add("rotation", JsonObject().apply {
                addProperty("yaw", player.yaw)
                addProperty("pitch", player.pitch)
            })
            add("region", JsonObject().apply {
                addProperty("x", player.chunkPos.x)
                addProperty("z", player.chunkPos.z)
            })
        })
    }
}

/**
 * Represents statistics for a player, including health, max health, absorption, armor, food level,
 * experience level, and experience progress.
 *
 * This data class automatically generates an [equals] method which compares the values of all
 * properties declared in the primary constructor. Therefore, instances of [PlayerStatistics] with
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
data class PlayerStatistics(
    val username: String,
    val skinIdentifier: Identifier? = null,
    val health: Float,
    val maxHealth: Float,
    val absorption: Float,
    val armor: Int,
    val food: Int,
    val experienceLevel: Int,
    val experienceProgress: Float,
    val armorItems: List<ItemStack> = emptyList()
) {

    companion object {
        /**
         * Creates a [PlayerStatistics] instance from a [PlayerEntity].
         *
         * @param player The player entity to extract statistics from.
         * @return A [PlayerStatistics] instance representing the player's statistics.
         */
        fun fromPlayer(player: PlayerEntity) = PlayerStatistics(
            player.nameForScoreboard,
            network.playerList.find { it.profile.id == player.uuid }?.skinTextures?.texture,
            player.health,
            player.maxHealth,
            player.absorptionAmount,
            player.armor,
            player.hungerManager.foodLevel,
            player.experienceLevel,
            player.experienceProgress,
            player.armorItems.toList()
        )
    }

}
