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

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED
 */
public interface VfpApi {

    VfpApi INSTANCE = detect();

    ProtocolVersion targetVersion();

    void setTargetVersion(ProtocolVersion version);

    @Nullable UserConnection userConnection();

    void openProtocolSelectionScreen(Screen parent);

    void addChangeProtocolVersionListener(BiConsumer<ProtocolVersion, ProtocolVersion> listener);

    private static VfpApi detect() {
        var version = FabricLoader.getInstance()
                .getModContainer("viafabricplus")
                .orElseThrow()
                .getMetadata()
                .getVersion();

        return version instanceof SemanticVersion semantic && semantic.getVersionComponent(0) >= 5
                ? new VfpApi5()
                : new VfpApi4();
    }

}
