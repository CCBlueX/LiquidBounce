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

package net.ccbluex.liquidbounce.utils.client.vfp;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.api.ViaFabricPlusBase;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;

/**
 * The 5.x API jar we compile against no longer declares the change callback, hence the reflection.
 */
@SuppressWarnings({"deprecation", "removal"})
final class VfpApi4 implements VfpApi {

    private static final String CHANGE_CALLBACK = "com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback";

    @Override
    public ProtocolVersion targetVersion() {
        return ViaFabricPlus.getImpl().getTargetVersion();
    }

    @Override
    public void setTargetVersion(ProtocolVersion version) {
        ViaFabricPlus.getImpl().setTargetVersion(version);
    }

    @Override
    public @Nullable UserConnection userConnection() {
        return ViaFabricPlus.getImpl().getPlayNetworkUserConnection();
    }

    @Override
    public void openProtocolSelectionScreen(Screen parent) {
        ViaFabricPlus.getImpl().openProtocolSelectionScreen(parent);
    }

    @Override
    public void addChangeProtocolVersionListener(BiConsumer<ProtocolVersion, ProtocolVersion> listener) {
        try {
            var callbackType = Class.forName(CHANGE_CALLBACK);
            var accept = MethodHandles.lookup()
                    .findVirtual(BiConsumer.class, "accept", MethodType.methodType(void.class, Object.class, Object.class))
                    .bindTo(listener);
            var callback = MethodHandleProxies.asInterfaceInstance(callbackType, accept);

            ViaFabricPlusBase.class
                    .getMethod("registerOnChangeProtocolVersionCallback", callbackType)
                    .invoke(ViaFabricPlus.getImpl(), callback);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ViaFabricPlus 4.x change callback is unavailable", e);
        }
    }

}
