/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.entity;

import net.ccbluex.liquidbounce.event.AttackEvent;
import net.ccbluex.liquidbounce.event.ClickWindowEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.utils.CooldownHelper;
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
@SideOnly(Side.CLIENT)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPlayerInteractionManager;syncCurrentPlayItem()V"))
    private void attackEntity(PlayerEntity PlayerEntity, Entity targetEntity, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new AttackEvent(targetEntity));
        CooldownHelper.INSTANCE.resetLastAttackedTicks();
    }

    @Inject(method = "getIsHittingBlock", at = @At("HEAD"), cancellable = true)
    private void getIsHittingBlock(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (AbortBreaking.INSTANCE.handleEvents())
            callbackInfoReturnable.setReturnValue(false);
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode, PlayerEntity playerIn, CallbackInfoReturnable<ItemStack> callbackInfo) {
        final ClickWindowEvent event = new ClickWindowEvent(windowId, slotId, mouseButtonClicked, mode);
        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled()) {
            callbackInfo.cancel();
            return;
        }

        // Only reset click delay, if a click didn't get cancelled
        InventoryUtils.INSTANCE.getCLICK_TIMER().reset();
    }
}