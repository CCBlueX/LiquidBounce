/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockAir;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

@ModuleInfo(name = "WallClimb", description = "Allows you to climb up walls like a spider.", category = ModuleCategory.MOVEMENT)
public class WallClimb extends Module {

    private final ListValue modeValue = new ListValue("Mode", new String[] {"Simple", "CheckerClimb", "Clip", "AAC3.3.12", "AACGlide"}, "Simple");
    private final ListValue clipMode = new ListValue("ClipMode", new String[] {"Jump", "Fast"}, "Fast");
    private final FloatValue checkerClimbMotionValue = new FloatValue("CheckerClimbMotion", 0F, 0F, 1F);

    private boolean glitch;
    private int waited;

    @EventTarget
    public void onMove(MoveEvent event) {
        if(!mc.thePlayer.isCollidedHorizontally || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava())
            return;

        if("simple".equalsIgnoreCase(modeValue.get())) {
            event.setY(0.2D);
            mc.thePlayer.motionY = 0D;
        }
    }

    @EventTarget
    public void onUpdate(MotionEvent event) {
        if(event.getEventState() != EventState.POST)
            return;

        switch(modeValue.get().toLowerCase()) {
            case "clip":
                if(mc.thePlayer.motionY < 0)
                    glitch = true;

                if(mc.thePlayer.isCollidedHorizontally) {
                    switch(clipMode.get().toLowerCase()) {
                        case "jump":
                            if(mc.thePlayer.onGround)
                                mc.thePlayer.jump();
                            break;
                        case "fast":
                            if(mc.thePlayer.onGround)
                                mc.thePlayer.motionY = .42;
                            else if(mc.thePlayer.motionY < 0)
                                mc.thePlayer.motionY = -0.3;
                            break;
                    }
                }
                break;
            case "checkerclimb":
                final boolean isInsideBlock = BlockUtils.collideBlockIntersects(mc.thePlayer.getEntityBoundingBox(), block -> !(block instanceof BlockAir));
                final float motion = checkerClimbMotionValue.get();

                if(isInsideBlock && motion != 0F)
                    mc.thePlayer.motionY = motion;
                break;
            case "aac3.3.12":
                if(mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder()) {
                    waited++;

                    if(waited == 1)
                        mc.thePlayer.motionY = 0.43;

                    if(waited == 12)
                        mc.thePlayer.motionY = 0.43;

                    if(waited == 23)
                        mc.thePlayer.motionY = 0.43;

                    if(waited == 29)
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.5, mc.thePlayer.posZ);

                    if(waited >= 30)
                        waited = 0;
                }else if(mc.thePlayer.onGround)
                    waited = 0;
                break;
            case "aacglide":
                if(!mc.thePlayer.isCollidedHorizontally || mc.thePlayer.isOnLadder())
                    return;

                mc.thePlayer.motionY = -0.19;
                break;
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if(packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            if(glitch) {
                final float yaw = (float) MovementUtils.getDirection();

                packetPlayer.x = packetPlayer.x - MathHelper.sin(yaw) * 0.00000001D;
                packetPlayer.z = packetPlayer.z + MathHelper.cos(yaw) * 0.00000001D;

                glitch = false;
            }
        }
    }

    @EventTarget
    public void onBlockBB(final BlockBBEvent event) {
        if(mc.thePlayer == null)
            return;

        final String mode = modeValue.get();

        switch(mode.toLowerCase()) {
            case "checkerclimb":
                if(event.getY() > mc.thePlayer.posY)
                    event.setBoundingBox(null);
                break;
            case "clip":
                if(event.getBlock() != null && mc.thePlayer != null && event.getBlock() instanceof BlockAir && event.getY() < mc.thePlayer.posY && mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava())
                    event.setBoundingBox(new AxisAlignedBB(0, 0, 0, 1, 1, 1).offset(mc.thePlayer.posX, (int) mc.thePlayer.posY - 1, mc.thePlayer.posZ));
                break;
        }
    }
}
