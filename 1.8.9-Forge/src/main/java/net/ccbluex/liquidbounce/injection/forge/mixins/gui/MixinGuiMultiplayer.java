/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import static net.ccbluex.liquidbounce.LiquidBounce.wrapper;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.IClassProvider;
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.injection.backend.GuiScreenImplKt;
import net.ccbluex.liquidbounce.ui.client.GuiAntiModDisable;
import net.ccbluex.liquidbounce.ui.client.tools.GuiTools;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends MixinGuiScreen
{

	private GuiButton bungeeCordSpoofButton;

	@Inject(method = "initGui", at = @At("RETURN"))
	private void initGui(final CallbackInfo callbackInfo)
	{
		buttonList.add(new GuiButton(997, 5, 8, 98, 20, "AntiModDisable"));
		buttonList.add(bungeeCordSpoofButton = new GuiButton(998, 108, 8, 98, 20, "BungeeCord Spoof: " + (BungeeCordSpoof.enabled ? "On" : "Off")));
		buttonList.add(new GuiButton(999, width - 104, 8, 98, 20, "Tools"));
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"))
	private void actionPerformed(final GuiButton button, final CallbackInfo callbackInfo)
	{
		final IClassProvider provider = wrapper.getClassProvider();

		switch (button.id)
		{
			case 997:
				mc.displayGuiScreen(GuiScreenImplKt.unwrap(provider.wrapGuiScreen(new GuiAntiModDisable(GuiScreenImplKt.wrap((GuiScreen) (Object) this)))));
				break;
			case 998:
				BungeeCordSpoof.enabled = !BungeeCordSpoof.enabled;
				bungeeCordSpoofButton.displayString = "BungeeCord Spoof: " + (BungeeCordSpoof.enabled ? "\u00A7a(On)" : "\u00A7c(Off)");
				FileManager.Companion.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 999:
				mc.displayGuiScreen(GuiScreenImplKt.unwrap(provider.wrapGuiScreen(new GuiTools(GuiScreenImplKt.wrap((GuiScreen) (Object) this)))));
				break;
		}
	}
}
