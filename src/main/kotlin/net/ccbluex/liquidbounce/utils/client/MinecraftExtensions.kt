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
package net.ccbluex.liquidbounce.utils.client

import com.mojang.authlib.GameProfileRepository
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.ServicesKeySet
import com.mojang.blaze3d.platform.Window
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.server.Services
import net.minecraft.server.players.ProfileResolver
import net.minecraft.server.players.UserNameToIdResolver

val Window.dimensions
    get() = intArrayOf(screenWidth, screenHeight)

val Window.scaledDimension
    get() = intArrayOf(guiScaledWidth, guiScaledHeight)

val mc: Minecraft
    inline get() = Minecraft.getInstance()
val player: LocalPlayer
    inline get() = mc.player!!
val world: ClientLevel
    inline get() = mc.level!!
val network: ClientPacketListener
    inline get() = mc.connection!!
val interaction: MultiPlayerGameMode
    inline get() = mc.gameMode!!
val gpuDevice: GpuDevice
    inline get() = RenderSystem.getDevice()

fun Services.with(
    sessionService: MinecraftSessionService = this.sessionService,
    servicesKeySet: ServicesKeySet = this.servicesKeySet,
    profileRepository: GameProfileRepository = this.profileRepository,
    nameToIdCache: UserNameToIdResolver = this.nameToIdCache,
    profileResolver: ProfileResolver = this.profileResolver
): Services {
    return Services(
        sessionService, servicesKeySet,
        profileRepository,
        nameToIdCache,
        profileResolver
    )
}
