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

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.client.ClientProtocolVersion;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility layer for ViaFabricPlus
 *
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED
 */
public enum Vfp306Compatibility {

    INSTANCE;

    public ClientProtocolVersion unsafeGetProtocolVersion() {
        try {
            Class<?> protocolHackClass = Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack");
            Method getTargetVersionMethod = protocolHackClass.getMethod("getTargetVersion");
            Object versionEnum = getTargetVersionMethod.invoke(null);
            return new ClientProtocolVersion((String) versionEnum.getClass().getMethod("getName").invoke(versionEnum),
                    (Integer) versionEnum.getClass().getMethod("getVersion").invoke(versionEnum));
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol version", throwable);
            return null;
        }
    }

    public ClientProtocolVersion[] unsafeGetProtocolVersions() {
        try {
            Class<?> versionEnumClass = Class.forName("net.raphimc.vialoader.util.VersionEnum");
            Field sortedVersionsField = versionEnumClass.getField("SORTED_VERSIONS");
            List sortedVersions = (List) sortedVersionsField.get(null);
            return (ClientProtocolVersion[]) sortedVersions.stream()
                    .map(version -> {
                        try {
                            return new ClientProtocolVersion((String) version.getClass().getMethod("getName").invoke(version),
                                    (Integer) version.getClass().getMethod("getVersion").invoke(version));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(ClientProtocolVersion[]::new);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to get protocol versions", throwable);
            return new ClientProtocolVersion[0];
        }
    }

    public void unsafeSelectProtocolVersion(int protocolId) {
        try {
            Class<?> versionEnumClass = Class.forName("net.raphimc.vialoader.util.VersionEnum");
            Method fromProtocolIdMethod = versionEnumClass.getMethod("fromProtocolId", int.class);
            Object versionEnum = fromProtocolIdMethod.invoke(null, protocolId);

            if (versionEnum == null) {
                throw new IllegalStateException("Protocol version " + protocolId + " not found");
            }

            Class<?> protocolHackClass = Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack");
            Method setTargetVersionMethod = protocolHackClass.getMethod("setTargetVersion", versionEnumClass);
            setTargetVersionMethod.invoke(null, versionEnum);
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to select protocol version", throwable);
        }
    }

    public boolean isOldCombat() {
        try {
            Class<?> protocolHackClass = Class.forName("de.florianmichael.viafabricplus.protocolhack.ProtocolHack");
            Method getTargetVersionMethod = protocolHackClass.getMethod("getTargetVersion");
            Object versionEnum = getTargetVersionMethod.invoke(null);
            Method isOlderThanOrEqualToMethod = versionEnum.getClass().getMethod("isOlderThanOrEqualTo", versionEnum.getClass());
            Object result = isOlderThanOrEqualToMethod.invoke(versionEnum,
                    Class.forName("net.raphimc.vialoader.util.VersionEnum").getField("r1_8").get(null));
            return (boolean) result;
        } catch (Throwable throwable) {
            LiquidBounce.INSTANCE.getLogger().error("Failed to check if old combat", throwable);
            return false;
        }
    }

}
