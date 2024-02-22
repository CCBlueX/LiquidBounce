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
    val armorItems: List<ItemStack> = emptyList()
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
            player.armorItems.toList()
        )
    }

}
