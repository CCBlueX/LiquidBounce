/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.BlockBBEvent;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "FastClimb", description = "Allows you to climb up ladders and vines faster.", category = ModuleCategory.MOVEMENT)
public class FastClimb extends Module {
    public final ListValue modeValue = new ListValue("Mode", new String[] {"Normal", "InstantTP", "AAC", "AACv3", "OAAC", "LAAC"}, "Normal");
    private final FloatValue normalSpeedValue = new FloatValue("NormalSpeed", 0.2872F, 0.01F, 5F);

    @EventTarget
    public void onMove(final MoveEvent event) {
        final String mode = modeValue.get();

        if(mode.equalsIgnoreCase("Normal") && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isOnLadder()) {
            event.setY(normalSpeedValue.get());
            mc.thePlayer.motionY = 0;
        }else if(mode.equalsIgnoreCase("AAC") && mc.thePlayer.isCollidedHorizontally) {
            final EnumFacing facing = mc.thePlayer.getHorizontalFacing();
            double x = 0;
            double z = 0;
            if(facing == EnumFacing.NORTH)
                z = -0.99;
            if(facing == EnumFacing.EAST)
                x = +0.99;
            if(facing == EnumFacing.SOUTH)
                z = +0.99;
            if(facing == EnumFacing.WEST)
                x = -0.99;

            final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);
            final Block block = BlockUtils.getBlock(blockPos);

            if(block instanceof BlockLadder || block instanceof BlockVine) {
                event.setY(0.5);
                mc.thePlayer.motionY = 0;
            }
        }else if(mode.equalsIgnoreCase("AACv3") && BlockUtils.collideBlockIntersects(mc.thePlayer.getEntityBoundingBox(), block -> block instanceof BlockLadder || block instanceof BlockVine) && mc.gameSettings.keyBindForward.isKeyDown()) {
            event.setY(0.5D);
            event.setX(0);
            event.setZ(0);
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }else if(mode.equalsIgnoreCase("OAAC") && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isOnLadder()) {
            event.setY(0.1649D);
            mc.thePlayer.motionY = 0;
        }else if(mode.equalsIgnoreCase("LAAC") && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isOnLadder()) {
            event.setY(0.1699D);
            mc.thePlayer.motionY = 0;
        }else if(mode.equalsIgnoreCase("InstantTP") && mc.thePlayer.isOnLadder() && mc.gameSettings.keyBindForward.isKeyDown()) {
            for(int i = (int) mc.thePlayer.posY; i < 256; i++) {
                final Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ));

                if(!(block instanceof BlockLadder)) {
                    final EnumFacing horizontalFacing = mc.thePlayer.getHorizontalFacing();
                    double x = 0;
                    double z = 0;
                    switch(horizontalFacing) {
                        case DOWN:
                        case UP:
                            break;
                        case NORTH:
                            z = -1;
                            break;
                        case EAST:
                            x = +1;
                            break;
                        case SOUTH:
                            z = +1;
                            break;
                        case WEST:
                            x = -1;
                            break;
                    }

                    mc.thePlayer.setPosition(mc.thePlayer.posX + x, i, mc.thePlayer.posZ + z);
                    break;
                }
            }
        }
    }

    @EventTarget
    public void onBlockBB(final BlockBBEvent event) {
        if(mc.thePlayer != null && (event.getBlock() instanceof BlockLadder || event.getBlock() instanceof BlockVine) && modeValue.get().equalsIgnoreCase("AACv3") && mc.thePlayer.isOnLadder())
            event.setBoundingBox(null);
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
