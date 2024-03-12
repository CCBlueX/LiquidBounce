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

package net.ccbluex.liquidbounce.utils.client.vfp;

import de.florianmichael.viafabricplus.protocolhack.ProtocolHack;
import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.client.ProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.raphimc.vialoader.util.VersionEnum;

/**
 * Compatibility layer for ViaFabricPlus
 *
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED
 */
public enum VfpCompatibility {

    INSTANCE;

    public void unsafeDsableConflictingVfpOptions() {
        try {
            VisualSettings visualSettings = VisualSettings.global();

            // 1 == off, 0 == on
            visualSettings.enableSwordBlocking.setValue(1);
            visualSettings.enableBlockHitAnimation.setValue(1);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to disable conflicting options", throwable);
        }
    }

    public ProtocolVersion unsafeGetProtocolVersion() {
        try {
            VersionEnum version = ProtocolHack.getTargetVersion();
            return new ProtocolVersion(version.getProtocol().getName(), version.getProtocol().getVersion());
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol version", throwable);
            return null;
        }
    }

    public ProtocolVersion[] unsafeGetProtocolVersions() {
        try {
            return VersionEnum.SORTED_VERSIONS
                    .stream()
                    .map(version -> new ProtocolVersion(version.getProtocol().getName(), version.getProtocol().getVersion()))
                    .toArray(ProtocolVersion[]::new);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol versions", throwable);
            return new ProtocolVersion[0];
        }
    }

    public void unsafeOpenVfpProtocolSelection() {
        try {
            var currentScreen = MinecraftClient.getInstance().currentScreen;
            if (currentScreen == null) {
                currentScreen = new TitleScreen();
            }

            ProtocolSelectionScreen.INSTANCE.open(currentScreen);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to open ViaFabricPlus screen", throwable);
        }
    }

    public void unsafeSelectProtocolVersion(int protocolId) {
        try {
            VersionEnum version = VersionEnum.fromProtocolId(protocolId);
            if (version == null) {
                throw new IllegalStateException("Protocol version " + protocolId + " not found");
            }

            ProtocolHack.setTargetVersion(version);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to select protocol version", throwable);
        }
    }

    public boolean isOldCombat() {
        try {
            var version = ProtocolHack.getTargetVersion();

            // Check if the version is older or equal than 1.8
            return version.isOlderThanOrEqualTo(VersionEnum.r1_8);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
            return false;
        }
    }

}
