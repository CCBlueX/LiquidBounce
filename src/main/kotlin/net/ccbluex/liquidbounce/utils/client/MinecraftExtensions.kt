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
package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.util.Window
import net.minecraft.client.world.ClientWorld


val Window.dimensions
    get() = Pair(width, height)

val Window.scaledDimension
    get() = Pair(scaledWidth, scaledHeight)

val mc: MinecraftClient
    inline get() = MinecraftClient.getInstance()!!
val player: ClientPlayerEntity
    inline get() = mc.player!!
val world: ClientWorld
    inline get() = mc.world!!
val network: ClientPlayNetworkHandler
    inline get() = mc.networkHandler!!
val interaction: ClientPlayerInteractionManager
    inline get() = mc.interactionManager!!
