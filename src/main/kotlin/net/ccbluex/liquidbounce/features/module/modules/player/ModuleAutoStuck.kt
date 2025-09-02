package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoClutchHelper
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket


@Suppress("TooManyFunctions")
object ModuleAutoStuck : ClientModule("AutoStuck", Category.WORLD) {

    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val resetTicks by int("ResetTicks", 300, 200..500, "ticks")
    private val fallDistance by int("FallDistance", 5, 0..25, "blocks")
    private val onlyWithPearl by boolean("OnlyWithPearl", false)
    private val onlyDuringCombat by boolean("OnlyDuringCombat", false)
    private val onlyReceiveHit by boolean("OnlyReceiveHit", false)
    val alwaysInVoid by boolean("AlwaysInVoid", true)

    private const val LOWEST_Y = -64
    private var stuckTicks = 0
    private var stuckCooldown = 0
    private var lastGroundY = LOWEST_Y
    private var ignoreTicks = 0
    private var scaffolding = false

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
        player.isInsideWaterOrBubbleColumn || voidFallPrediction.hasSolidBlockBelow()

    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        lastGroundY = LOWEST_Y
    }
    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        if (player.isSpectator || ignoreTicks > 0 || player.y <= 0) {
            ignoreTicks--
            shouldEnableStuck = false
            shouldActivate = false
            return@handler
        }

        if (!alwaysInVoid && player.isOnGround) lastGroundY = player.y.toInt() - 1

        if (stuckCooldown > 0) {
            stuckCooldown--
            return@handler
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
                scaffolding = true
            }
            if (player.isOnGround) {
                if (scaffolding) {
                    ModuleScaffold.enabled = false
                    scaffolding = false
                }
            }
        } else {
            if (scaffolding) {
                ModuleScaffold.enabled = false
                scaffolding = false
            }
        }


    }
    private fun shouldEnableScaffold(): Boolean {
        val scaffoldCombatReady = !ScaffoldAutoClutchHelper.scaffoldOnlyDuringCombat || CombatManager.isInCombat
        val scaffoldReceiveHit = !ScaffoldAutoClutchHelper.scaffoldOnlyReceiveHit || CombatManager.isReceiveHit
            return alwaysInVoid
                && voidFallPrediction.isVoidFallImminent
                && ScaffoldAutoClutchHelper.enabled
                && scaffoldCombatReady
                && scaffoldReceiveHit
                && player.pos.add(0.0, -1.0, 0.0).searchBlocksInRadius(4.5f) { _, state ->
            !state.isAir
        }.any()
    }


    private fun isReadyToActivate(): Boolean {
        val combatReady = !onlyDuringCombat || CombatManager.isInCombat
        val receiveHitReady = !onlyReceiveHit || CombatManager.isReceiveHit
        val pearlReady = !onlyWithPearl || hasPearlInHotbar()

        val airReady = !player.isOnGround
        val voidReady = if (alwaysInVoid) {
            voidFallPrediction.isVoidFallImminent
        } else {
            player.y <= lastGroundY + 1 - fallDistance
        }
        return combatReady && pearlReady && airReady && voidReady && receiveHitReady
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
