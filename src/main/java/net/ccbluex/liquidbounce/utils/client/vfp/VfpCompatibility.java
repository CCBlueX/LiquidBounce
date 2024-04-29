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

//import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
//import com.viaversion.viaversion.api.protocol.version.VersionType;
//import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
//import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen;
//import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.client.ClientProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Compatibility layer for ViaFabricPlus
 *
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED
 */
public enum VfpCompatibility {

    INSTANCE;

    // TODO Fix this when ViaFabricPlus is updated

    public void unsafeDsableConflictingVfpOptions() {
        throw new IllegalStateException(":c");
//        try {
//            VisualSettings visualSettings = VisualSettings.global();
//
//            // 1 == off, 0 == on
//            visualSettings.enableSwordBlocking.setValue(1);
//            visualSettings.enableBlockHitAnimation.setValue(1);
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to disable conflicting options", throwable);
//        }
    }

    public ClientProtocolVersion unsafeGetProtocolVersion() {
        throw new IllegalStateException(":c");
//        try {
//            ProtocolVersion version = ProtocolTranslator.getTargetVersion();
//            return new ClientProtocolVersion(version.getName(), version.getVersion());
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol version", throwable);
//            return null;
//        }
    }

    public ClientProtocolVersion[] unsafeGetProtocolVersions() {
        throw new IllegalStateException(":c");
//        try {
//            var protocols = ProtocolVersion.getProtocols()
//                    .stream()
//                    .filter(version -> version.getVersionType() == VersionType.RELEASE)
//                    .map(version -> new ClientProtocolVersion(version.getName(), version.getVersion()))
//                    .toArray(ClientProtocolVersion[]::new);
//
//            ArrayUtils.reverse(protocols);
//            return protocols;
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol versions", throwable);
//            return new ClientProtocolVersion[0];
//        }
    }

    public void unsafeOpenVfpProtocolSelection() {
        throw new IllegalStateException(":c");
//        try {
//            var currentScreen = MinecraftClient.getInstance().currentScreen;
//            if (currentScreen == null) {
//                currentScreen = new TitleScreen();
//            }
//
//            ProtocolSelectionScreen.INSTANCE.open(currentScreen);
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to open ViaFabricPlus screen", throwable);
//        }
    }

    public void unsafeSelectProtocolVersion(int protocolId) {
        throw new IllegalStateException(":c");
//        try {
//            if (!ProtocolVersion.isRegistered(protocolId)) {
//                throw new IllegalArgumentException("Protocol version is not registered");
//            }
//
//            ProtocolVersion version = ProtocolVersion.getProtocol(protocolId);
//            ProtocolTranslator.setTargetVersion(version);
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to select protocol version", throwable);
//        }
    }

    public boolean isOldCombat() {
        throw new IllegalStateException(":c");
//        try {
//            var version = ProtocolTranslator.getTargetVersion();
//
//            // Check if the version is older or equal than 1.8
//            return version.olderThanOrEqualTo(ProtocolVersion.v1_8);
//        } catch (Throwable throwable) {
//            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
//            return false;
//        }
    }

}
