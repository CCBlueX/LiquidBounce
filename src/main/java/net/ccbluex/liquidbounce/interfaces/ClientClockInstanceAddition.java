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
package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;

/**
 * Additions to {@link net.minecraft.client.ClientClockManager.ClientClockInstance}.
 *
 * <p>Since 26.3 clock instances are queried through
 * {@code ClockManager#getInstance(Holder)} without carrying their own definition,
 * so the definition is tagged upon creation to allow overrides for specific clocks.</p>
 */
public interface ClientClockInstanceAddition {

    /**
     * Sets the {@link WorldClock} definition this clock instance belongs to.
     */
    void liquid_bounce$setDefinition(Holder<WorldClock> definition);

}
