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
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemBucket
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.atan2
import kotlin.math.hypot

@ModuleInfo(name = "Ignite", description = "Automatically sets targets around you on fire.", category = ModuleCategory.COMBAT)
class Ignite : Module()
{
    /**
     * Options
     */
    private val lighterValue = BoolValue("Lighter", true)
    private val lavaBucketValue = BoolValue("Lava", true)
    private val randomSlotValue = BoolValue("RandomSlot", false)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)

    /**
     * Variables
     */
    private val msTimer = MSTimer()

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        if (!msTimer.hasTimePassed(500L)) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler
        val controller = mc.playerController

        val inventoryContainer = thePlayer.inventoryContainer

        val itemDelay = itemDelayValue.get()
        val randomSlot = randomSlotValue.get()

        val lighterInHotbar = if (lighterValue.get()) inventoryContainer.findItem(36, 45, Items.flint_and_steel, itemDelay.toLong(), randomSlot) else -1
        val lavaInHotbar = if (lavaBucketValue.get()) inventoryContainer.findItem(26, 45, Items.lava_bucket, itemDelay.toLong(), randomSlot) else -1

        if (lighterInHotbar == -1 && lavaInHotbar == -1) return

        val fireInHotbar = if (lighterInHotbar == -1) lavaInHotbar else lighterInHotbar

        theWorld.getEntitiesInRadius(thePlayer, 8.0).asSequence().filterIsInstance<EntityLivingBase>().filterNot(Entity::isBurning).filter { it.isSelected(true) }.map(Entity::getPosition).filter { thePlayer.getDistanceSq(it) < 22.3 }.filter(theWorld::isReplaceable).firstOrNull { theWorld.getBlock(it) is BlockAir }?.let { blockPos ->
            RotationUtils.keepCurrentRotation = true
            netHandler.addToSendQueue(C09PacketHeldItemChange(fireInHotbar - 36))

            val itemStack = thePlayer.inventory.getStackInSlot(fireInHotbar)

            if (itemStack != null && itemStack.item is ItemBucket)
            {
                val diffX = blockPos.x + 0.5 - thePlayer.posX
                val diffY = blockPos.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
                val diffZ = blockPos.z + 0.5 - thePlayer.posZ
                val sqrt = hypot(diffX, diffZ)

                val yaw = (atan2(diffZ, diffX).toFloat()).toDegrees - 90.0f
                val pitch = -(atan2(diffY, sqrt).toFloat()).toDegrees

                CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

                netHandler.addToSendQueue(C05PacketPlayerLook(thePlayer.rotationYaw + (yaw - thePlayer.rotationYaw).wrapAngleTo180, thePlayer.rotationPitch + (pitch - thePlayer.rotationPitch).wrapAngleTo180, thePlayer.onGround))

                controller.sendUseItem(thePlayer, theWorld, itemStack)
            }
            else run {
                EnumFacing.values().forEach { side ->
                    val neighbor = blockPos.offset(side)

                    if (!theWorld.canBeClicked(neighbor)) return@forEach

                    val diffX = neighbor.x + 0.5 - thePlayer.posX
                    val diffY = neighbor.y + 0.5 - (thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight)
                    val diffZ = neighbor.z + 0.5 - thePlayer.posZ
                    val sqrt = hypot(diffX, diffZ)

                    val yaw = (atan2(diffZ, diffX).toFloat()).toDegrees - 90.0f
                    val pitch = -(atan2(diffY, sqrt).toFloat()).toDegrees

                    netHandler.addToSendQueue(C05PacketPlayerLook(thePlayer.rotationYaw + (yaw - thePlayer.rotationYaw).wrapAngleTo180, thePlayer.rotationPitch + (pitch - thePlayer.rotationPitch).wrapAngleTo180, thePlayer.onGround))

                    CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

                    if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, neighbor, side.opposite, Vec3(side.directionVec)))
                    {
                        thePlayer.swingItem()
                        return@run
                    }
                }
            }

            netHandler.addToSendQueue(C09PacketHeldItemChange(thePlayer.inventory.currentItem))
            RotationUtils.keepCurrentRotation = false
            netHandler.addToSendQueue(C05PacketPlayerLook(thePlayer.rotationYaw, thePlayer.rotationPitch, thePlayer.onGround))
            msTimer.reset()
        }
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
