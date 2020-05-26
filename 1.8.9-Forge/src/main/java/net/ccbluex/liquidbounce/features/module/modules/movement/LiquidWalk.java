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
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_J)
public class LiquidWalk extends Module {

    public final ListValue modeValue = new ListValue("Mode", new String[] {"Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin"}, "NCP");
    private final BoolValue noJumpValue = new BoolValue("NoJump", false);

    private final FloatValue aacFlyValue = new FloatValue("AACFlyMotion", 0.5F, 0.1F, 1F);

    private boolean nextTick;

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(mc.thePlayer == null || mc.thePlayer.isSneaking())
            return;

        switch(modeValue.get().toLowerCase()) {
            case "ncp":
            case "vanilla":
                if(BlockUtils.collideBlock(mc.thePlayer.getEntityBoundingBox(), block -> block instanceof BlockLiquid) && mc.thePlayer.isInsideOfMaterial(Material.air) && !mc.thePlayer.isSneaking())
                    mc.thePlayer.motionY = 0.08D;
                break;
            case "aac":
                final BlockPos blockPos = mc.thePlayer.getPosition().down();

                if(!mc.thePlayer.onGround && BlockUtils.getBlock(blockPos) == Blocks.water || mc.thePlayer.isInWater()) {
                    if(!mc.thePlayer.isSprinting()) {
                        mc.thePlayer.motionX *= 0.99999;
                        mc.thePlayer.motionY *= 0.0;
                        mc.thePlayer.motionZ *= 0.99999;

                        if(mc.thePlayer.isCollidedHorizontally)
                            mc.thePlayer.motionY = (int) (mc.thePlayer.posY - (int) (mc.thePlayer.posY - 1)) / 8F;
                    }else{
                        mc.thePlayer.motionX *= 0.99999;
                        mc.thePlayer.motionY *= 0.0;
                        mc.thePlayer.motionZ *= 0.99999;

                        if(mc.thePlayer.isCollidedHorizontally)
                            mc.thePlayer.motionY = (int) (mc.thePlayer.posY - (int) (mc.thePlayer.posY - 1)) / 8F;
                    }

                    if(mc.thePlayer.fallDistance >= 4)
                        mc.thePlayer.motionY = -0.004;
                    else if(mc.thePlayer.isInWater())
                        mc.thePlayer.motionY = 0.09;
                }

                if(mc.thePlayer.hurtTime != 0)
                    mc.thePlayer.onGround = false;
                break;
            case "spartan":
                if(mc.thePlayer.isInWater()) {
                    if(mc.thePlayer.isCollidedHorizontally) {
                        mc.thePlayer.motionY += 0.15;
                        return;
                    }

                    final Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1, mc.thePlayer.posZ));
                    final Block blockUp = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1.1D, mc.thePlayer.posZ));

                    if(blockUp instanceof BlockLiquid) {
                        mc.thePlayer.motionY = 0.1D;
                    }else if(block instanceof BlockLiquid) {
                        mc.thePlayer.motionY = 0;
                    }

                    mc.thePlayer.onGround = true;
                    mc.thePlayer.motionX *= 1.085;
                    mc.thePlayer.motionZ *= 1.085;
                }
                break;
            case "aac3.3.11":
                if(mc.thePlayer.isInWater()) {
                    mc.thePlayer.motionX *= 1.17D;
                    mc.thePlayer.motionZ *= 1.17D;

                    if(mc.thePlayer.isCollidedHorizontally)
                        mc.thePlayer.motionY = 0.24;
                    else if(mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1.0D, mc.thePlayer.posZ)).getBlock() != Blocks.air)
                        mc.thePlayer.motionY += 0.04D;
                }
                break;

            case "dolphin":
                if(mc.thePlayer.isInWater())
                    mc.thePlayer.motionY += 0.03999999910593033;
                break;

        }
    }

    @EventTarget
    public void onMove(final MoveEvent event) {
        if ("aacfly".equals(modeValue.get().toLowerCase()) && mc.thePlayer.isInWater()) {
            event.setY(aacFlyValue.get());
            mc.thePlayer.motionY = aacFlyValue.get();
        }
    }

    @EventTarget
    public void onBlockBB(final BlockBBEvent event) {
        if(mc.thePlayer == null || mc.thePlayer.getEntityBoundingBox() == null)
            return;

        if(event.getBlock() instanceof BlockLiquid && !BlockUtils.collideBlock(mc.thePlayer.getEntityBoundingBox(), block -> block instanceof BlockLiquid) && !mc.thePlayer.isSneaking()) {
            switch(modeValue.get().toLowerCase()) {
                case "ncp":
                case "vanilla":
                    event.setBoundingBox(AxisAlignedBB.fromBounds(event.getX(), event.getY(), event.getZ(), event.getX() + 1, event.getY() + 1, event.getZ() + 1));
                    break;
            }
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if(mc.thePlayer == null || !modeValue.get().equalsIgnoreCase("NCP"))
            return;

        if(event.getPacket() instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) event.getPacket();

            if(BlockUtils.collideBlock(new AxisAlignedBB(mc.thePlayer.getEntityBoundingBox().maxX, mc.thePlayer.getEntityBoundingBox().maxY, mc.thePlayer.getEntityBoundingBox().maxZ, mc.thePlayer.getEntityBoundingBox().minX, mc.thePlayer.getEntityBoundingBox().minY - 0.01D, mc.thePlayer.getEntityBoundingBox().minZ), block -> block instanceof BlockLiquid)) {
                nextTick = !nextTick;

                if(nextTick) packetPlayer.y -= 0.001D;
            }
        }
    }

    @EventTarget
    public void onJump(final JumpEvent event) {
        if (mc.thePlayer == null)
            return;

        final Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.01, mc.thePlayer.posZ));

        if (noJumpValue.get() && block instanceof BlockLiquid)
            event.cancelEvent();
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
