/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@ModuleInfo(name = "AtAllProvider", description = "Automatically mentions everyone on the server when using '@a' in your message.", category = ModuleCategory.MISC)
public class AtAllProvider extends Module {

    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 1000, 0, 20000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = minDelayValue.get();

            if(i > newValue)
                set(i);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 500, 0, 20000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = maxDelayValue.get();

            if(i < newValue)
                set(i);
        }
    };

    private final BoolValue retryValue = new BoolValue("Retry", false);

    private final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private final List<String> retryQueue = new ArrayList<>();
    private final MSTimer msTimer = new MSTimer();
    private long delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

    @Override
    public void onDisable() {
        synchronized(sendQueue) {
            sendQueue.clear();
        }

        synchronized(retryQueue) {
            retryQueue.clear();
        }
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(!msTimer.hasTimePassed(delay))
            return;

        try {
            synchronized(sendQueue) {
                if(sendQueue.isEmpty()) {
                    if(!retryValue.get() || retryQueue.isEmpty())
                        return;
                    else
                        sendQueue.addAll(retryQueue);
                }

                mc.thePlayer.sendChatMessage(sendQueue.take());
                msTimer.reset();
                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
            }
        }catch(final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if(event.getPacket() instanceof C01PacketChatMessage) {
            final C01PacketChatMessage packetChatMessage = (C01PacketChatMessage) event.getPacket();
            final String message = packetChatMessage.getMessage();

            if(message.contains("@a")) {
                synchronized(sendQueue) {
                    for(final NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                        final String playerName = playerInfo.getGameProfile().getName();

                        if(playerName.equals(mc.thePlayer.getName()))
                            continue;

                        sendQueue.add(message.replace("@a", playerName));
                    }

                    if(retryValue.get()) {
                        synchronized(retryQueue) {
                            retryQueue.clear();
                            retryQueue.addAll(Arrays.asList(sendQueue.toArray(new String[0])));
                        }
                    }
                }

                event.cancelEvent();
            }
        }
    }

}