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
public abstract class MixinGuiDownloadTerrain extends MixinGuiScreen
{

    @Inject(method = "initGui", at = @At("RETURN"))
    private void injectDisconnectButton(final CallbackInfo ci)
    {
        buttonList.add(new GuiButton(0, (width >> 1) - 100, (height >> 2) + 120 + 12, I18n.format("gui.cancel")));
    }

    @Override
    protected void injectedActionPerformed(final GuiButton button)
    {
        if (button.id == 0)
        {
            final boolean integratedServerRunning = mc.isIntegratedServerRunning();
            final boolean connectedToRealms = mc.isConnectedToRealms();
            button.enabled = false;
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);

            if (integratedServerRunning)
                mc.displayGuiScreen(new GuiMainMenu());
            else if (connectedToRealms)
            {
                final RealmsBridge realmsbridge = new RealmsBridge();
                realmsbridge.switchToRealms(new GuiMainMenu());
            }
            else
                mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
        }
    }
}
