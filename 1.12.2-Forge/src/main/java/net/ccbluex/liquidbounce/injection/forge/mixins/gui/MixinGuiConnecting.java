/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.injection.backend.ServerDataImplKt;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.mcleaks.MCLeaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.util.text.TextComponentTranslation;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(GuiConnecting.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiConnecting extends GuiScreen {

    @Shadow
    @Final
    private static AtomicInteger CONNECTION_ID;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    private NetworkManager networkManager;
    @Shadow
    private boolean cancel;
    @Shadow
    @Final
    private GuiScreen previousGuiScreen;

    @Inject(method = "connect", at = @At("HEAD"))
    private void headConnect(final String ip, final int port, CallbackInfo callbackInfo) {
        ServerUtils.serverData = ServerDataImplKt.wrap(new ServerData("", ip + ":" + port, false));
    }

    @Inject(method = "connect", at = @At(value = "NEW", target = "net/minecraft/network/login/client/CPacketLoginStart"), cancellable = true)
    private void mcLeaks(CallbackInfo callbackInfo) {
        if (MCLeaks.isAltActive()) {
            networkManager.sendPacket(new CPacketLoginStart(new GameProfile(null, MCLeaks.getSession().getUsername())));
            callbackInfo.cancel();
        }
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    private void connect(final String ip, final int port) {
        LOGGER.info("Connecting to " + ip + ", " + port);

        new Thread(() -> {
            InetAddress inetaddress = null;

            try {
                if (cancel) {
                    return;
                }

                inetaddress = InetAddress.getByName(ip);
                networkManager = NetworkManager.createNetworkManagerAndConnect(inetaddress, port, mc.gameSettings.isUsingNativeTransport());
                networkManager.setNetHandler(new NetHandlerLoginClient(networkManager, mc, previousGuiScreen));
                networkManager.sendPacket(new C00Handshake(ip, port, EnumConnectionState.LOGIN, true));
                networkManager.sendPacket(new CPacketLoginStart(MCLeaks.isAltActive() ? new GameProfile(null, MCLeaks.getSession().getUsername()) : mc.getSession().getProfile()));
            } catch (UnknownHostException unknownhostexception) {
                if (cancel)
                    return;

                LOGGER.error("Couldn't connect to server", unknownhostexception);
                mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed", new TextComponentTranslation("disconnect.genericReason", "Unknown host")));
            } catch (Exception exception) {
                if (cancel) {
                    return;
                }

                LOGGER.error("Couldn't connect to server", exception);
                String s = exception.toString();

                if (inetaddress != null) {
                    String s1 = inetaddress.toString() + ":" + port;
                    s = s.replaceAll(s1, "");
                }

                mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed", new TextComponentTranslation("disconnect.genericReason", s)));
            }
        }, "Server Connector #" + CONNECTION_ID.incrementAndGet()).start();
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        this.drawDefaultBackground();

        RenderUtils.drawLoadingCircle(scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 4 + 70);

        String ip = "Unknown";

        final ServerData serverData = mc.getCurrentServerData();
        if (serverData != null)
            ip = serverData.serverIP;

        Fonts.font40.drawCenteredString("Connecting to", scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 4 + 110, 0xFFFFFF, true);
        Fonts.font35.drawCenteredString(ip, scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 4 + 120, 0x5281FB, true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}