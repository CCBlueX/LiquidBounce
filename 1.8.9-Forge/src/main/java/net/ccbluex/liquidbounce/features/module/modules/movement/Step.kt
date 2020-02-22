/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSnow;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

@ModuleInfo(name = "Step", description = "Allows you to step up blocks.", category = ModuleCategory.MOVEMENT)
public class Step extends Module {

    private final ListValue modeValue = new ListValue("Mode", new String[] {"Vanilla", "Jump", "NCP", "AAC", "LAAC", "AAC3.3.4", "OldNCP", "Spartan", "Rewinside"}, "NCP");
    private final FloatValue heightValue = new FloatValue("Height", 1F, 0.6F, 10F);
    private final IntegerValue delayValue = new IntegerValue("Delay", 0, 0, 500);

    private boolean isStep;
    private double stepX;
    private double stepY;
    private double stepZ;

    private boolean spartanSwitch;
    private boolean isAACStep;

    private final MSTimer msTimer = new MSTimer();

    @Override
    public void onDisable() {
        if(mc.thePlayer == null)
            return;

        mc.thePlayer.stepHeight = 0.5F;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        switch(modeValue.get().toLowerCase()) {
            case "jump":
                if(mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.jump();
                break;
            case "laac":
                if(mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWeb) {
                    if(mc.thePlayer.onGround && msTimer.hasTimePassed(delayValue.get())) {
                        isStep = true;
                        mc.thePlayer.motionY += 0.620000001490116;

                        float f = mc.thePlayer.rotationYaw * 0.017453292F;
                        mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2F;
                        mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2F;
                        msTimer.reset();
                    }

                    mc.thePlayer.onGround = true;
                }else
                    isStep = false;
                break;
            case "aac3.3.4":
                if(mc.thePlayer.isCollidedHorizontally && MovementUtils.isMoving()) {
                    if(mc.thePlayer.onGround) {
                        final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
                        final double x = -Math.sin(yaw) * 1;
                        final double z = Math.cos(yaw) * 1;
                        final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + 1, mc.thePlayer.posZ + z);
                        final Block block = BlockUtils.getBlock(blockPos);
                        final AxisAlignedBB axisAlignedBB = block.getCollisionBoundingBox(mc.theWorld, blockPos, BlockUtils.getState(blockPos));

                        if(!(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z)) instanceof BlockAir) && (axisAlignedBB == null || block instanceof BlockSnow)) {
                            mc.thePlayer.motionX *= 1.26;
                            mc.thePlayer.motionZ *= 1.26;
                            mc.thePlayer.jump();

                            isAACStep = true;
                        }
                    }

                    if(isAACStep && mc.thePlayer.moveStrafing == 0F) {
                        mc.thePlayer.motionY -= 0.015;
                        mc.thePlayer.jumpMovementFactor = 0.3F;
                    }
                }else
                    isAACStep = false;
                break;
        }
    }

    @EventTarget
    public void onStep(StepEvent event) {
        if(mc.thePlayer == null)
            return;

        if (LiquidBounce.moduleManager.getModule(Phase.class).getState()) {
            event.setStepHeight(0F);
            return;
        }

        final Fly fly = (Fly) LiquidBounce.moduleManager.getModule(Fly.class);

        if(fly.getState()) {
            final String flyMode = fly.modeValue.get();

            if(flyMode.equalsIgnoreCase("Hypixel") || flyMode.equalsIgnoreCase("OtherHypixel") || flyMode.equalsIgnoreCase("LatestHypixel") || flyMode.equalsIgnoreCase("Rewinside") || (flyMode.equalsIgnoreCase("Mineplex") && mc.thePlayer.inventory.getCurrentItem() == null)) {
                event.setStepHeight(0F);
                return;
            }
        }

        final String mode = modeValue.get();

        if(!mc.thePlayer.onGround || !msTimer.hasTimePassed(delayValue.get()) || mode.equalsIgnoreCase("Jump") || mode.equalsIgnoreCase("LAAC") || mode.equalsIgnoreCase("AAC3.3.4")) {
            mc.thePlayer.stepHeight = 0.5F;
            event.setStepHeight(0.5F);
            return;
        }

        final float height = heightValue.get();
        mc.thePlayer.stepHeight = height;
        event.setStepHeight(height);

        if(event.getStepHeight() > 0.5F) {
            isStep = true;
            stepX = mc.thePlayer.posX;
            stepY = mc.thePlayer.posY;
            stepZ = mc.thePlayer.posZ;
        }
    }

    @EventTarget(ignoreCondition = true)
    public void onStepConfirm(StepConfirmEvent event) {
        if(isStep) {
            final String mode = modeValue.get();

            if(mc.thePlayer.getEntityBoundingBox().minY - stepY > 0.5D) {
                if(mode.equalsIgnoreCase("NCP") || mode.equalsIgnoreCase("AAC")) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.41999998688698D, stepZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.7531999805212D, stepZ, false));
                    msTimer.reset();
                }else if(mode.equalsIgnoreCase("Spartan")) {
                    if(spartanSwitch) {
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.41999998688698D, stepZ, false));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.7531999805212D, stepZ, false));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 1.001335979112147D, stepZ, false));
                    }else
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.6D, stepZ, false));

                    spartanSwitch = !spartanSwitch;
                    msTimer.reset();
                }else if(mode.equalsIgnoreCase("Rewinside")) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.41999998688698D, stepZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 0.7531999805212D, stepZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(stepX, stepY + 1.001335979112147D, stepZ, false));
                    msTimer.reset();
                }
            }

            isStep = false;
            stepX = 0D;
            stepY = 0D;
            stepZ = 0D;
        }
    }

    @EventTarget(ignoreCondition = true)
    public void onPacket(PacketEvent event) {
        Packet packet = event.getPacket();

        if(packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            if(isStep && modeValue.get().equalsIgnoreCase("OldNCP")) {
                packetPlayer.y += 0.07D;
                isStep = false;
            }
        }
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
