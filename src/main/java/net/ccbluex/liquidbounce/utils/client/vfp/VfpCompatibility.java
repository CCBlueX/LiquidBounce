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

package net.ccbluex.liquidbounce.utils.client.vfp;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.client.ClientProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Compatibility layer for ViaFabricPlus
 * <p>
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED
 */
public enum VfpCompatibility {

    INSTANCE;

    public ClientProtocolVersion unsafeGetProtocolVersion() {
        try {
            ProtocolVersion version = ViaFabricPlus.getImpl().getTargetVersion();
            return new ClientProtocolVersion(version.getName(), version.getVersion());
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol version", throwable);
            return null;
        }
    }

    public ClientProtocolVersion[] unsafeGetProtocolVersions() {
        try {
            var protocols = ProtocolVersion.getProtocols()
                    .stream()
                    .filter(version -> version.getVersionType() == VersionType.RELEASE)
                    .map(version -> new ClientProtocolVersion(version.getName(), version.getVersion()))
                    .toArray(ClientProtocolVersion[]::new);

            ArrayUtils.reverse(protocols);
            return protocols;
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol versions", throwable);
            return new ClientProtocolVersion[0];
        }
    }

    public void unsafeOpenVfpProtocolSelection() {
        try {
            var currentScreen = MinecraftClient.getInstance().currentScreen;
            if (currentScreen == null) {
                currentScreen = new TitleScreen();
            }

            ViaFabricPlus.getImpl().openProtocolSelectionScreen(currentScreen);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to open ViaFabricPlus screen", throwable);
        }
    }

    public void unsafeSelectProtocolVersion(int protocolId) {
        try {
            if (!ProtocolVersion.isRegistered(protocolId)) {
                throw new IllegalArgumentException("Protocol version is not registered");
            }

            ProtocolVersion version = ProtocolVersion.getProtocol(protocolId);
            ViaFabricPlus.getImpl().setTargetVersion(version);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to select protocol version", throwable);
        }
    }

    public boolean isEqual1_8() {
        try {
            var version = ViaFabricPlus.getImpl().getTargetVersion();

            // Check if the version is equal to 1.8
            return version.equalTo(ProtocolVersion.v1_8);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
            return false;
        }
    }

    public boolean isOlderThanOrEqual1_8() {
        try {
            var version = ViaFabricPlus.getImpl().getTargetVersion();

            // Check if the version is older or equal than 1.8
            return version.olderThanOrEqualTo(ProtocolVersion.v1_8);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
            return false;
        }
    }

    public boolean isOlderThanOrEqual1_7_10() {
        try {
            var version = ViaFabricPlus.getImpl().getTargetVersion();

            // Check if the version is older or equal than 1.7.10
            return version.olderThanOrEqualTo(ProtocolVersion.v1_7_6);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
            return false;
        }
    }

    public boolean isNewerThanOrEqual1_16() {
        try {
            var version = ViaFabricPlus.getImpl().getTargetVersion();

            // Check if the version is older or equal than 1.12.2
            return version.newerThanOrEqualTo(ProtocolVersion.v1_16);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if 1.16", throwable);
            return false;
        }
    }

    public boolean isOlderThanOrEqual1_11_1() {
        try {
            var version = ViaFabricPlus.getImpl().getTargetVersion();

            // Check if the version is older or equal than 1.11.1
            return version.olderThanOrEqualTo(ProtocolVersion.v1_11_1);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if 1.11.1", throwable);
            return false;
        }
    }
}
