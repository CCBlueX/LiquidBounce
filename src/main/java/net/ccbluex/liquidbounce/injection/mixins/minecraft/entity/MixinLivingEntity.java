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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiLevitation;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow protected abstract float getJumpVelocity();

    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);

    @Shadow @Nullable public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);

    @Shadow public abstract ItemStack getMainHandStack();

    /**
     * Hook anti levitation module
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    public boolean hookTravelStatusEffect(LivingEntity livingEntity, StatusEffect effect) {
        if ((effect == StatusEffects.LEVITATION || effect == StatusEffects.SLOW_FALLING) &&
                ModuleAntiLevitation.INSTANCE.getEnabled()) {
            livingEntity.fallDistance = 0f;
            return false;
        }

        return livingEntity.hasStatusEffect(effect);
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(StatusEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NAUSEA && ModuleAntiBlind.INSTANCE.getEnabled() && ModuleAntiBlind.INSTANCE.getAntiNausea()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    /**
     * Hook jump event
     * @author mojang
     */
    @Overwrite
    public void jump() {
        float f = this.getJumpVelocity();

        // Check if entity is client user
        if ((Object) this == MinecraftClient.getInstance().player) {
            // Hook player jump event
            final PlayerJumpEvent jumpEvent = new PlayerJumpEvent(f);
            EventManager.INSTANCE.callEvent(jumpEvent);
            if (jumpEvent.isCancelled()) {
                return;
            }
            f = jumpEvent.getMotion();
        }

        if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            f += 0.1F * (float)(this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, f, vec3d.z);
        if (this.isSprinting()) {
            float g = this.yaw * 0.017453292F;
            this.setVelocity(this.getVelocity().add(-MathHelper.sin(g) * 0.2F, 0.0D, MathHelper.cos(g) * 0.2F));
        }

        this.velocityDirty = true;
    }

}
