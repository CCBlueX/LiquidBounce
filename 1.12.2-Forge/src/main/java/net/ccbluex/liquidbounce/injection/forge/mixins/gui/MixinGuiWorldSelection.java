package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.GuiWorldSelection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiWorldSelection.class)
public abstract class MixinGuiWorldSelection extends MixinGuiScreen
{

	@Inject(method = "drawScreen", at = @At("HEAD"))
	private void injectDrawDefaultBackground(final int mouseX, final int mouseY, final float partialTicks, final CallbackInfo ci)
	{
		drawDefaultBackground();
	}

}
