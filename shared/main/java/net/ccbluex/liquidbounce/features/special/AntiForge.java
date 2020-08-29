/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special;

import io.netty.buffer.Unpooled;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketCustomPayload;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public class AntiForge extends MinecraftInstance implements Listenable {

    public static boolean enabled = true;
    public static boolean blockFML = true;
    public static boolean blockProxyPacket = true;
    public static boolean blockPayloadPackets = true;

    @EventTarget
    public void onPacket(PacketEvent event) {
        final IPacket packet = event.getPacket();

        if (enabled && !mc.isIntegratedServerRunning()) {
            try {
                if (blockProxyPacket && packet.getClass().getName().equals("net.minecraftforge.fml.common.network.internal.FMLProxyPacket"))
                    event.cancelEvent();

                if (blockPayloadPackets && classProvider.isCPacketCustomPayload(packet)) {
                    ICPacketCustomPayload customPayload = packet.asCPacketCustomPayload();

                    if (!customPayload.getChannelName().startsWith("MC|"))
                        event.cancelEvent();
                    else if (customPayload.getChannelName().equalsIgnoreCase("MC|Brand"))
                        customPayload.setData(classProvider.createPacketBuffer(Unpooled.buffer()).writeString("vanilla"));
                }
            }catch(final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleEvents() {
        return true;
    }
}