/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDownloadTerrain.class)
public abstract class MixinGuiDownloadTerrain extends MixinGuiScreen {

    @Inject(method = "initGui", at = @At(value = "RETURN"))
    private void injectDisconnectButton(CallbackInfo ci) {
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120 + 12, I18n.format("gui.cancel")));
    }

    @Override
    protected void injectedActionPerformed(GuiButton button) {
        if (button.id == 0) {
            boolean flag = mc.isIntegratedServerRunning();
            boolean flag1 = mc.isConnectedToRealms();
            button.enabled = false;
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);

            if (flag) {
                mc.displayGuiScreen(new GuiMainMenu());
            } else if (flag1) {
                RealmsBridge realmsbridge = new RealmsBridge();
                realmsbridge.switchToRealms(new GuiMainMenu());
            } else {
                mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            }
        }

    }
}
