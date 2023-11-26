/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*

object NoSlow : Module("NoSlow", ModuleCategory.MOVEMENT, gameDetecting = false) {

    private val swordMode by ListValue("SwordMode", arrayOf("None", "NCP", "UpdatedNCP", "AAC5", "SwitchItem"), "None")

    private val blockForwardMultiplier by FloatValue("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by FloatValue("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumePacket by ListValue("ConsumeMode", arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem"), "None")

    private val consumeForwardMultiplier by FloatValue("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by FloatValue("ConsumeStrafeMultiplier", 1f, 0.2F..1f)

    private val bowForwardMultiplier by FloatValue("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by FloatValue("BowStrafeMultiplier", 1f, 0.2F..1f)

    // Blocks
    val soulsand by BoolValue("Soulsand", true)
    val liquidPush by BoolValue("LiquidPush", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return
        val heldItem = thePlayer.heldItem ?: return
        val currentItem = thePlayer.inventory.currentItem

        if (!isMoving) {
            return
        }

        if ((heldItem.item is ItemFood || heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk) && thePlayer.isUsingItem) {
            when (consumePacket.lowercase()) {
                "aac5" -> {
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f))
                }
                "switchitem" -> {
                    when (event.eventState) {
                        EventState.PRE -> {
                            serverSlot = (serverSlot + 1) % 9
                            serverSlot = currentItem
                        }                          


                        else -> {}
                    }
                }
                "updatedncp" -> {
                    when (event.eventState) {
                        EventState.POST -> {
                            sendPacket(
                                C08PacketPlayerBlockPlacement(
                                    BlockPos.ORIGIN, 5, heldItem, 0f, 0f, 0f
                                )
                            )
                        }

                        else -> {}
                    }
                }
                else -> {
                    return
                }
            }
        }

        if (heldItem.item is ItemSword && thePlayer.isBlocking) {
            when (swordMode.lowercase()) {
                "none" -> {
                    return
                }

                "ncp" -> {
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            C07PacketPlayerDigging(
                                RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN
                            )
                        )

                        EventState.POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f
                            )
                        )

                        else -> {}
                    }
                }

                "updatedncp" -> {
                    when (event.eventState) {
                        EventState.POST -> {
                            sendPacket(
                                C08PacketPlayerBlockPlacement(
                                    BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f
                                )
                            )
                        }

                        else -> {}
                    }
                }

                "aac5" -> {
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f
                            )
                        )
                    }
                }

                "switchitem" -> {
                    when (event.eventState) {
                        EventState.PRE -> {
                            serverSlot = (serverSlot + 1) % 9
                            serverSlot = currentItem
                        }                          


                        else -> {}
                    }
                }
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val heldItem = mc.thePlayer.heldItem?.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    /**
     * Not sure how it works, but it should allow you to block again
     * after jumping by stopping the player xz.
     */
    @EventTarget
    fun onJump(event: JumpEvent) {
        if (swordMode.lowercase() == "updatedncp" && heldItem.item is ItemSword && thePlayer.isBlocking) {
            mc.thePlayer.stopXZ()
        }
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

        is ItemSword -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is ItemBow -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }
}
