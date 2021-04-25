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
import net.ccbluex.liquidbounce.event.PlayerStrideEvent;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {

    @Shadow @Final public PlayerInventory inventory;

    /**
     * Hook player stride event
     */
    @Redirect(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;onGround:Z"),
            require = 1, allow = 1)
    private boolean hookStrideGround(PlayerEntity playerEntity) {
        final PlayerStrideEvent event = new PlayerStrideEvent(false);
        EventManager.INSTANCE.callEvent(event);
        if (event.getStrideOnAir()) {
            return true;
        }
        return playerEntity.isOnGround();
    }

    @Redirect(method = "getEquippedStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hookMainHandStack(PlayerInventory playerInventory) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if ((Object) this != player)
            return this.inventory.getMainHandStack();

        int slot = SilentHotbar.INSTANCE.getServersideSlot();

        return PlayerInventory.isValidHotbarIndex(slot) ? player.inventory.main.get(slot) : ItemStack.EMPTY;
    }

}
