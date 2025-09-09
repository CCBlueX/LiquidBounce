package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.isInVoid
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import org.lwjgl.glfw.GLFW

@Suppress("TooManyFunctions")
object ModuleAutoStuck : ClientModule("AutoStuck", Category.WORLD) {

    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val resetTicks by int("ResetTicks", 300, 200..500, "ticks")
    private val pauseOnFlag by int("PauseOnFlag", 20, 0..100, "ticks")
    private val fallDistance by int("FallDistance", 15, 0..25, "blocks")
    private val scaffoldFreezeTicks by int("ScaffoldBlockedTicks", 3, 0..20, "ticks")
    private val directlyKeybind by key("Directly", GLFW.GLFW_KEY_V)
    private val onlyWithPearl by boolean("OnlyWithPearl", false)
    private val onlyDuringCombat by boolean("OnlyDuringCombat", false)
    private val onlyReceiveHit by boolean("OnlyReceiveHit", false)
    val immediately by boolean("Immediately", true)


    private const val LOWEST_Y = -64
    private var stuckTicks = 0
    private var stuckCooldown = 0
    private var lastGroundY = LOWEST_Y
    private var ignoreTicks = 0
    private var pauseTicks = 0
    private var pauseAutoStuck = 0

    private var freezingTicks = 0
    var scaffoldBlocked = false
    var freezing = false
    var forceStuck = false
    var shouldEnableStuck = false
    var shouldActivate = false

    private fun hasPearlInHotbar() =
        player.inventory.main.any { it?.item == Items.ENDER_PEARL }

    private fun reset(disable: Boolean) {
        if (disable) {
            if (shouldEnableStuck) {
                shouldEnableStuck = false
                shouldActivate = false
            }
        }

        lastGroundY = LOWEST_Y
        stuckTicks = 0
        stuckCooldown = 0
        ignoreTicks = 0
        pauseTicks = 0
        freezing = false
        freezingTicks = 0
        scaffoldBlocked = false
        shouldEnableStuck = false
        shouldActivate = false
        forceStuck = false
    }


    @Suppress("unused")
    private val serverConnectHandler = handler<ServerConnectEvent> {
        ignoreTicks = 20
    }


    @Suppress("unused")
    private val keyHandler = handler<KeyEvent> {
        if (it.action != GLFW.GLFW_PRESS) return@handler

        if (it.key.code == directlyKeybind.code) {
            forceStuck = !forceStuck

            if (shouldEnableStuck) {

                pauseAutoStuck = 50
                shouldEnableStuck = false
                shouldActivate = false
            }
        }
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        if (shouldEnableStuck || forceStuck) {
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
        if (!shouldEnableStuck && !forceStuck) return@handler
        val packet = event.packet

        if (!player.isOnGround) {
            freezing = true
            freezingTicks++

            if (freezingTicks >= scaffoldFreezeTicks) {
                scaffoldBlocked = true
            }
            when (packet) {
                is PlayerPositionLookS2CPacket -> {
                    reset(true)
                    pauseTicks = pauseOnFlag
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
                            packet.hand, packet.sequence, player.yaw, player.pitch
                        )
                    )
                }
                is PlayerInteractEntityC2SPacket ->{
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
                is PlayerInteractBlockC2SPacket ->{
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
            }
        } else if (freezing && !forceStuck) {
            shouldEnableStuck = false
            shouldActivate = false
        }
    }

    private fun shouldDisableStuck(): Boolean =
        player.isInsideWaterOrBubbleColumn || voidFallPrediction.hasSolidBlockBelow()

    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        reset(true)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        if (player.isSpectator || ignoreTicks > 0 || player.y <= 0) {
            ignoreTicks--
            reset(true)
            return@handler
        }

        if (!immediately && player.isOnGround) lastGroundY = player.y.toInt() - 1

        if (stuckCooldown > 0) {
            stuckCooldown--
            return@handler
        }

        if (shouldEnableStuck) {
            stuckTicks++
            if (stuckTicks >= resetTicks || shouldDisableStuck()) {
                reset(true)
            }
        } else {
            stuckTicks = 0
        }
        if (pauseAutoStuck > 0) {
            pauseAutoStuck--
            shouldActivate = false
        } else {
            shouldActivate = isReadyToActivate()
        }


        if (shouldActivate && !shouldEnableStuck && stuckCooldown <= 0 && !shouldDisableStuck()) {
            shouldEnableStuck = true
            freezing = false
        } else if (!shouldActivate && shouldEnableStuck) {
            shouldEnableStuck = false
            ModuleScaffold.enabled = false
        }
    }



    private fun isReadyToActivate(): Boolean {
        val combatReady = !onlyDuringCombat || CombatManager.isInCombat
        val receiveHitReady = !onlyReceiveHit || CombatManager.isReceiveHit
        val pearlReady = !onlyWithPearl || hasPearlInHotbar()

        val airReady = !player.isOnGround
        val voidReady = if (immediately) {
            voidFallPrediction.isVoidFallImminent
        } else {
            player.y <= lastGroundY + 1 - fallDistance && isInVoid(player.pos)
        }
        return combatReady && pearlReady && airReady && voidReady && receiveHitReady
    }

    override fun onEnabled() {
        reset(false)
    }

    override fun onDisabled() {
        reset(true)
    }
}
