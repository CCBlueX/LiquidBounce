/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class YPort extends SpeedMode {

    private double moveSpeed = 0.2873D;
    private int level = 1;
    private double lastDist;
    private int timerDelay;
    private boolean safeJump;

    public YPort() {
        super("YPort");
    }

    @Override
    public void onMotion() {
        if(!safeJump && !mc.gameSettings.keyBindJump.isKeyDown() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInsideOfMaterial(Material.water) && !mc.thePlayer.isInsideOfMaterial(Material.lava) && !mc.thePlayer.isInWater() && ((!(this.getBlock(-1.1) instanceof BlockAir) && !(this.getBlock(-1.1) instanceof BlockAir)) || (!(this.getBlock(-0.1) instanceof BlockAir) && mc.thePlayer.motionX != 0.0 && mc.thePlayer.motionZ != 0.0 && !mc.thePlayer.onGround && mc.thePlayer.fallDistance < 3.0f && mc.thePlayer.fallDistance > 0.05)) && this.level == 3)
            mc.thePlayer.motionY = -0.3994;

        double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        this.lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

        if(!MovementUtils.isMoving())
            safeJump = true;
        else if(mc.thePlayer.onGround)
            safeJump = false;
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onMove(MoveEvent event) {
        this.timerDelay += 1;
        this.timerDelay %= 5;

        if(this.timerDelay != 0) {
            mc.timer.timerSpeed = 1F;
        }else{
            if(MovementUtils.hasMotion())
                mc.timer.timerSpeed = 32767F;

            if(MovementUtils.hasMotion()) {
                mc.timer.timerSpeed = 1.3F;
                mc.thePlayer.motionX *= 1.0199999809265137D;
                mc.thePlayer.motionZ *= 1.0199999809265137D;
            }
        }

        if(mc.thePlayer.onGround && MovementUtils.hasMotion())
            this.level = 2;

        if(round(mc.thePlayer.posY - (int) mc.thePlayer.posY) == round(0.138D)) {
            mc.thePlayer.motionY -= 0.08D;
            event.setY(event.getY() - 0.09316090325960147D);
            mc.thePlayer.posY -= 0.09316090325960147D;
        }

        if(this.level == 1 && (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F)) {
            this.level = 2;
            this.moveSpeed = (1.38D * getBaseMoveSpeed() - 0.01D);
        }else if(this.level == 2) {
            this.level = 3;
            mc.thePlayer.motionY = 0.399399995803833D;
            event.setY(0.399399995803833D);
            this.moveSpeed *= 2.149D;
        }else if(this.level == 3) {
            this.level = 4;
            double difference = 0.66D * (this.lastDist - getBaseMoveSpeed());
            this.moveSpeed = (this.lastDist - difference);
        }else{
            if((mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0D, mc.thePlayer.motionY, 0.0D)).size() > 0) || (mc.thePlayer.isCollidedVertically))
                this.level = 1;
            this.moveSpeed = (this.lastDist - this.lastDist / 159.0D);
        }

        this.moveSpeed = Math.max(this.moveSpeed, getBaseMoveSpeed());
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;
        if(forward == 0F && strafe == 0F) {
            event.setX(0D);
            event.setZ(0D);
        }else if(forward != 0F) {
            if(strafe >= 1F) {
                yaw += (forward > 0F ? -45 : 45);
                strafe = 0F;
            }else if(strafe <= -1.0F) {
                yaw += (forward > 0F ? 45 : -45);
                strafe = 0F;
            }

            if(forward > 0F)
                forward = 1F;
            else if(forward < 0F)
                forward = -1F;
        }

        final double mx = Math.cos(Math.toRadians(yaw + 90.0F));
        final double mz = Math.sin(Math.toRadians(yaw + 90.0F));

        event.setX((forward * this.moveSpeed * mx + strafe * this.moveSpeed * mz));
        event.setZ((forward * this.moveSpeed * mz - strafe * this.moveSpeed * mx));

        mc.thePlayer.stepHeight = 0.6F;

        if(forward == 0F && strafe == 0F) {
            event.setX(0D);
            event.setZ(0D);
        }
    }

    private double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if(mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    private Block getBlock(AxisAlignedBB axisAlignedBB) {
        for(int x = MathHelper.floor_double(axisAlignedBB.minX); x < MathHelper.floor_double(axisAlignedBB.maxX) + 1; ++x) {
            for(int z = MathHelper.floor_double(axisAlignedBB.minZ); z < MathHelper.floor_double(axisAlignedBB.maxZ) + 1; ++z) {
                final Block block = mc.theWorld.getBlockState(new BlockPos(x, (int) axisAlignedBB.minY, z)).getBlock();

                if(block != null)
                    return block;
            }
        }

        return null;
    }

    private Block getBlock(double offset) {
        return this.getBlock(mc.thePlayer.getEntityBoundingBox().offset(0.0, offset, 0.0));
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
