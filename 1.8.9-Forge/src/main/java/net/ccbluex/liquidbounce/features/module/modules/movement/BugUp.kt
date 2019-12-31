package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "BugUp", description = "Automatically setbacks you after falling a certain distance.", category = ModuleCategory.MOVEMENT)
public class BugUp extends Module {

    private final ListValue modeValue = new ListValue("Mode", new String[] {"TeleportBack", "FlyFlag", "OnGroundSpoof"}, "FlyFlag");
    private final FloatValue fallDistanceValue = new FloatValue("FallDistance", 2F, 1F, 5F);

    private double prevX;
    private double prevY;
    private double prevZ;

    @Override
    public void onDisable() {
        prevX = 0;
        prevY = 0;
        prevZ = 0;
    }

    @EventTarget
    public void onUpdate(final UpdateEvent e) {
        if(mc.thePlayer.onGround) {
            prevX = mc.thePlayer.posX;
            prevY = mc.thePlayer.posY;
            prevZ = mc.thePlayer.posZ;
        }

        if(mc.thePlayer.fallDistance > fallDistanceValue.get()) {
            final String mode = modeValue.get();

            switch(mode.toLowerCase()) {
                case "teleportback":
                    mc.thePlayer.setPositionAndUpdate((int) prevX, (int) prevY, (int) prevZ);
                    mc.thePlayer.fallDistance = 0F;
                    mc.thePlayer.motionY = 0;
                    break;
                case "flyflag":
                    mc.thePlayer.motionY += 0.1;
                    mc.thePlayer.fallDistance = 0F;
                    break;
                case "ongroundspoof":
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                    break;
            }
        }
    }
}