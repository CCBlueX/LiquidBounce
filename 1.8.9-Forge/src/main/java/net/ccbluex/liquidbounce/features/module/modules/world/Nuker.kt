/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.VecRotation;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.List;
import java.util.*;

@ModuleInfo(name = "Nuker", description = "Breaks all blocks around you.", category = ModuleCategory.WORLD)
public class Nuker extends Module {

    private final FloatValue radiusValue = new FloatValue("Radius", 5.2F, 1F, 6F);
    private final BoolValue throughWallsValue = new BoolValue("ThroughWalls", false);

    private final List<BlockPos> attackedBlocks = new ArrayList<>();
    private BlockPos currentBlock;

    static float currentDamage;
    private int blockHitDelay;

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(blockHitDelay > 0) {
            blockHitDelay--;
            return;
        }

        attackedBlocks.clear();

        if(mc.playerController.isNotCreative()) {
            final Optional<Map.Entry<BlockPos, Block>> optionalEntry = BlockUtils.searchBlocks(Math.round(radiusValue.get()) + 1).entrySet().stream()
                    .filter(entry -> {
                        final Block block = entry.getValue();

                        return BlockUtils.getCenterDistance(entry.getKey()) <= radiusValue.get() && !(block instanceof BlockAir) && block != Blocks.bedrock && !(block instanceof BlockLiquid);
                    })
                    .filter(entry -> {
                        if (throughWallsValue.get()) return true;

                        final BlockPos blockPos = entry.getKey();
                        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                        final MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), false, true, false);

                        return movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(blockPos);
                    })
                    .min(Comparator.comparingDouble(value -> {
                        final BlockPos blockPos = value.getKey();

                        final boolean onBlock = (int) mc.thePlayer.posX == blockPos.getX() + 1 && mc.thePlayer.posY > blockPos.getY() && (int) mc.thePlayer.posZ == blockPos.getZ() + 1;
                        final double dist = BlockUtils.getCenterDistance(value.getKey());

                        return onBlock ? Double.MAX_VALUE - dist : dist;
                    }));

            if(!optionalEntry.isPresent())
                return;

            final Map.Entry<BlockPos, Block> entry = optionalEntry.get();

            final BlockPos blockPos = entry.getKey();
            final Block block = entry.getValue();

            if(!blockPos.equals(currentBlock))
                currentDamage = 0F;

            currentBlock = blockPos;

            attackedBlocks.add(blockPos);

            final VecRotation vecRotation = RotationUtils.faceBlock(blockPos);
            if (vecRotation != null)
                RotationUtils.setTargetRotation(vecRotation.getRotation());

            final AutoTool autoTool = (AutoTool) LiquidBounce.moduleManager.getModule(AutoTool.class);

            if(autoTool != null && autoTool.getState())
                autoTool.switchSlot(blockPos);

            if(currentDamage == 0F) {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));

                if(mc.thePlayer.capabilities.isCreativeMode || block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, blockPos) >= 1.0F) {
                    currentDamage = 0F;
                    mc.thePlayer.swingItem();
                    mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.DOWN);
                    return;
                }
            }

            mc.thePlayer.swingItem();

            currentDamage += block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, blockPos);
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int) (currentDamage * 10F) - 1);

            if(currentDamage >= 1.0F) {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
                mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.DOWN);
                blockHitDelay = 4;
                currentDamage = 0F;
            }
        }else{
            final ItemStack heldItem = mc.thePlayer.getHeldItem();

            if (heldItem != null && heldItem.getItem() instanceof ItemSword)
                return;

            BlockUtils.searchBlocks(Math.round(radiusValue.get()) + 1).entrySet().stream()
                    .filter(entry -> BlockUtils.getCenterDistance(entry.getKey()) <= radiusValue.get() && !(entry.getValue() instanceof BlockAir))
                    .map(Map.Entry :: getKey)
                    .filter(blockPos -> {
                        if(throughWallsValue.get()) return true;

                        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                        final MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), false, true, false);

                        return movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(blockPos);
                    })
                    .forEach(blockPos -> {
                        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
                        mc.thePlayer.swingItem();
                        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));

                        attackedBlocks.add(blockPos);
                    });
        }
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        for(final BlockPos blockPos : attackedBlocks) RenderUtils.drawBlockBox(blockPos, Color.RED, true);
    }

    private boolean isBreakable(final Block block) {
        return false;
    }
}
