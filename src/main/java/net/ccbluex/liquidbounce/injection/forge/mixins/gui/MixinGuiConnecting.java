/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiConnecting.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiConnecting extends GuiScreen
{

    @Shadow
    private NetworkManager networkManager;

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    private boolean cancel;

    @Shadow
    @Final
    private GuiScreen previousGuiScreen;

    @Shadow
    @Final
    private static AtomicInteger CONNECTION_ID;

    @Inject(method = "connect", at = @At("HEAD"))
    private void headConnect(final String ip, final int port, final CallbackInfo callbackInfo)
    {
        ServerUtils.setLastServerData(new ServerData("", ip + ":" + port, false));
    }

    /**
     * @author CCBlueX
     * @reason
     */
    @Override
    @Overwrite
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
    {
        final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        final float middleWidth = scaledResolution.getScaledWidth() >> 1;
        final float quarterHeight = scaledResolution.getScaledHeight() >> 2;

        drawDefaultBackground();

        RenderUtils.drawLoadingCircle(middleWidth, quarterHeight + 70);

        String ip = "Unknown IP";
        String gameVersion = "Unknown Version";
        String protocolVersion = "Unknown Protocol Version";
        Color color = Color.gray;

        final ServerData serverData = mc.getCurrentServerData();
        if (serverData != null)
        {
            ip = serverData.serverIP + (serverData.serverName.isEmpty() ? "" : "(" + serverData.serverName + ")");
            gameVersion = "Minecraft " + serverData.gameVersion;
            protocolVersion = "NetworkManager v" + serverData.version;
            color = Color.cyan;
        }

        Fonts.font40.drawCenteredString("Connecting to", middleWidth, quarterHeight + 110, 0xFFFFFF, true);
        Fonts.font40.drawCenteredString(ip, middleWidth, quarterHeight + 120, color.getRGB(), true);
        Fonts.font35.drawCenteredString(gameVersion, middleWidth, quarterHeight + 160, color.getRGB(), true);
        Fonts.font35.drawCenteredString(protocolVersion, middleWidth, quarterHeight + 170, color.getRGB(), true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
