/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import de.florianmichael.viafabricplus.protocolhack.ProtocolHack
import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen
import de.florianmichael.viafabricplus.settings.impl.VisualSettings
import net.minecraft.SharedConstants
import net.minecraft.client.gui.screen.TitleScreen
import net.raphimc.vialoader.util.VersionEnum

// Only runs once
val usesViaFabricPlus = runCatching {
    Class.forName("de.florianmichael.viafabricplus.ViaFabricPlus")
    true
}.getOrDefault(false)

val hasProtocolHack = runCatching {
    Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack")
    true
}.getOrDefault(false)

val hasVisualSettings = runCatching {
    Class.forName("de.florianmichael.viafabricplus.settings.impl.VisualSettings")
    true
}.getOrDefault(false)

/**
 * Both 1.20.3 and 1.20.4 use protocol 765, so we can use this as a default
 */
val defaultProtocolVersion = ProtocolVersion(SharedConstants.getGameVersion().name,
    SharedConstants.getGameVersion().protocolVersion)

val protocolVersion: ProtocolVersion
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (!hasProtocolHack) {
            return@runCatching defaultProtocolVersion
        }

        iGetProtocolVersion()
    }.onFailure {
        logger.error("Failed to get protocol version", it)
    }.getOrDefault(defaultProtocolVersion)

val protocolVersions: Array<ProtocolVersion>
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (!hasProtocolHack) {
            return@runCatching arrayOf(defaultProtocolVersion)
        }

        iGetProtocolVersions()
    }.onFailure {
        logger.error("Failed to get protocol version", it)
    }.getOrDefault(arrayOf(defaultProtocolVersion))

data class ProtocolVersion(val name: String, val version: Int)

val isOldCombat: Boolean
    get() = runCatching {
        // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
        if (!hasProtocolHack) {
            return@runCatching false
        }

        iIsOldCombat()
    }.onFailure {
        logger.error("Failed to check if the server is using old combat", it)
    }.getOrDefault(false)

/**
 * Internal method to prevent ClassNotFoundExceptions
 */
private fun iGetProtocolVersion(): ProtocolVersion {
    val version = ProtocolHack.getTargetVersion()
    return ProtocolVersion(version.protocol.name, version.protocol.version)
}

/**
 * Internal method to prevent NoClassDefFoundError
 */
private fun iGetProtocolVersions(): Array<ProtocolVersion> {
    return VersionEnum.SORTED_VERSIONS.map { version ->
        ProtocolVersion(version.protocol.name, version.protocol.version)
    }.toTypedArray()
}

/**
 * Internal method to prevent NoClassDefFoundError
 */
private fun iIsOldCombat(): Boolean {
    val version = ProtocolHack.getTargetVersion()

    // Check if the version is older or equal than 1.8
    return version.isOlderThanOrEqualTo(VersionEnum.r1_8)
}

fun selectProtocolVersion(protocolId: Int) {
    // Check if the ViaFabricPlus mod is loaded - prevents from causing too many exceptions
    if (!usesViaFabricPlus) {
        error("ViaFabricPlus is not loaded")
    }

    iSelectProtocolVersion(protocolId)
}

/**
 * Internal method to prevent NoClassDefFoundError
 */
private fun iSelectProtocolVersion(protocolId: Int) {
    runCatching {
        val version = VersionEnum.fromProtocolId(protocolId)
            ?: error("Protocol version $protocolId not found")

        ProtocolHack.setTargetVersion(version)
    }.onFailure {
        logger.error("Failed to select protocol version", it)
    }
}

fun openVfpProtocolSelection() {
    // Check if the ViaFabricPlus mod is loaded
    if (!usesViaFabricPlus) {
        logger.error("ViaFabricPlus is not loaded")
        return
    }

    iOpenVfpProtocolSelection()
}

/**
 * Internal method to prevent NoClassDefFoundError
 */
private fun iOpenVfpProtocolSelection() {
    runCatching {
        ProtocolSelectionScreen.INSTANCE.open(mc.currentScreen ?: TitleScreen())
    }.onFailure {
        logger.error("Failed to open ViaFabricPlus screen", it)
    }
}

fun disableConflictingVfpOptions() {
    // Check if the ViaFabricPlus mod is loaded
    if (!usesViaFabricPlus || !hasVisualSettings) {
        return
    }

    iDisableConflictingVfpOptions()
}

/**
 * Internal method to prevent NoClassDefFoundError
 */
private fun iDisableConflictingVfpOptions() {
    runCatching {
        val visualSettings = VisualSettings.global()

        // 1 == off, 0 == on
        visualSettings.enableSwordBlocking.value = 1
        visualSettings.enableBlockHitAnimation.value = 1
    }.onFailure {
        logger.error("Failed to disable conflicting options", it)
    }
}
