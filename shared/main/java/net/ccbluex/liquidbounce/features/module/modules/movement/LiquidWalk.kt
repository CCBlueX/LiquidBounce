/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.IClassProvider
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT, defaultKeyBinds = [Keyboard.KEY_J])
class LiquidWalk : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC3.1.0", "AAC3.3.5", "AAC3.3.11", "Spartan146", "Dolphin"), "NCP") // AAC3.3.5 Mode = AAC WaterFly

	private val waterOnlyValue = BoolValue("OnlyWater", false)

	private val noJumpValue = BoolValue("NoJump", false)
	private val aacFlyValue = FloatValue("AAC3.3.5-Motion", 0.5f, 0.1f, 1f)

	private var nextTick = false

	private val waterBlocks by lazy(LazyThreadSafetyMode.NONE) { arrayOf(BlockType.WATER, BlockType.FLOWING_WATER).map(classProvider::getBlockEnum) }

	private fun checkLiquid(provider: IClassProvider, block: IBlock, waterOnly: Boolean) = if (waterOnly) block in waterBlocks else provider.isBlockLiquid(block)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.sneaking) return

		val waterOnly = waterOnlyValue.get()

		val isInLiquid = thePlayer.isInWater || (!waterOnly && thePlayer.isInLava)

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

		val provider = classProvider

		when (modeValue.get().toLowerCase())
		{
			"ncp", "vanilla" -> if (collideBlock(theWorld, thePlayer.entityBoundingBox) { checkLiquid(provider, it.block, waterOnly) } && thePlayer.isInsideOfMaterial(provider.getMaterialEnum(MaterialType.AIR)) && !thePlayer.sneaking) thePlayer.motionY = 0.08

			"aac3.1.0" ->
			{
				val block = getBlock(theWorld, thePlayer.position.down())

				if (!thePlayer.onGround && checkLiquid(provider, block, waterOnly) || isInLiquid)
				{
					if (!thePlayer.sprinting)
					{
						MovementUtils.multiply(thePlayer, 0.99999)
						thePlayer.motionY = 0.0

						if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((posY - (posY - 1).toInt()).toInt() * 0.125)
					}
					else
					{
						MovementUtils.multiply(thePlayer, 0.99999)
						thePlayer.motionY = 0.0

						if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((posY - (posY - 1).toInt()).toInt() * 0.125)
					}
					if (thePlayer.fallDistance >= 4) thePlayer.motionY = -0.004 else if (isInLiquid) thePlayer.motionY = 0.09
				}

				if (thePlayer.hurtTime != 0) thePlayer.onGround = false
			}

			"spartan146" -> if (isInLiquid)
			{
				if (thePlayer.isCollidedHorizontally)
				{
					thePlayer.motionY += 0.15
					return
				}

				val block = getBlock(theWorld, WBlockPos(posX, posY + 1, posZ))
				val blockUp = getBlock(theWorld, WBlockPos(posX, posY + 1.1, posZ))

				if (checkLiquid(provider, blockUp, waterOnly)) thePlayer.motionY = 0.1 else if (checkLiquid(provider, block, waterOnly)) thePlayer.motionY = 0.0

				thePlayer.onGround = true
				MovementUtils.multiply(thePlayer, 1.085)
			}

			"aac3.3.11" -> if (isInLiquid)
			{
				MovementUtils.multiply(thePlayer, 1.17)
				if (thePlayer.isCollidedHorizontally) thePlayer.motionY = 0.24 else if (theWorld.getBlockState(WBlockPos(posX, posY + 1.0, posZ)).block != provider.getBlockEnum(BlockType.AIR)) thePlayer.motionY += 0.04
			}

			"dolphin" -> if (isInLiquid) thePlayer.motionY += 0.03999999910593033 // Same as normal swimming
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		if (modeValue.get().equals("AAC3.3.5", ignoreCase = true) && (thePlayer.isInWater || !waterOnlyValue.get() && thePlayer.isInLava))
		{
			val aacFlyMotion = aacFlyValue.get().toDouble()

			event.y = aacFlyMotion
			thePlayer.motionY = aacFlyMotion
		}
	}

	@EventTarget
	fun onBlockBB(event: BlockBBEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		val waterOnly = waterOnlyValue.get()

		if (checkLiquid(provider, event.block, waterOnly) && !collideBlock(theWorld, thePlayer.entityBoundingBox) { checkLiquid(provider, it.block, waterOnly) } && !thePlayer.sneaking) when (modeValue.get().toLowerCase())
		{
			"ncp", "vanilla" ->
			{
				val x = event.x
				val y = event.y
				val z = event.z

				event.boundingBox = provider.createAxisAlignedBB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
			}
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (!modeValue.get().equals("NCP", ignoreCase = true)) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		val lava = waterOnlyValue.get()

		// Bypass NCP Jesus checks
		if (provider.isCPacketPlayer(event.packet))
		{
			val packetPlayer = event.packet.asCPacketPlayer()

			val bb = thePlayer.entityBoundingBox
			if (collideBlock(theWorld, provider.createAxisAlignedBB(bb.minX, bb.minY - 0.01, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)) { checkLiquid(provider, it.block, lava) })
			{
				nextTick = !nextTick
				if (nextTick) packetPlayer.y -= 0.001
			}
		}
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val block = getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

		if (noJumpValue.get() && checkLiquid(classProvider, block, waterOnlyValue.get())) event.cancelEvent()
	}

	override val tag: String
		get() = modeValue.get()
}
