/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.EntityMarginEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleNoPitchLimit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow public float yaw;

    @Shadow public float pitch;

    @Shadow public float prevPitch;

    @Shadow public float prevYaw;

    @Shadow @Nullable private Entity vehicle;

    /**
     * Hook entity margin modification event
     */
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void hookMargin(CallbackInfoReturnable<Float> callback) {
        final EntityMarginEvent marginEvent = new EntityMarginEvent((Entity) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(marginEvent);
        callback.setReturnValue(marginEvent.getMargin());
    }

    /**
     * Overwrite changeLookDirection to add NoPitchLimit modification
     *
     * @author Mojang / CCBlueX
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
        final boolean noLimit = ModuleNoPitchLimit.INSTANCE.getEnabled();

        double d = cursorDeltaY * 0.15D;
        double e = cursorDeltaX * 0.15D;
        this.pitch = (float)((double)this.pitch + d);
        this.yaw = (float)((double)this.yaw + e);
        if (!noLimit) {
            this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
        }
        this.prevPitch = (float)((double)this.prevPitch + d);
        this.prevYaw = (float)((double)this.prevYaw + e);
        if (!noLimit) {
            this.prevPitch = MathHelper.clamp(this.prevPitch, -90.0F, 90.0F);
        }
        if (this.vehicle != null) {
            this.vehicle.onPassengerLookAround((Entity) (Object) this);
        }

    }

}
