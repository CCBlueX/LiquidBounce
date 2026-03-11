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

import com.viaversion.viafabricplus.ViaFabricPlus
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility1_8
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

// Only runs once
val usesViaFabricPlus = runCatching {
    Class.forName("com.viaversion.viafabricplus.ViaFabricPlus")

    // Register ViaFabricPlus protocol version change callback
    ViaFabricPlus.getImpl().registerOnChangeProtocolVersionCallback { _, _ ->
        // Update the window title
        mc.execute {
            mc.updateTitle()
        }
    }

    true
}.getOrDefault(false)

/**
 * Both 1.20.3 and 1.20.4 use protocol 765, so we can use this as a default
 */
val defaultProtocolVersion = ClientProtocolVersion(
    SharedConstants.getCurrentVersion().name(),
    SharedConstants.getCurrentVersion().protocolVersion()
)

val protocolVersion: ClientProtocolVersion
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (usesViaFabricPlus) {
            return@runCatching VfpCompatibility.INSTANCE.unsafeGetProtocolVersion()
        } else {
            return@runCatching defaultProtocolVersion
        }
    }.onFailure {
        logger.error("Failed to get protocol version", it)
    }.getOrDefault(defaultProtocolVersion)

val protocolVersions: Array<ClientProtocolVersion>
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (usesViaFabricPlus) {
            return@runCatching VfpCompatibility.INSTANCE.unsafeGetProtocolVersions()
        } else {
            return@runCatching arrayOf(defaultProtocolVersion)
        }
    }.onFailure {
        logger.error("Failed to get protocol version", it)
    }.getOrDefault(arrayOf(defaultProtocolVersion))

data class ClientProtocolVersion(val name: String, val version: Int)

val isEqual1_8: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isEqual1_8
    }.onFailure {
        logger.error("Failed to check if the server is using old combat", it)
    }.getOrDefault(false)

val isOlderThanOrEqual1_8: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isOlderThanOrEqual1_8
    }.onFailure {
        logger.error("Failed to check if the server is using old combat", it)
    }.getOrDefault(false)

val isOlderThanOrEquals1_7_10: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isOlderThanOrEqual1_7_10
    }.onFailure {
        logger.error("Failed to check if the server is using 1.7.10-", it)
    }.getOrDefault(false)

val isNewerThanOrEquals1_16: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isNewerThanOrEqual1_16
    }.onFailure {
        logger.error("Failed to check if the server is using 1.16+", it)
    }.getOrDefault(false)

/**
 * Since 1.21.5 anything can be used to blocking
 */
val isNewerThanOrEquals1_21_5: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isNewerThanOrEqual1_21_5
    }.onFailure {
        logger.error("Failed to check if the server is using 1.21.5+", it)
    }.getOrDefault(false)

/**
 * Since 1.21.6 the [ServerboundPlayerCommandPacket.Action] removed 2 entries for sneaking
 */
val isNewerThanOrEquals1_21_6: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isNewerThanOrEqual1_21_6
    }.onFailure {
        logger.error("Failed to check if the server is using 1.21.6+", it)
    }.getOrDefault(false)

/**
 * Since 1.21.9 the byte format of [net.minecraft.world.phys.Vec3] have been rewritten
 * with [net.minecraft.network.LpVec3].
 */
val isNewerThanOrEquals1_21_9: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isNewerThanOrEqual1_21_9
    }.onFailure {
        logger.error("Failed to check if the server is using 1.21.9+", it)
    }.getOrDefault(false)

val isOlderThanOrEqual1_11_1: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        usesViaFabricPlus && VfpCompatibility.INSTANCE.isOlderThanOrEqual1_11_1
    }.onFailure {
        logger.error("Failed to check if the server is using 1.11.1", it)
    }.getOrDefault(false)

fun selectProtocolVersion(protocolId: Int) {
    // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
    if (usesViaFabricPlus) {
        VfpCompatibility.INSTANCE.unsafeSelectProtocolVersion(protocolId)
    } else {
        error("ViaFabricPlus is not loaded")
    }
}

fun openVfpProtocolSelection() {
    // Check if the ViaFabricPlus mod is loaded
    if (!usesViaFabricPlus) {
        logger.error("ViaFabricPlus is not loaded")
        return
    }

    VfpCompatibility.INSTANCE.unsafeOpenVfpProtocolSelection()
}

@Suppress("FunctionName")
fun send1_8SignUpdate(blockPos: BlockPos, lines: Array<String>) {
    require(usesViaFabricPlus) { "ViaFabricPlus is missing" }
    require(isEqual1_8) { "Not 1.8 protocol" }

    VfpCompatibility1_8.INSTANCE.sendSignUpdate(blockPos, lines)
}

@Suppress("FunctionName")
fun send1_8PlayerInput(sideways: Float, forward: Float, jumping: Boolean, sneaking: Boolean) {
    require(usesViaFabricPlus) { "ViaFabricPlus is missing" }
    require(isEqual1_8) { "Not 1.8 protocol" }

    VfpCompatibility1_8.INSTANCE.sendPlayerInput(sideways, forward, jumping, sneaking)
}
