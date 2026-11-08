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
package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * The entity whose shield is being submitted, for {@link net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams.Shield}.
 */
public final class ShieldTintContext {

    private static final ScopedValue<LivingEntity> HOLDER = ScopedValue.newInstance();

    private ShieldTintContext() {
    }

    public static void render(LivingEntity holder, Runnable render) {
        ScopedValue.where(HOLDER, holder).run(render);
    }

    public static @Nullable LivingEntity holder() {
        return HOLDER.isBound() ? HOLDER.get() : null;
    }

}
