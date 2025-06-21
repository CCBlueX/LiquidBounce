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
@file:JvmName("ProtocolUtil")
package net.ccbluex.liquidbounce.utils.client

import com.mojang.blaze3d.systems.RenderSystem
import com.viaversion.viafabricplus.ViaFabricPlus
import com.viaversion.viaversion.api.minecraft.BlockPosition
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.api.protocol.version.VersionType
import com.viaversion.viaversion.api.type.Types
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.minecraft.SharedConstants
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.util.math.BlockPos

object VfpUnsafe {
    @JvmStatic
    val clientProtocolVersion: ClientProtocolVersion
        get() = try {
            val version = ViaFabricPlus.getImpl().targetVersion
            ClientProtocolVersion(version.name, version.version)
        } catch (throwable: Throwable) {
            logger.error("Failed to get protocol version", throwable)
            defaultProtocolVersion
        }

    @JvmStatic
    val clientProtocolVersions: Array<ClientProtocolVersion>
        get() = try {
            val protocols = ProtocolVersion.getProtocols()
                .filter { version -> version.versionType == VersionType.RELEASE }
                .mapArray { version ->
                    ClientProtocolVersion(version.name, version.version)
                }
            protocols.reverse()

            protocols
        } catch (throwable: Throwable) {
            logger.error("Failed to get protocol versions", throwable)
            emptyArray()
        }

    @JvmStatic
    fun openVfpProtocolSelection() {
        try {
            ViaFabricPlus.getImpl().openProtocolSelectionScreen(mc.currentScreen ?: TitleScreen())
        } catch (throwable: Throwable) {
            logger.error("Failed to open ViaFabricPlus screen", throwable)
        }
    }

    @JvmStatic
    fun selectProtocolVersion(protocolId: Int) {
        require(ProtocolVersion.isRegistered(protocolId)) { "Protocol version is not registered" }

        try {
            val version = ProtocolVersion.getProtocol(protocolId)
            ViaFabricPlus.getImpl().targetVersion = version
        } catch (throwable: Throwable) {
            logger.error("Failed to select protocol version", throwable)
        }
    }
}

// Only runs once
val usesViaFabricPlus = runCatching {
    Class.forName("com.viaversion.viafabricplus.ViaFabricPlus")

    // Register ViaFabricPlus protocol version change callback
    ViaFabricPlus.getImpl().registerOnChangeProtocolVersionCallback { _, _ ->
        // Update the window title
        RenderSystem.recordRenderCall {
            mc.updateWindowTitle()
        }
    }

    true
}.getOrDefault(false)

data class ClientProtocolVersion(val name: String, val version: Int)

/**
 * Get the current [ProtocolVersion] of [ViaFabricPlus].
 * If it's not loaded, null will be returned.
 */
fun getVFPVersionOrNull(): ProtocolVersion? {
    return if (usesViaFabricPlus) {
        ViaFabricPlus.getImpl().targetVersion
    } else {
        null
    }
}

/**
 * Get the current [ProtocolVersion] of [ViaFabricPlus].
 * If it's not loaded, the current game version as [ProtocolVersion] will be returned.
 */
fun getVFPVersionOrDefault(): ProtocolVersion =
    getVFPVersionOrNull() ?: ProtocolVersion.getProtocol(defaultProtocolVersion.version)

/**
 * Both 1.20.3 and 1.20.4 use protocol 765, so we can use this as a default
 */
val defaultProtocolVersion = ClientProtocolVersion(
    SharedConstants.getGameVersion().name,
    SharedConstants.getGameVersion().protocolVersion
)

val protocolVersion: ClientProtocolVersion
    get() = if (usesViaFabricPlus) {
        VfpUnsafe.clientProtocolVersion
    } else {
        defaultProtocolVersion
    }

val protocolVersions: Array<ClientProtocolVersion>
    get() = if (usesViaFabricPlus) {
        VfpUnsafe.clientProtocolVersions
    } else {
        arrayOf(defaultProtocolVersion)
    }

val isEqual1_8: Boolean
    get() = getVFPVersionOrDefault().compareTo(ProtocolVersion.v1_8) == 0

val isOlderThanOrEqual1_8: Boolean
    get() = getVFPVersionOrDefault() >= ProtocolVersion.v1_8

val isOlderThanOrEqual1_9: Boolean
    get() = getVFPVersionOrDefault() >= ProtocolVersion.v1_9

val isOlderThanOrEquals1_7_10: Boolean
    get() = getVFPVersionOrDefault() >= ProtocolVersion.v1_7_6

val isNewerThanOrEquals1_16: Boolean
    get() = getVFPVersionOrDefault() >= ProtocolVersion.v1_16

val isOlderThanOrEqual1_11_1: Boolean
    get() = getVFPVersionOrDefault() >= ProtocolVersion.v1_11_1

fun selectProtocolVersion(protocolId: Int) {
    // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
    if (usesViaFabricPlus) {
        VfpUnsafe.selectProtocolVersion(protocolId)
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

    VfpUnsafe.openVfpProtocolSelection()
}

@Suppress("FunctionName")
private inline fun send1_8Packet(packetType: ServerboundPacketType, writer: PacketWrapper.() -> Unit) {
    require(isEqual1_8) { "Not on 1.8 protocol" }

    val packet = PacketWrapper.create(packetType, ViaFabricPlus.getImpl().playNetworkUserConnection)
    writer(packet)
    packet.sendToServerRaw()
}

@Suppress("FunctionName")
fun send1_8SignUpdate(blockPos: BlockPos, lines: Array<String>) {
    require(usesViaFabricPlus) { "ViaFabricPlus is missing" }
    require(isEqual1_8) { "Not 1.8 protocol" }

    require(lines.size == 4) { "Lines length does not match 4" }
    send1_8Packet(ServerboundPackets1_8.SIGN_UPDATE) {
        write(
            Types.BLOCK_POSITION1_8,
            BlockPosition(blockPos.x, blockPos.y, blockPos.z)
        )

        for (line in lines) {
            write(Types.STRING, line)
        }
    }
}

@Suppress("FunctionName")
fun send1_8PlayerInput(sideways: Float, forwards: Float, jumping: Boolean, sneaking: Boolean) {
    require(usesViaFabricPlus) { "ViaFabricPlus is missing" }
    require(isEqual1_8) { "Not 1.8 protocol" }

    send1_8Packet(ServerboundPackets1_8.PLAYER_INPUT) {
        write(Types.FLOAT, sideways)
        write(Types.FLOAT, forwards)
        val b: Byte = when {
            jumping && sneaking -> 0b11
            jumping -> 0b01
            sneaking -> 0b10
            else -> 0b00
        }
        write(Types.BYTE, b)
    }
}
