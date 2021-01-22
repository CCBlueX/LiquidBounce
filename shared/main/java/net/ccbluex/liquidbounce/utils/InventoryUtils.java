/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import java.util.*;
import java.util.stream.IntStream;

import net.ccbluex.liquidbounce.api.enums.BlockType;
import net.ccbluex.liquidbounce.api.enums.EnumFacingType;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.api.minecraft.item.IItem;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemBlock;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging.WAction;
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB;
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos;
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld;
import net.ccbluex.liquidbounce.event.ClickWindowEvent;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;

public final class InventoryUtils extends MinecraftInstance implements Listenable
{

	// @formatter:off
	public static final List<IBlock> AUTOBLOCK_BLACKLIST = Arrays.asList(
			// Interactible blocks
			classProvider.getBlockEnum(BlockType.CHEST), classProvider.getBlockEnum(BlockType.ENDER_CHEST), classProvider.getBlockEnum(BlockType.TRAPPED_CHEST), classProvider.getBlockEnum(BlockType.ANVIL), classProvider.getBlockEnum(BlockType.DISPENSER), classProvider.getBlockEnum(BlockType.DROPPER), classProvider.getBlockEnum(BlockType.FURNACE), classProvider.getBlockEnum(BlockType.LIT_FURNACE), classProvider.getBlockEnum(BlockType.CRAFTING_TABLE), classProvider.getBlockEnum(BlockType.ENCHANTING_TABLE), classProvider.getBlockEnum(BlockType.JUKEBOX), classProvider.getBlockEnum(BlockType.BED), classProvider.getBlockEnum(BlockType.NOTEBLOCK),

			classProvider.getBlockEnum(BlockType.WEB),

			// Some excepted blocks
			classProvider.getBlockEnum(BlockType.TORCH), classProvider.getBlockEnum(BlockType.REDSTONE_TORCH), classProvider.getBlockEnum(BlockType.REDSTONE_WIRE), classProvider.getBlockEnum(BlockType.LADDER), classProvider.getBlockEnum(BlockType.VINE), classProvider.getBlockEnum(BlockType.WATERLILY), classProvider.getBlockEnum(BlockType.CACTUS), classProvider.getBlockEnum(BlockType.GLASS_PANE), classProvider.getBlockEnum(BlockType.IRON_BARS),

			// Pressure plates
			classProvider.getBlockEnum(BlockType.STONE_PRESSURE_PLATE), classProvider.getBlockEnum(BlockType.WODDEN_PRESSURE_PLATE), classProvider.getBlockEnum(BlockType.LIGHT_WEIGHTED_PRESSURE_PLATE), classProvider.getBlockEnum(BlockType.HEAVY_WEIGHTED_PRESSURE_PLATE),

			// Falling blocks
			classProvider.getBlockEnum(BlockType.SAND), classProvider.getBlockEnum(BlockType.GRAVEL),

			classProvider.getBlockEnum(BlockType.TNT), classProvider.getBlockEnum(BlockType.STANDING_BANNER), classProvider.getBlockEnum(BlockType.WALL_BANNER));
	// @formatter:on

	public static final MSTimer CLICK_TIMER = new MSTimer();

	public static int findItem(final int startSlot, final int endSlot, final IItem item)
	{
		for (int i = startSlot; i < endSlot; i++)
		{
			final IItemStack stack = Objects.requireNonNull(mc.getThePlayer()).getInventoryContainer().getSlot(i).getStack();

			if (stack != null && Objects.equals(stack.getItem(), item))
				return i;
		}

		return -1;
	}

	public static boolean hasSpaceHotbar()
	{
		return IntStream.range(36, 45).mapToObj(i -> Objects.requireNonNull(mc.getThePlayer()).getInventory().getStackInSlot(i)).anyMatch(Objects::isNull);
	}

	public static int findAutoBlockBlock(final boolean autoblockFullcubeOnly, final double boundingBoxYLimit)
	{
		final IWorld theWorld = Objects.requireNonNull(mc.getTheWorld());
		final IEntityPlayerSP thePlayer = Objects.requireNonNull(mc.getThePlayer());

		final List<Integer> hotbarSlots = new ArrayList<>(9);
		for (int i = 36; i < 45; i++)
		{
			final IItemStack itemStack = thePlayer.getInventoryContainer().getSlot(i).getStack();

			if (itemStack != null && classProvider.isItemBlock(itemStack.getItem()) && itemStack.getStackSize() > 0)
			{
				final IItemBlock itemBlock = Objects.requireNonNull(itemStack.getItem()).asItemBlock();
				final IBlock block = itemBlock.getBlock();

				if (canAutoBlock(block) && block.isFullCube(Objects.requireNonNull(block.getDefaultState())))
					hotbarSlots.add(i);
			}
		}
		final Optional<Integer> pred = boundingBoxYLimit == -1 ? Optional.ofNullable(hotbarSlots.isEmpty() ? null : hotbarSlots.get(0)) : hotbarSlots.stream().filter(c ->
		{
			final IBlock b = mc.getThePlayer().getInventoryContainer().getSlot(c).getStack().getItem().asItemBlock().getBlock();
			final IAxisAlignedBB box = b.getCollisionBoundingBox(theWorld, WBlockPos.Companion.getORIGIN(), Objects.requireNonNull(b.getDefaultState()));
			return box != null && box.getMaxY() - box.getMinY() <= boundingBoxYLimit;
		}).max(Comparator.comparingDouble(c ->
		{
			final IBlock block = mc.getThePlayer().getInventoryContainer().getSlot(c).getStack().getItem().asItemBlock().getBlock();
			return block.getBlockBoundsMaxY() - block.getBlockBoundsMinY();
		}));
		if (pred.isPresent())
			return pred.get();

		hotbarSlots.clear(); // Reuse list

		if (!autoblockFullcubeOnly)
		{
			for (int i = 36; i < 45; i++)
			{
				final IItemStack itemStack = mc.getThePlayer().getInventoryContainer().getSlot(i).getStack();

				if (itemStack != null && classProvider.isItemBlock(itemStack.getItem()) && itemStack.getStackSize() > 0)
				{
					final IItemBlock itemBlock = Objects.requireNonNull(itemStack.getItem()).asItemBlock();
					final IBlock block = itemBlock.getBlock();

					if (canAutoBlock(block))
						hotbarSlots.add(i);
				}
			}
			final Optional<Integer> pred2 = boundingBoxYLimit == -1 ? Optional.ofNullable(hotbarSlots.isEmpty() ? null : hotbarSlots.get(0)) : hotbarSlots.stream().filter(c ->
			{
				final IBlock block = thePlayer.getInventoryContainer().getSlot(c).getStack().getItem().asItemBlock().getBlock();
				final IAxisAlignedBB box = block.getCollisionBoundingBox(theWorld, WBlockPos.Companion.getORIGIN(), Objects.requireNonNull(block.getDefaultState()));
				return box != null && box.getMaxY() - box.getMinY() <= boundingBoxYLimit;
			}).max(Comparator.comparingDouble(c ->
			{
				final IBlock block = thePlayer.getInventoryContainer().getSlot(c).getStack().getItem().asItemBlock().getBlock();
				return block.getBlockBoundsMaxY() - block.getBlockBoundsMinY();
			}));

			if (pred2.isPresent())
				return pred2.get();
		}

		return -1;
	}

	public static boolean canAutoBlock(final IBlock block)
	{
		return !AUTOBLOCK_BLACKLIST.contains(block) && !classProvider.isBlockBush(block) && !classProvider.isBlockRailBase(block) && !classProvider.isBlockSign(block) && !classProvider.isBlockDoor(block);
	}

	@EventTarget
	public void onClick(final ClickWindowEvent event)
	{
		CLICK_TIMER.reset();
	}

	@EventTarget
	public void onPacket(final PacketEvent event)
	{
		final IPacket packet = event.getPacket();

		if (classProvider.isCPacketPlayerDigging(packet))
		{
			final ICPacketPlayerDigging digging = packet.asCPacketPlayerDigging();
			if ((digging.getStatus() == WAction.DROP_ITEM || digging.getStatus() == WAction.DROP_ALL_ITEMS) && WBlockPos.Companion.getORIGIN().equals(digging.getPosition()) && classProvider.getEnumFacing(EnumFacingType.DOWN).equals(digging.getFacing()))
				CLICK_TIMER.reset(); // Drop (all) item(s) in hotbar with Q (Ctrl+Q)
		}

		if (classProvider.isCPacketPlayerBlockPlacement(packet))
			CLICK_TIMER.reset();
	}

	@Override
	public boolean handleEvents()
	{
		return true;
	}
}
