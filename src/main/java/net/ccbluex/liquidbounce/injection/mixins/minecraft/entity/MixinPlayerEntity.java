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
import net.ccbluex.liquidbounce.event.PlayerSafeWalkEvent;
import net.ccbluex.liquidbounce.event.PlayerStrideEvent;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiReducedDebugInfo;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {

    @Shadow
    @Final
    public PlayerInventory inventory;

    /**
     * Hook player stride event
     */
    @ModifyVariable(
            method = "tickMovement",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;strideDistance:F",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setMovementSpeed(F)V"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")
            ),
            index = 1,
            ordinal = 0,
            require = 1,
            allow = 1
    )
    private float hookStrideForce(float strideForce) {
        final PlayerStrideEvent event = new PlayerStrideEvent(strideForce);
        EventManager.INSTANCE.callEvent(event);
        return event.getStrideForce();
    }

    /**
     * Hook silent inventory feature
     */
    @Redirect(method = "getEquippedStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hookMainHandStack(PlayerInventory playerInventory) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if ((Object) this != player)
            return this.inventory.getMainHandStack();

        int slot = SilentHotbar.INSTANCE.getServersideSlot();

        return PlayerInventory.isValidHotbarIndex(slot) ? player.getInventory().main.get(slot) : ItemStack.EMPTY;
    }

    /**
     * Hook safe walk event
     */
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void hookSafeWalk(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        final PlayerSafeWalkEvent event = EventManager.INSTANCE.callEvent(new PlayerSafeWalkEvent());

        if (event.isSafeWalk()) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * There are a few velocity changes when attacking an entity, which could be easily detected by anti-cheats when a different server-side rotation is applied.
     */
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(PlayerEntity entity) {
        if (RotationManager.INSTANCE.getActiveConfigurable() == null || !RotationManager.INSTANCE.getActiveConfigurable().getFixVelocity())
            return entity.getYaw();

        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        if (currentRotation == null)
            return entity.getYaw();

        currentRotation = currentRotation.fixedSensitivity();

        return currentRotation.getYaw();
    }

    @Inject(method = "hasReducedDebugInfo", at = @At("HEAD"), cancellable = true)
    private void injectReducedDebugInfo(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ModuleAntiReducedDebugInfo.INSTANCE.getEnabled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

}
