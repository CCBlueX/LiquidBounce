/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object NoSlow : Module("NoSlow", Category.MOVEMENT, gameDetecting = false, hideModule = false) {

    private val swordMode by ListValue("SwordMode", arrayOf("None", "NCP", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Blink"), "None")

    private val reblinkTicks by IntegerValue("ReblinkTicks", 10,1..20) { swordMode == "Blink" }

    private val blockForwardMultiplier by FloatValue("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by FloatValue("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumePacket by ListValue("ConsumeMode", arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Intave"), "None")

    private val consumeForwardMultiplier by FloatValue("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by FloatValue("ConsumeStrafeMultiplier", 1f, 0.2F..1f)
    private val consumeFoodOnly by BoolValue("ConsumeFoodOnly", true) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }
    private val consumeDrinkOnly by BoolValue("ConsumeDrinkOnly", true) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }

    private val bowPacket by ListValue("BowMode", arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08"), "None")

    private val bowForwardMultiplier by FloatValue("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by FloatValue("BowStrafeMultiplier", 1f, 0.2F..1f)

    // Blocks
    val soulsand by BoolValue("Soulsand", true)
    val liquidPush by BoolValue("LiquidPush", true)

    private var shouldSwap = false

    private var shouldBlink = true

    private val BlinkTimer = TickTimer()

    override fun onDisable() {
        shouldSwap = false
        shouldBlink = true
        BlinkTimer.reset()
        BlinkUtils.unblink()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val player = mc.player ?: return
        val mainHandStack = player.mainHandStack ?: return
        val currentItem = player.inventory.selectedSlot
        val isUsingItem = usingItemFunc()

        if (mc.player.velocityX == 0.0 && mc.player.velocityZ == 0.0 && !shouldSwap)
            return

        if (!consumeFoodOnly && mainHandStack.item is FoodItem || !consumeDrinkOnly && (mainHandStack.item is PotionItem || mainHandStack.item is MilkBucketItem))
            return

        if ((mainHandStack.item is FoodItem || mainHandStack.item is PotionItem || mainHandStack.item is MilkBucketItem) && (isUsingItem || shouldSwap)) {
            when (consumePacket.lowercase()) {
                "aac5" ->
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mainHandStack, 0f, 0f, 0f))

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        serverSlot = (serverSlot + 1) % 9
                        serverSlot = currentItem
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.PRE && shouldSwap) {
                        serverSlot = (serverSlot + 1) % 9
                        serverSlot = currentItem
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mainHandStack, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }

                "intave" -> {
                    if (event.eventState == EventState.PRE) {
                        sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.UP))
                    }
                }
                
                else -> return
            }
        }

        if (mainHandStack.item is BowItem && (isUsingItem || shouldSwap)) {
            when (bowPacket.lowercase()) {
                "aac5" ->
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mainHandStack, 0f, 0f, 0f))
                
                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        serverSlot = (serverSlot + 1) % 9
                        serverSlot = currentItem
                    }
                
                "updatedncp" ->
                    if (event.eventState == EventState.PRE && shouldSwap) {
                        serverSlot = (serverSlot + 1) % 9
                        serverSlot = currentItem
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mainHandStack, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }

                else -> return
            }
        }

        if (mainHandStack.item is SwordItem && isUsingItem) {
            when (swordMode.lowercase()) {
                "none" -> return

                "ncp" ->
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN)
                        )

                        EventState.POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, mainHandStack, 0f, 0f, 0f
                            )
                        )

                        else -> return
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos.ORIGIN, 255, mainHandStack, 0f, 0f, 0f
                            )
                        )
                    }

                "aac5" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, player.mainHandStack, 0f, 0f, 0f
                            )
                        )
                    }

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        serverSlot = (serverSlot + 1) % 9
                        serverSlot = currentItem
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val player = mc.player ?: return

        if (event.isCancelled || shouldSwap)
            return

        if (swordMode == "Blink") {
            when (packet) {
                is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is ChatMessageC2SPacket, is S01PacketPong -> return

                is PlayerActionC2SPacket, is C02PacketUseEntity, is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                    BlinkTimer.update()
                    if (shouldBlink && BlinkTimer.hasTimePassed(reblinkTicks) && (BlinkUtils.packetsReceived.isNotEmpty() || BlinkUtils.packets.isNotEmpty())) {
                        BlinkUtils.unblink()
                        BlinkTimer.reset()
                        shouldBlink = false
                    } else if (!BlinkTimer.hasTimePassed(reblinkTicks)) {
                        shouldBlink = true
                    }
                    return
                }

                // Flush on kb
                is S12PacketEntityVelocity -> {
                    if (mc.player.entityId == packet.entityID) {
                        BlinkUtils.unblink()
                        return
                    }
                }

                // Flush on explosion
                is S27PacketExplosion -> {
                    if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                        BlinkUtils.unblink()
                        return
                    }
                }

                is C03PacketPlayer -> {
                    if (swordMode == "Blink") {
                        if (isMoving) {
                            if (player.mainHandStack?.item is SwordItem && usingItemFunc()) {
                                if (shouldBlink)
                                    BlinkUtils.blink(packet, event)
                            } else {
                                shouldBlink = true
                                BlinkUtils.unblink()
                            }
                        }
                    }
                }
            }
        }

        when (packet) {
            is C08PacketPlayerBlockPlacement -> {
                if (packet.stack?.item != null && player.mainHandStack?.item != null && packet.stack.item == mc.player.mainHandStack?.item) {
                    if ((consumePacket == "UpdatedNCP" && (packet.stack.item is FoodItem || packet.stack.item is PotionItem || packet.stack.item is MilkBucketItem)) || (bowPacket == "UpdatedNCP" && packet.stack.item is BowItem)) {
                        shouldSwap = true;
                    }
                }
            }
        }
    }
    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val mainHandStack = mc.player.mainHandStack?.item

        if (!consumeFoodOnly && mainHandStack is FoodItem || !consumeDrinkOnly && (mainHandStack is PotionItem || mainHandStack is MilkBucketItem))
            return

        event.forward = getMultiplier(mainHandStack, true)
        event.strafe = getMultiplier(mainHandStack, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is FoodItem, is PotionItem, is MilkBucketItem -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

        is SwordItem -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is BowItem -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }

    fun isUNCPBlocking() = swordMode == "UpdatedNCP" && mc.options.useKey.isPressed && (mc.player.mainHandStack?.item is SwordItem)
    fun usingItemFunc() = mc.player?.mainHandStack != null && (mc.player.isUsingItem || (mc.player.mainHandStack?.item is SwordItem && KillAura.blockStatus) || isUNCPBlocking())
}
