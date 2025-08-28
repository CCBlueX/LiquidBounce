package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.autoclutch.ModuleAutoClutch.isVoidFallImminent
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoClutchHelper
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import kotlin.math.floor

@Suppress("TooManyFunctions")
object ModuleAutoStuck : ClientModule("AutoStuck", Category.WORLD) {
    private val resetTicks by int("ResetTicks", 300, 200..500, "ticks")
    private val fallDistance by int("FallDistance", 5, 0..25, "blocks")
    private val onlyPearl by boolean("OnlyPearl", true)
    private val onlyDuringCombat by boolean("OnlyDuringCombat", false)
    val alwaysInVoid by boolean("AlwaysInVoid", true)


    private const val LOWEST_Y = -64
    private var stuckTicks = 0
    private var stuckCooldown = 0
    private var lastGroundY = LOWEST_Y
    private var ignoreTicks = 0

    var isInAir = false
    var shouldEnableStuck = false
    var shouldActivate = false

    private fun hasPearlInHotbar() =
        player.inventory.main.any { it?.item == Items.ENDER_PEARL }

    @Suppress("unused")
    private val serverConnectHandler = handler<ServerConnectEvent> {
        ignoreTicks = 20
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        if (shouldEnableStuck) {
            player.movement.x = 0.0
            player.movement.y = 0.0
            player.movement.z = 0.0
            it.directionalInput = DirectionalInput(
                forwards = false,
                backwards = false,
                left = false,
                right = false
            )
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (!shouldEnableStuck) return@handler

        if (!player.isOnGround) {
            isInAir = true

            when (event.packet) {
                is PlayerPositionLookS2CPacket -> {
                    shouldEnableStuck = false
                    shouldActivate = false
                }
                is PlayerMoveC2SPacket -> event.cancelEvent()
                is PlayerInteractItemC2SPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                        )
                    )
                    sendPacketSilently(
                        PlayerInteractItemC2SPacket(
                            event.packet.hand, event.packet.sequence, player.yaw, player.pitch
                        )
                    )
                }
            }
        } else if (isInAir) {
            shouldEnableStuck = false
            shouldActivate = false
        }
    }

    private fun shouldDisableStuck(): Boolean =
        player.isInsideWaterOrBubbleColumn || hasSolidBlockBelow()

    @Suppress("NestedBlockDepth")
    private fun hasSolidBlockBelow(): Boolean {
        val checkDepth = 3
        val bb = player.boundingBox
        val minX = floor(bb.minX).toInt()
        val maxX = floor(bb.maxX).toInt()
        val minZ = floor(bb.minZ).toInt()
        val maxZ = floor(bb.maxZ).toInt()

        for (dy in 0..checkDepth) {
            val y = floor(bb.minY).toInt() - dy
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val state = BlockPos(x, y, z).getState()
                    if (state != null && !state.isAir) {
                        return true
                    }
                }
            }
        }
        return false
    }


    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        lastGroundY = LOWEST_Y
    }
    @Suppress("unused")
    private val tickHandler = tickHandler {
        val world = mc.world ?: return@tickHandler
        val player = mc.player ?: return@tickHandler

        if (player.isSpectator || ignoreTicks > 0 || player.y <= 0) {
            ignoreTicks--
            shouldEnableStuck = false
            shouldActivate = false
            return@tickHandler
        }

        if (!alwaysInVoid && player.isOnGround) lastGroundY = player.y.toInt() - 1

        if (stuckCooldown > 0) {
            stuckCooldown--
            return@tickHandler
        }

        if (shouldEnableStuck) {
            stuckTicks++
            if (stuckTicks >= resetTicks || shouldDisableStuck()) {
                stuckTicks = 0
                shouldEnableStuck = false
                stuckCooldown = 1
            }
        } else {
            stuckTicks = 0
        }

        shouldActivate = isReadyToActivate()

        if (shouldActivate && !shouldEnableStuck && stuckCooldown <= 0 && !shouldDisableStuck()) {
            shouldEnableStuck = true
            isInAir = false
        } else if (!shouldActivate && shouldEnableStuck) {
            shouldEnableStuck = false
            ModuleScaffold.enabled = false
        }

        if (shouldEnableScaffold()) {
            if (!ModuleScaffold.enabled) {
                ModuleScaffold.enabled = true
            }
            if (player.isOnGround) {
                ModuleScaffold.enabled = false
            }
        }
    }
    private fun shouldEnableScaffold(): Boolean {
        val scaffoldCombatReady = !ScaffoldAutoClutchHelper.scaffoldOnlyDuringCombat || CombatManager.isInCombat
        val scaffoldReceiveHit = !ScaffoldAutoClutchHelper.scaffoldOnlyDuringCombat || CombatManager.isReceiveHit
            return alwaysInVoid
                && isVoidFallImminent
                && ScaffoldAutoClutchHelper.enabled
                && scaffoldCombatReady
                && scaffoldReceiveHit
                && player.pos.add(0.0, -1.0, 0.0).searchBlocksInRadius(4.5f) { _, state ->
            !state.isAir
        }.any()
    }


    private fun isReadyToActivate(): Boolean {
        val combatReady = !onlyDuringCombat || CombatManager.isInCombat
        val pearlReady = !onlyPearl || hasPearlInHotbar()

        val airReady = !player.isOnGround
        val voidReady = if (alwaysInVoid) isVoidFallImminent else player.y <= lastGroundY + 1 - fallDistance
        return combatReady && pearlReady && airReady && voidReady
    }

    override fun onEnabled() {
        stuckTicks = 0
        isInAir = false
        lastGroundY = LOWEST_Y
    }

    override fun onDisabled() {
        shouldEnableStuck = false
    }
}
