/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof;
import net.ccbluex.liquidbounce.ui.client.GuiClientFixes;
import net.ccbluex.liquidbounce.ui.client.tools.GuiTools;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

import static net.ccbluex.liquidbounce.LiquidBounce.fileManager;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends MixinGuiScreen {

    private GuiButton bungeeCordSpoofButton;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        buttonList.add(new GuiButton(997, 5, 8, 45, 20, "Fixes"));
        buttonList.add(bungeeCordSpoofButton = new GuiButton(998, 55, 8, 98, 20, "BungeeCord Spoof: " + (BungeeCordSpoof.enabled ? "On" : "Off")));
        buttonList.add(new GuiButton(999, width - 104, 8, 98, 20, "Tools"));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) throws IOException {
        switch (button.id) {
            case 997:
                mc.displayGuiScreen(new GuiClientFixes((GuiScreen) (Object) this));
                break;
            case 998:
                BungeeCordSpoof.enabled = !BungeeCordSpoof.enabled;
                bungeeCordSpoofButton.displayString = "BungeeCord Spoof: " + (BungeeCordSpoof.enabled ? "On" : "Off");
                fileManager.getValuesConfig().saveConfig();
                break;
            case 999:
                mc.displayGuiScreen(new GuiTools((GuiScreen) (Object) this));
                break;
        }
    }
}