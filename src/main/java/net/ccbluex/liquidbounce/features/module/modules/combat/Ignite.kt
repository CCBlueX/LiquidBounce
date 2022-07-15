/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.BlockAir
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import java.util.*
import kotlin.math.atan2
import kotlin.math.hypot

@ModuleInfo(name = "Ignite", description = "Automatically sets targets around you on fire.", category = ModuleCategory.COMBAT)
class Ignite : Module()
{
    /**
     * Options
     */
    private val rangeValue = FloatValue("Range", 1f, 3f, 6f)
    private val delayValue = IntegerRangeValue("Delay", 0, 3000, 0, 0)
    private val lighterValue = BoolValue("Lighter", true)
    private val lavaBucketValue = BoolValue("Lava", true)
    private val randomSlotValue = BoolValue("RandomSlot", false)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)
    private val silentRotationValue = BoolValue("SilentRotation", true, "SilentRotation")
    private val keepRotationGroup = ValueGroup("KeepRotation")
    private val keepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
    private val keepRotationTicksValue = IntegerValue("Ticks", 1, 1, 40, "KeepRotationLength")

    init
    {
        keepRotationGroup.addAll(keepRotationEnabledValue, keepRotationTicksValue)
    }

    /**
     * Variables
     */
    private val msTimer = MSTimer()
    private var delay = delayValue.getRandomLong()

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        if (!msTimer.hasTimePassed(delay)) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val itemDelay = itemDelayValue.get()
        val randomSlot = randomSlotValue.get()
        val range = rangeValue.get()

        val inventoryContainer = thePlayer.inventoryContainer
        val lighterInHotbar = if (lighterValue.get()) inventoryContainer.findItem(36, 45, Items.flint_and_steel, itemDelay.toLong(), randomSlot) else -1
        val lavaInHotbar = if (lavaBucketValue.get()) inventoryContainer.findItem(26, 45, Items.lava_bucket, itemDelay.toLong(), randomSlot) else -1

        if (lighterInHotbar == -1 && lavaInHotbar == -1) return

        val fireInHotbar = if (lighterInHotbar == -1) lavaInHotbar else lighterInHotbar

        theWorld.getEntitiesInRadius(thePlayer, 8.0).asSequence().filterIsInstance<EntityLivingBase>().filterNot(Entity::isBurning).filter { it.isSelected(true) }.filter { thePlayer.getDistanceToEntityBox(it) < range }.map(Entity::getPosition).filter(theWorld::isReplaceable).firstOrNull { theWorld.getBlock(it) is BlockAir }?.let { blockPos ->
            InventoryUtils.tryHoldSlot(thePlayer, fireInHotbar - 36, lock = true)

            val itemStack = thePlayer.inventory.getStackInSlot(fireInHotbar - 36)

            if (itemStack != null && itemStack.item is ItemBucket) useLavaBucket(theWorld, thePlayer, blockPos, itemStack) else useLighter(theWorld, thePlayer, blockPos, itemStack)

            InventoryUtils.resetSlot(thePlayer)
            delay = delayValue.getRandomLong()
            msTimer.reset()
        }
    }

    private fun useLighter(theWorld: WorldClient, thePlayer: EntityPlayerSP, blockPos: BlockPos, itemStack: ItemStack?)
    {
        for (side in EnumFacing.values())
        {
            val neighbor = blockPos.offset(side)
            if (!theWorld.canBeClicked(neighbor)) continue
            aim(thePlayer, neighbor)

            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
            if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, itemStack, neighbor, side.opposite, Vec3(side.directionVec)))
            {
                thePlayer.swingItem()
                break
            }
        }
    }

    private fun useLavaBucket(theWorld: WorldClient, thePlayer: EntityPlayerSP, blockPos: BlockPos, itemStack: ItemStack?)
    {
        aim(thePlayer, blockPos)
        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        mc.playerController.sendUseItem(thePlayer, theWorld, itemStack)
    }

    private fun aim(thePlayer: EntityPlayerSP, target: Vec3i)
    {
        val diffX = target.x + 0.5 - thePlayer.posX
        val diffY = target.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
        val diffZ = target.z + 0.5 - thePlayer.posZ
        val sqrt = hypot(diffX, diffZ)

        val rotation = Rotation((atan2(diffZ, diffX).toFloat()).toDegrees - 90.0f, -(atan2(diffY, sqrt).toFloat()).toDegrees)
        if (silentRotationValue.get()) RotationUtils.setTargetRotation(rotation, if (keepRotationEnabledValue.get()) keepRotationTicksValue.get() else 0) else rotation.applyRotationToPlayer(thePlayer)
    }

    override val tag: String
        get()
        {
            val tagBuilder = StringJoiner(" and ")
            if (lighterValue.get()) tagBuilder.add("Lighter")
            if (lavaBucketValue.get()) tagBuilder.add("Lava")

            return "$tagBuilder"
        }
}
