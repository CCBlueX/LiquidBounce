/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.timer.TickTimer;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockLiquid;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

@ModuleInfo(name = "NoFall", description = "Prevents you from taking fall damage.", category = ModuleCategory.PLAYER)
public class NoFall extends Module {

    public final ListValue modeValue = new ListValue("Mode", new String[]{"SpoofGround", "NoGround", "Packet", "AAC", "LAAC", "AAC3.3.11", "AAC3.3.15", "Spartan", "CubeCraft", "Hypixel"}, "SpoofGround");

    private int state;
    private boolean jumped;
    private final TickTimer spartanTimer = new TickTimer();

    private int hypixel;

    @EventTarget(ignoreCondition = true)
    public void onUpdate(UpdateEvent event) {
        if(mc.thePlayer.onGround)
            jumped = false;

        if(mc.thePlayer.motionY > 0)
            jumped = true;

        if (!getState() || LiquidBounce.moduleManager.getModule(FreeCam.class).getState())
            return;

        if(BlockUtils.collideBlock(mc.thePlayer.getEntityBoundingBox(), block -> block instanceof BlockLiquid) ||
                BlockUtils.collideBlock(new AxisAlignedBB(mc.thePlayer.getEntityBoundingBox().maxX, mc.thePlayer.getEntityBoundingBox().maxY, mc.thePlayer.getEntityBoundingBox().maxZ, mc.thePlayer.getEntityBoundingBox().minX, mc.thePlayer.getEntityBoundingBox().minY - 0.01D, mc.thePlayer.getEntityBoundingBox().minZ), block -> block instanceof BlockLiquid))
            return;

        switch(modeValue.get().toLowerCase()) {
            case "packet":
                if(mc.thePlayer.fallDistance > 2F)
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                break;
            case "cubecraft":
                if(mc.thePlayer.fallDistance > 2F) {
                    mc.thePlayer.onGround = false;
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                }
                break;
            case "aac":
                if(mc.thePlayer.fallDistance > 2F) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                    state = 2;
                }else if(state == 2 && mc.thePlayer.fallDistance < 2) {
                    mc.thePlayer.motionY = 0.1D;
                    state = 3;
                    return;
                }

                switch(state) {
                    case 3:
                        mc.thePlayer.motionY = 0.1D;
                        state = 4;
                        break;
                    case 4:
                        mc.thePlayer.motionY = 0.1D;
                        state = 5;
                        break;
                    case 5:
                        mc.thePlayer.motionY = 0.1D;
                        state = 1;
                        break;
                }
                break;
            case "laac":
                if(!jumped && mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater()
                        && !mc.thePlayer.isInWeb)
                    mc.thePlayer.motionY = -6;
                break;
            case "aac3.3.11":
                if(mc.thePlayer.fallDistance > 2) {
                    mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY - 10E-4D, mc.thePlayer.posZ, mc.thePlayer.onGround));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                }
                break;
            case "aac3.3.15":
                if(mc.thePlayer.fallDistance > 2) {
                    if(!mc.isIntegratedServerRunning())
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                                Double.NaN, mc.thePlayer.posZ, false));
                    mc.thePlayer.fallDistance = -9999;
                }
                break;
            case "spartan":
                spartanTimer.update();

                if(mc.thePlayer.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY + 10, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY - 10, mc.thePlayer.posZ, true));
                    spartanTimer.reset();
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();
        final String mode = modeValue.get();

        if (packet instanceof C03PacketPlayer) {
            final C03PacketPlayer playerPacket = (C03PacketPlayer) packet;

            if (mode.equalsIgnoreCase("SpoofGround"))
                playerPacket.onGround = true;

            if (mode.equalsIgnoreCase("NoGround"))
                playerPacket.onGround = false;

            if (mode.equalsIgnoreCase("Hypixel")
                    && mc.thePlayer != null && mc.thePlayer.fallDistance > 1.5)
                playerPacket.onGround = mc.thePlayer.ticksExisted % 2 == 0;
        }
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if(BlockUtils.collideBlock(mc.thePlayer.getEntityBoundingBox(), block -> block instanceof BlockLiquid) || BlockUtils.collideBlock(new AxisAlignedBB(mc.thePlayer.getEntityBoundingBox().maxX, mc.thePlayer.getEntityBoundingBox().maxY, mc.thePlayer.getEntityBoundingBox().maxZ, mc.thePlayer.getEntityBoundingBox().minX, mc.thePlayer.getEntityBoundingBox().minY - 0.01D, mc.thePlayer.getEntityBoundingBox().minZ), block -> block instanceof BlockLiquid))
            return;

        if (modeValue.get().equalsIgnoreCase("laac")) {
            if (!jumped && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInWeb && mc.thePlayer.motionY < 0D) {
                event.setX(0);
                event.setZ(0);
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    public void onJump(final JumpEvent event) {
        jumped = true;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
