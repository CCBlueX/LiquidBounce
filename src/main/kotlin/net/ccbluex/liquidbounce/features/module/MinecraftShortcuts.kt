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
 */
package net.ccbluex.liquidbounce.features.module

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

/**
 * Collection of the most used variables
 * to make the code more readable.
 *
 * However, we do not check for nulls here, because
 * we are sure that the client is in-game, if not
 * fiddling with the handler code.
 */
interface MinecraftShortcuts {
    val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.client.mc
    val player: ClientPlayerEntity
        get() = mc.player!!
    val world: ClientWorld
        get() = mc.world!!
    val network: ClientPlayNetworkHandler
        get() = mc.networkHandler!!
    val interaction: ClientPlayerInteractionManager
        get() = mc.interactionManager!!
}
