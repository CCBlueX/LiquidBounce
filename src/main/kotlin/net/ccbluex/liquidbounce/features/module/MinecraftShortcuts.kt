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
package net.ccbluex.liquidbounce.features.module

import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer

/**
 * Collection of the most used variables
 * to make the code more readable.
 *
 * However, we do not check for nulls here, because
 * we are sure that the client is in-game, if not
 * fiddling with the handler code.
 */
interface MinecraftShortcuts {
    val mc: Minecraft
        get() = net.ccbluex.liquidbounce.utils.client.mc
    val player: LocalPlayer
        get() = requireNotNull(mc.player) { "mc.player is null" }
    val world: ClientLevel
        get() = requireNotNull(mc.level) { "mc.level is null" }
    val network: ClientPacketListener
        get() = requireNotNull(mc.connection) { "mc.connection is null" }
    val interaction: MultiPlayerGameMode
        get() = requireNotNull(mc.gameMode) { "mc.gameMode is null" }
    val gpuDevice: GpuDevice
        get() = RenderSystem.getDevice()
}
