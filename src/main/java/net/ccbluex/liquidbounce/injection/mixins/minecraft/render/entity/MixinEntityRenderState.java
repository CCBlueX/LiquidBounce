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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity;

import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements EntityRenderStateAddition {

    @Unique
    private Entity liquid_bounce$entity;

    @Unique
    private boolean liquid_bounce$isCustom = false;

    @Unique
    @Override
    public void liquid_bounce$setEntity(Entity entity) {
        this.liquid_bounce$entity = entity;
    }

    @Unique
    @Override
    public Entity liquid_bounce$getEntity() {
        return liquid_bounce$entity;
    }

    @Override
    public boolean liquid_bounce$isCustom() {
        return liquid_bounce$isCustom;
    }

    @Override
    public void liquid_bounce$setCustom(boolean custom) {
        liquid_bounce$isCustom = custom;
    }
}
