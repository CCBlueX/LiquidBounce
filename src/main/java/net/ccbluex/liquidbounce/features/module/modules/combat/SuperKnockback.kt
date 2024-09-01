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
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.*
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
        val player = mc.player ?: return
        val target = event.targetEntity as? LivingEntity ?: return
        val distance = player.getDistanceToEntityBox(target)

        val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
        val angleDifferenceToPlayer = abs(getAngleDifference(rotationToPlayer, target.yaw))

        if (event.targetEntity.hurtTime > hurtTime || !timer.hasTimePassed(delay) || onlyGround && !player.onGround || RandomUtils.nextInt(
                endExclusive = 100
            ) > chance) return

        if (onlyMove && (!isMoving || onlyMoveForward && player.input.movementSideways != 0f)) return

        // Is the enemy facing his back on us?
        if (angleDifferenceToPlayer > minEnemyRotDiffToIgnore && !target.hitBox.contains(player.eyes)) return

        when (mode) {
            "Old" -> {
                // Users reported that this mode is better than the other ones
                if (player.isSprinting) {
                    sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                }

                sendPackets(
                    ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING),
                    ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING),
                    ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING)
                )
                player.isSprinting = true
                player.lastSprinting = true
            }

            "SprintTap", "Silent" -> if (player.isSprinting && player.lastSprinting) ticks = 2

            "Packet" -> {
                sendPackets(
                    ClientCommandC2SPacket(player, STOP_SPRINTING),
                    ClientCommandC2SPacket(player, START_SPRINTING)
                )
            }

            "SneakPacket" -> {
                sendPackets(
                    ClientCommandC2SPacket(player, STOP_SPRINTING),
                    ClientCommandC2SPacket(player, START_SNEAKING),
                    ClientCommandC2SPacket(player, START_SPRINTING),
                    ClientCommandC2SPacket(player, STOP_SNEAKING)
                )
            }

            "WTap" -> {
                // We want the player to be sprinting before we block inputs
                if (player.isSprinting && player.lastSprinting && !blockInput && !startWaiting) {
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

                    if (player.isSprinting && player.lastSprinting) {
                        player.isSprinting = false
                        player.lastSprinting = false
                    } else {
                        player.isSprinting = true
                        player.lastSprinting = true
                    }

                    mc.player.stopXZ()

                } else if (sprintTicks >= unSprintTicks.get()) {

                    player.isSprinting = false
                    player.lastSprinting = false

                    sprintTicks = 0
                }
            }
        }

        timer.reset()
    }

    @EventTarget
    fun onPostSprintUpdate(event: PostSprintUpdateEvent) {
        val player = mc.player ?: return
        if (mode == "SprintTap") {
            when (ticks) {
                2 -> {
                    player.isSprinting = false
                    forceSprintState = 2
                    ticks--
                }

                1 -> {
                    if (player.input.movementForward > 0.8) {
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
        val player = mc.player ?: return
        val packet = event.packet
        if (packet is PlayerMoveC2SPacket && mode == "Silent") {
            if (ticks == 2) {
                sendPacket(ClientCommandC2SPacket(player, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1 && player.isSprinting) {
                sendPacket(ClientCommandC2SPacket(player, START_SPRINTING))
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
