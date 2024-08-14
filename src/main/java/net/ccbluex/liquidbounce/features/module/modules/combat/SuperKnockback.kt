/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.getAngleDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import kotlin.math.abs

object SuperKnockback : Module("SuperKnockback", Category.COMBAT, hideModule = false) {

    private val chance by IntegerValue("Chance", 100, 0..100)
    private val delay by IntegerValue("Delay", 0, 0..500)
    private val hurtTime by IntegerValue("HurtTime", 10, 0..10)

    private val mode by ListValue("Mode",
        arrayOf("WTap", "SprintTap", "SprintTap2", "Old", "Silent", "Packet", "SneakPacket"),
        "Old"
    )
    private val maxTicksUntilBlock: IntegerValue = object : IntegerValue("MaxTicksUntilBlock", 2, 0..5) {
        override fun isSupported() = mode == "WTap"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minTicksUntilBlock.get())
    }
    private val minTicksUntilBlock: IntegerValue = object : IntegerValue("MinTicksUntilBlock", 0, 0..5) {
        override fun isSupported() = mode == "WTap"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxTicksUntilBlock.get())
    }

    private val reSprintMaxTicks: IntegerValue = object : IntegerValue("ReSprintMaxTicks", 2, 1..5) {
        override fun isSupported() = mode == "WTap"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(reSprintMinTicks.get())
    }
    private val reSprintMinTicks: IntegerValue = object : IntegerValue("ReSprintMinTicks", 1, 1..5) {
        override fun isSupported() = mode == "WTap"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(reSprintMaxTicks.get())
    }

    private val targetDistance by IntegerValue("TargetDistance", 3, 1..5) { mode == "WTap" }

    private val stopTicks: IntegerValue = object : IntegerValue("PressBackTicks", 1, 1..5) {
        override fun isSupported() = mode == "SprintTap2"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(unSprintTicks.get())
    }
    private val unSprintTicks: IntegerValue = object : IntegerValue("ReleaseBackTicks", 2, 1..5) {
        override fun isSupported() = mode == "SprintTap2"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(stopTicks.get())
    }

    private val minEnemyRotDiffToIgnore by FloatValue("MinRotationDiffFromEnemyToIgnore", 180f, 0f..180f)

    private val onlyGround by BoolValue("OnlyGround", false)
    val onlyMove by BoolValue("OnlyMove", true)
    val onlyMoveForward by BoolValue("OnlyMoveForward", true) { onlyMove }

    private var ticks = 0
    private var forceSprintState = 0
    private val timer = MSTimer()

    // WTap
    private var blockInputTicks = randomDelay(minTicksUntilBlock.get(), maxTicksUntilBlock.get())
    private var blockTicksElapsed = 0
    private var startWaiting = false
    private var blockInput = false
    private var allowInputTicks = randomDelay(reSprintMinTicks.get(), reSprintMaxTicks.get())
    private var ticksElapsed = 0

    // SprintTap2
    private var sprintTicks = 0

    override fun onToggle(state: Boolean) {
        // Make sure the user won't have their input forever blocked
        blockInput = false
        startWaiting = false
        blockTicksElapsed = 0
        ticksElapsed = 0
        sprintTicks = 0
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer ?: return
        val target = event.targetEntity as? EntityLivingBase ?: return
        val distance = player.getDistanceToEntityBox(target)

        val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
        val angleDifferenceToPlayer = abs(getAngleDifference(rotationToPlayer, target.rotationYaw))

        if (event.targetEntity.hurtTime > hurtTime || !timer.hasTimePassed(delay) || onlyGround && !player.onGround || RandomUtils.nextInt(
                endExclusive = 100
            ) > chance) return

        if (onlyMove && (!isMoving || onlyMoveForward && player.movementInput.moveStrafe != 0f)) return

        // Is the enemy facing his back on us?
        if (angleDifferenceToPlayer > minEnemyRotDiffToIgnore && !target.hitBox.isVecInside(player.eyes)) return

        when (mode) {
            "Old" -> {
                // Users reported that this mode is better than the other ones
                if (player.isSprinting) {
                    sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
                }

                sendPackets(
                    C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING),
                    C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING),
                    C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING)
                )
                player.isSprinting = true
                player.serverSprintState = true
            }

            "SprintTap", "Silent" -> if (player.isSprinting && player.serverSprintState) ticks = 2

            "Packet" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SPRINTING)
                )
            }

            "SneakPacket" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SNEAKING),
                    C0BPacketEntityAction(player, START_SPRINTING),
                    C0BPacketEntityAction(player, STOP_SNEAKING)
                )
            }

            "WTap" -> {
                // We want the player to be sprinting before we block inputs
                if (player.isSprinting && player.serverSprintState && !blockInput && !startWaiting) {
                    val delayMultiplier = 1.0 / (abs(targetDistance - distance) + 1)

                    blockInputTicks = (randomDelay(minTicksUntilBlock.get(),
                        maxTicksUntilBlock.get()
                    ) * delayMultiplier).toInt()

                    blockInput = blockInputTicks == 0

                    if (!blockInput) {
                        startWaiting = true
                    }

                    allowInputTicks = (randomDelay(reSprintMinTicks.get(),
                        reSprintMaxTicks.get()
                    ) * delayMultiplier).toInt()
                }
            }

            "SprintTap2" -> {
                if (++sprintTicks == stopTicks.get()) {

                    if (player.isSprinting && player.serverSprintState) {
                        player.isSprinting = false
                        player.serverSprintState = false
                    } else {
                        player.isSprinting = true
                        player.serverSprintState = true
                    }

                    mc.thePlayer.stopXZ()

                } else if (sprintTicks >= unSprintTicks.get()) {

                    player.isSprinting = false
                    player.serverSprintState = false

                    sprintTicks = 0
                }
            }
        }

        timer.reset()
    }

    @EventTarget
    fun onPostSprintUpdate(event: PostSprintUpdateEvent) {
        val player = mc.thePlayer ?: return
        if (mode == "SprintTap") {
            when (ticks) {
                2 -> {
                    player.isSprinting = false
                    forceSprintState = 2
                    ticks--
                }

                1 -> {
                    if (player.movementInput.moveForward > 0.8) {
                        player.isSprinting = true
                    }
                    forceSprintState = 1
                    ticks--
                }

                else -> {
                    forceSprintState = 0
                }
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mode == "WTap") {
            if (blockInput) {
                if (ticksElapsed++ >= allowInputTicks) {
                    blockInput = false
                    ticksElapsed = 0
                }
            } else {
                if (startWaiting) {
                    blockInput = blockTicksElapsed++ >= blockInputTicks

                    if (blockInput) {
                        startWaiting = false
                        blockTicksElapsed = 0
                    }
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet
        if (packet is C03PacketPlayer && mode == "Silent") {
            if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1 && player.isSprinting) {
                sendPacket(C0BPacketEntityAction(player, START_SPRINTING))
                ticks--
            }
        }
    }

    fun shouldBlockInput() = handleEvents() && mode == "WTap" && blockInput

    override val tag
        get() = mode

    fun breakSprint() = handleEvents() && forceSprintState == 2 && mode == "SprintTap"
    fun startSprint() = handleEvents() && forceSprintState == 1 && mode == "SprintTap"
}
