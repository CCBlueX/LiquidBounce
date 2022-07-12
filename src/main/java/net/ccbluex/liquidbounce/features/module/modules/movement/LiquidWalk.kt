/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.collideBlock
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard
import java.util.*

@ModuleInfo(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT, defaultKeyBinds = [Keyboard.KEY_J])
class LiquidWalk : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC3.1.0", "AAC3.3.5", "AAC3.3.11", "Spartan146", "Dolphin"), "NCP") // AAC3.3.5 Mode = AAC WaterFly

    private val waterOnlyValue = BoolValue("OnlyWater", false)

    private val noJumpValue = BoolValue("NoJump", false)
    private val aacFlyValue = FloatValue("AAC3.3.5-Motion", 0.5f, 0.1f, 1f)

    private var nextTick = false

    private val waterBlocks by lazy(LazyThreadSafetyMode.NONE) { arrayOf(Blocks.water, Blocks.flowing_water) }

    private fun checkLiquid(block: Block, waterOnly: Boolean) = if (waterOnly) block in waterBlocks else block is BlockLiquid

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking) return

        val waterOnly = waterOnlyValue.get()

        val isInLiquid = thePlayer.isInWater || (!waterOnly && thePlayer.isInLava)

        val posX = thePlayer.posX
        val posY = thePlayer.posY
        val posZ = thePlayer.posZ

        when (modeValue.get().lowercase(Locale.getDefault()))
        {
            "ncp", "vanilla" -> if (theWorld.collideBlock(thePlayer.entityBoundingBox) { checkLiquid(it.block, waterOnly) } && thePlayer.isInsideOfMaterial(Material.air) && !thePlayer.isSneaking) thePlayer.motionY = 0.08

            "aac3.1.0" ->
            {
                val block = theWorld.getBlock(thePlayer.position.down())

                if (!thePlayer.onGround && checkLiquid(block, waterOnly) || isInLiquid)
                {
                    if (!thePlayer.isSprinting)
                    {
                        thePlayer.multiply(0.99999)
                        thePlayer.motionY = 0.0

                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((posY - (posY - 1).toInt()).toInt() * 0.125)
                    }
                    else
                    {
                        thePlayer.multiply(0.99999)
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

                val block = theWorld.getBlock(BlockPos(posX, posY + 1, posZ))
                val blockUp = theWorld.getBlock(BlockPos(posX, posY + 1.1, posZ))

                if (checkLiquid(blockUp, waterOnly)) thePlayer.motionY = 0.1 else if (checkLiquid(block, waterOnly)) thePlayer.motionY = 0.0

                thePlayer.onGround = true
                thePlayer.multiply(1.085)
            }

            "aac3.3.11" -> if (isInLiquid)
            {
                thePlayer.multiply(1.17)
                if (thePlayer.isCollidedHorizontally) thePlayer.motionY = 0.24 else if (theWorld.getBlockState(BlockPos(posX, posY + 1.0, posZ)).block != Blocks.air) thePlayer.motionY += 0.04
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

        val waterOnly = waterOnlyValue.get()

        if (checkLiquid(event.block, waterOnly) && !theWorld.collideBlock(thePlayer.entityBoundingBox) { checkLiquid(it.block, waterOnly) } && !thePlayer.isSneaking) when (modeValue.get().lowercase(Locale.getDefault()))
        {
            "ncp", "vanilla" ->
            {
                val x = event.x
                val y = event.y
                val z = event.z

                event.boundingBox = AxisAlignedBB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (!modeValue.get().equals("NCP", ignoreCase = true)) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val lava = waterOnlyValue.get()

        // Bypass NCP Jesus checks
        if (event.packet is C03PacketPlayer)
        {
            val packetPlayer = event.packet

            val bb = thePlayer.entityBoundingBox
            if (theWorld.collideBlock(AxisAlignedBB(bb.minX, bb.minY - 0.01, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)) { checkLiquid(it.block, lava) })
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

        val block = theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

        if (noJumpValue.get() && checkLiquid(block, waterOnlyValue.get())) event.cancelEvent()
    }

    override val tag: String
        get() = modeValue.get()
}
