/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special;

import io.netty.buffer.Unpooled;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

public class AntiForge extends MinecraftInstance implements Listenable {

    public static boolean enabled = true;
    public static boolean blockFML = true;
    public static boolean blockProxyPacket = true;
    public static boolean blockPayloadPackets = true;

    @EventTarget
    public void onPacket(PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if (enabled && !mc.isIntegratedServerRunning()) {
            try {
                if (blockProxyPacket && packet.getClass().getName().equals("net.minecraftforge.fml.common.network.internal.FMLProxyPacket"))
                    event.cancelEvent();

                if (blockPayloadPackets && packet instanceof C17PacketCustomPayload) {
                    final C17PacketCustomPayload customPayload = (C17PacketCustomPayload) packet;

                    if (!customPayload.getChannelName().startsWith("MC|"))
                        event.cancelEvent();
                    else if (customPayload.getChannelName().equalsIgnoreCase("MC|Brand"))
                        customPayload.data = new PacketBuffer(Unpooled.buffer()).writeString("vanilla");
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