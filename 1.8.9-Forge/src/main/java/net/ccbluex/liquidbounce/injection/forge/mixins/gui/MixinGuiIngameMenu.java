/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends MixinGuiScreen
{

	@Inject(method = "initGui", at = @At("RETURN"))
	private void initGui(final CallbackInfo callbackInfo)
	{
		if (!mc.isIntegratedServerRunning())
			buttonList.add(new GuiButton(1337, width / 2 - 100, height / 4 + 128, "Reconnect"));
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"))
	private void actionPerformed(final GuiButton button, final CallbackInfo callbackInfo)
	{
		if (button.id == 1337)
		{
			mc.theWorld.sendQuittingDisconnectingPacket();
			ServerUtils.connectToLastServer();
		}
	}
}
