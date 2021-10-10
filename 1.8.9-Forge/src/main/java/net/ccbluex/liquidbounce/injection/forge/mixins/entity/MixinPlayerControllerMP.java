/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3;
import net.ccbluex.liquidbounce.event.AttackEvent;
import net.ccbluex.liquidbounce.event.ClickWindowEvent;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.injection.backend.EntityImplKt;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
@SideOnly(Side.CLIENT)
public class MixinPlayerControllerMP
{
	@Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"))
	private void attackEntity(final EntityPlayer entityPlayer, final Entity targetEntity, final CallbackInfo callbackInfo)
	{
		LiquidBounce.eventManager.callEvent(new AttackEvent(EntityImplKt.wrap(targetEntity), new WVec3(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ)));
	}

	@Inject(method = "getIsHittingBlock", at = @At("HEAD"), cancellable = true)
	private void getIsHittingBlock(final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
	{
		if (LiquidBounce.moduleManager.get(AbortBreaking.class).getState())
			callbackInfoReturnable.setReturnValue(false);
	}

	@Inject(method = "windowClick", at = @At("HEAD"), cancellable = true)
	private void windowClick(final int windowId, final int slotId, final int mouseButtonClicked, final int mode, final EntityPlayer playerIn, final CallbackInfoReturnable<ItemStack> callbackInfo)
	{
		final ClickWindowEvent event = new ClickWindowEvent(windowId, slotId, mouseButtonClicked, mode);
		LiquidBounce.eventManager.callEvent(event);

		if (event.isCancelled())
			callbackInfo.cancel();
	}
}
