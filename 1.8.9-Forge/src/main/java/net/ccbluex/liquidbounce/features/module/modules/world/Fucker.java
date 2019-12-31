package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.BlockValue;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.Block;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "Fucker", description = "Destroys selected blocks around you. (aka.  IDNuker)", category = ModuleCategory.WORLD)
public class Fucker extends Module {

	private final BlockValue blockValue = new BlockValue("ID", 26);
	private final BoolValue instantValue = new BoolValue("Instant", false);
	private final ListValue throughWallsValue = new ListValue("ThroughWalls", new String[] {"None", "Raycast", "Around"}, "None");
	private final BoolValue swingValue = new BoolValue("Swing", true);
	private final BoolValue rotationsValue = new BoolValue("Rotations", true);
	private final BoolValue surroundingsValue = new BoolValue("Surroundings", true);
	private final ListValue actionValue = new ListValue("Action", new String[] {"Destroy", "Use"}, "Destroy");
	private final BoolValue noHitValue = new BoolValue("NoHit", false);

	private BlockPos pos;
	private BlockPos oldPos;
	static float currentDamage;
	private int blockHitDelay;

	private final MSTimer switchTimer = new MSTimer();

	@EventTarget
	public void onUpdate(final UpdateEvent event) {
		if(noHitValue.get()) {
			final KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

            if (killAura.getState() && killAura.getTarget() != null)
				return;
		}

		final int targetId = blockValue.get();

		if(pos == null || Block.getIdFromBlock(BlockUtils.getBlock(pos)) != targetId || mc.thePlayer.getDistanceSq(pos) >= 22.399999618530273D)
			pos = find(targetId);

		boolean surroundings = false;

		if(pos != null && surroundingsValue.get()) {
			final double diffX = pos.getX() + 0.5 - mc.thePlayer.posX;
			final double diffY = pos.getY() + 0.5 - (mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight());
			final double diffZ = pos.getZ() + 0.5 - mc.thePlayer.posZ;
			final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
			final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
			final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

			float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
			float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
			float f2 = -MathHelper.cos(-pitch * 0.017453292F);
			float f3 = MathHelper.sin(-pitch * 0.017453292F);
            Vec3 vec31 = new Vec3(f1 * f2, f3, f * f2);

			Vec3 vec3 = mc.thePlayer.getPositionEyes(1F);
			Vec3 vec32 = vec3.addVector(vec31.xCoord * mc.playerController.getBlockReachDistance(), vec31.yCoord * mc.playerController.getBlockReachDistance(), vec31.zCoord * mc.playerController.getBlockReachDistance());

			final BlockPos blockPos = mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, true).getBlockPos();

			if(blockPos != null) {
				if(pos.getX() != blockPos.getX() || pos.getY() != blockPos.getY() || pos.getZ() != blockPos.getZ())
					surroundings = true;

				pos = blockPos;
			}
		}

		if(pos == null) {
			currentDamage = 0F;
			return;
		}

		if(oldPos != null && !oldPos.equals(pos))
			switchTimer.reset();

		oldPos = pos;

		if(blockHitDelay > 0) {
			blockHitDelay--;
			return;
		}

		if(!switchTimer.hasTimePassed(250L))
			return;

		if(rotationsValue.get()) RotationUtils.faceBlock(pos);

		switch(surroundings ? "destroy" : actionValue.get().toLowerCase()) {
			case "destroy":
				final AutoTool autoTool = (AutoTool) ModuleManager.getModule(AutoTool.class);
				if(autoTool != null && autoTool.getState())
					autoTool.switchSlot(pos);

				if(!instantValue.get()) {
					if(currentDamage == 0F) {
						mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.DOWN));
						if(mc.thePlayer.capabilities.isCreativeMode || BlockUtils.getBlock(pos).getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) >= 1.0F) {
							currentDamage = 0F;
							if(swingValue.get())
								mc.thePlayer.swingItem();
							mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN);
							pos = null;
							return;
						}
					}

					if(swingValue.get())
						mc.thePlayer.swingItem();

					currentDamage += BlockUtils.getBlock(pos).getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos);
					mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, (int) (currentDamage * 10F) - 1);
					if(currentDamage >= 1.0F) {
						mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.DOWN));
						mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN);
						blockHitDelay = 4;
						currentDamage = 0F;
						pos = null;
					}
				}else{
					mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.DOWN));
                    if (swingValue.get())
                        mc.thePlayer.swingItem();
					mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.DOWN));
					currentDamage = 0F;
					pos = null;
				}
				break;
			case "use":
				if(mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), pos, EnumFacing.DOWN, new Vec3(pos.getX(), pos.getY(), pos.getZ()))) {
					if(swingValue.get())
						mc.thePlayer.swingItem();

					blockHitDelay = 4;
					currentDamage = 0F;
					pos = null;
				}
				break;
		}
	}

	@EventTarget
	public void onRender3D(Render3DEvent event) {
		if(pos != null)
			RenderUtils.drawBlockBox(pos, Color.RED, true);
	}

	private BlockPos find(final int targetID) {
		return BlockUtils.searchBlocks(4).entrySet().stream()
				.filter(entry -> Block.getIdFromBlock(entry.getValue()) == targetID && mc.thePlayer.getDistanceSq(entry.getKey()) < 22.3D && (isHitable(entry.getKey()) || surroundingsValue.get()))
				.min(Comparator.comparingDouble(value -> BlockUtils.getCenterDistance(value.getKey())))
				.map(Map.Entry :: getKey)
				.orElse(null);
	}

	@Override
	public String getTag() {
		return BlockUtils.getBlockName(blockValue.get());
	}

	private boolean isHitable(final BlockPos blockPos) {
		switch(throughWallsValue.get().toLowerCase()) {
			case "raycast":
				final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
				final MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), false, true, false);

				return movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(blockPos);
			case "around":
				return !BlockUtils.isFullBlock(blockPos.down()) || !BlockUtils.isFullBlock(blockPos.up()) || !BlockUtils.isFullBlock(blockPos.north()) || !BlockUtils.isFullBlock(blockPos.east()) || !BlockUtils.isFullBlock(blockPos.south()) || !BlockUtils.isFullBlock(blockPos.west());
			default:
				return true;
		}
	}
}
