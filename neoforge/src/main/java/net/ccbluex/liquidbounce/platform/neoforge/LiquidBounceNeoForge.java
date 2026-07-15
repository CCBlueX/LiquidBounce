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

package net.ccbluex.liquidbounce.platform.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * NeoForge entry point.
 * <p>
 * LiquidBounce bootstraps through {@code MixinMinecraft} firing {@code ClientStartEvent},
 * identical to the Fabric distribution. This class only hosts the loader event
 * subscriptions that NeoForge requires mods to register through the mod event bus.
 */
@Mod(value = "liquidbounce", dist = Dist.CLIENT)
public final class LiquidBounceNeoForge {

    public LiquidBounceNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(AddClientReloadListenersEvent.class, NeoForgePlatform::onAddReloadListeners);
    }

}
