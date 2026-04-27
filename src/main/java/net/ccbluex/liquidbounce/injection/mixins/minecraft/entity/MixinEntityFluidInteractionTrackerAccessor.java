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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@NullMarked
@Mixin(targets = "net.minecraft.world.entity.EntityFluidInteraction$Tracker")
public interface MixinEntityFluidInteractionTrackerAccessor {

    @Accessor("height")
    double height();

    @Accessor("height")
    void height(double height);

    @Accessor("eyesInside")
    boolean eyesInside();

    @Accessor("eyesInside")
    void eyesInside(boolean eyesInside);

    @Accessor("accumulatedCurrent")
    Vec3 accumulatedCurrent();

    @Accessor("accumulatedCurrent")
    void accumulatedCurrent(Vec3 accumulatedCurrent);

    @Accessor("currentCount")
    int currentCount();

    @Accessor("currentCount")
    void currentCount(int currentCount);

}
