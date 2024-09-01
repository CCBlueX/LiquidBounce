/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.isOnGround
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.PacketUtils.queuedPackets
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.realMotionX
import net.ccbluex.liquidbounce.utils.realMotionY
import net.ccbluex.liquidbounce.utils.realMotionZ
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.AirBlock
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.packet.c2s.play.ConfirmGuiActionC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.ConfirmGuiActionS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object Velocity : Module("Velocity", Category.COMBAT, hideModule = false) {

    /**
     * OPTIONS
     */
    private val mode by ListValue(
        "Mode", arrayOf(
            "Simple", "AAC", "AACPush", "AACZero", "AACv4",
            "Reverse", "SmoothReverse", "Jump", "Glitch", "Legit",
            "GhostBlock", "Vulcan", "S32Packet", "MatrixReduce",
            "Intave", "Delay", "GrimC03", "HypixelAir"
        ), "Simple"
    )

    private val horizontal by FloatValue("Horizontal", 0F, 0F..1F) { mode in arrayOf("Simple", "AAC", "Legit") }
    private val vertical by FloatValue("Vertical", 0F, 0F..1F) { mode in arrayOf("Simple", "Legit") }

    // Reverse
    private val reverseStrength by FloatValue("ReverseStrength", 1F, 0.1F..1F) { mode == "Reverse" }
    private val reverse2Strength by FloatValue("SmoothReverseStrength", 0.05F, 0.02F..0.1F) { mode == "SmoothReverse" }

    private val onLook by BoolValue("onLook", false) { mode in arrayOf("Reverse", "SmoothReverse") }
    private val range by FloatValue("Range", 3.0F, 1F..5.0F) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }
    private val maxAngleDifference by FloatValue("MaxAngleDifference", 45.0f, 5.0f..90f) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }

    // AAC Push
    private val aacPushXZReducer by FloatValue("AACPushXZReducer", 2F, 1F..3F) { mode == "AACPush" }
    private val aacPushYReducer by BoolValue("AACPushYReducer", true) { mode == "AACPush" }

    // AAC v4
    private val aacv4MotionReducer by FloatValue("AACv4MotionReducer", 0.62F, 0F..1F) { mode == "AACv4" }

    // Legit
    private val legitDisableInAir by BoolValue("DisableInAir", true) { mode == "Legit" }

    // Chance
    private val chance by IntegerValue("Chance", 100, 0..100) { mode == "Jump" || mode == "Legit" }

    // Jump
    private val jumpCooldownMode by ListValue("JumpCooldownMode", arrayOf("Ticks", "ReceivedHits"), "Ticks")
    { mode == "Jump" }
    private val ticksUntilJump by IntegerValue("TicksUntilJump", 4, 0..20)
    { jumpCooldownMode == "Ticks" && mode == "Jump" }
    private val hitsUntilJump by IntegerValue("ReceivedHitsUntilJump", 2, 0..5)
    { jumpCooldownMode == "ReceivedHits" && mode == "Jump" }

    // Ghost Block
    private val maxHurtTime: IntegerValue = object : IntegerValue("MaxHurtTime", 9, 1..10) {
        override fun isSupported() = mode == "GhostBlock"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minHurtTime.get())
    }

    private val minHurtTime: IntegerValue = object : IntegerValue("MinHurtTime", 1, 1..10) {
        override fun isSupported() = mode == "GhostBlock" && !maxHurtTime.isMinimal()
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(0, maxHurtTime.get())
    }

    // Delay
    private val spoofDelay by IntegerValue("SpoofDelay", 500, 0..5000) { mode == "Delay" }
    var delayMode = false

    private val pauseOnExplosion by BoolValue("PauseOnExplosion", true)
    private val ticksToPause by IntegerValue("TicksToPause", 20, 1..50) { pauseOnExplosion }

    // TODO: Could this be useful in other modes? (Jump?)
    // Limits
    private val limitMaxMotionValue = BoolValue("LimitMaxMotion", false) { mode == "Simple" }
    private val maxXZMotion by FloatValue("MaxXZMotion", 0.4f, 0f..1.9f) { limitMaxMotionValue.isActive() }
    private val maxYMotion by FloatValue("MaxYMotion", 0.36f, 0f..0.46f) { limitMaxMotionValue.isActive() }
    //0.00075 is added silently

    // Vanilla XZ limits
    // Non-KB: 0.4 (no sprint), 0.9 (sprint)
    // KB 1: 0.9 (no sprint), 1.4 (sprint)
    // KB 2: 1.4 (no sprint), 1.9 (sprint)
    // Vanilla Y limits
    // 0.36075 (no sprint), 0.46075 (sprint)

    /**
     * VALUES
     */
    private val velocityTimer = MSTimer()
    private var hasReceivedVelocity = false

    // SmoothReverse
    private var reverseHurt = false

    // AACPush
    private var jump = false

    // Jump
    private var limitUntilJump = 0

    // Intave
    private var intaveTick = 0

    // Delay
    private val packets = LinkedHashMap<Packet<*>, Long>()

    // Grim
    private var timerTicks = 0

    // Pause On Explosion
    private var pauseTicks = 0

    override val tag
        get() = if (mode == "Simple" || mode == "Legit") {
            val horizontalPercentage = (horizontal * 100).toInt()
            val verticalPercentage = (vertical * 100).toInt()

            "$horizontalPercentage% $verticalPercentage%"
        } else mode

    override fun onDisable() {
        pauseTicks = 0
        mc.player?.speedInAir = 0.02F
        timerTicks = 0
        reset()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || !player.isAlive)
            return

        when (mode.lowercase()) {
            "glitch" -> {
                player.noClip = hasReceivedVelocity

                if (player.hurtTime == 7)
                    player.velocityY = 0.4

                hasReceivedVelocity = false
            }

            "reverse" -> {
                val nearbyEntity = getNearestEntityInRange()

                if (!hasReceivedVelocity)
                    return

                if (nearbyEntity != null) {
                    if (!player.onGround) {
                        if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                            return
                        }

                        speed *= reverseStrength
                    } else if (velocityTimer.hasTimePassed(80))
                        hasReceivedVelocity = false
                }
            }

            "smoothreverse" -> {
                val nearbyEntity = getNearestEntityInRange()

                if (hasReceivedVelocity) {
                    if (nearbyEntity == null) {
                        player.speedInAir = 0.02F
                        reverseHurt = false
                    } else {
                        if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                            hasReceivedVelocity = false
                            player.speedInAir = 0.02F
                            reverseHurt = false
                        } else {
                            if (player.hurtTime > 0) {
                                reverseHurt = true
                            }

                            if (!player.onGround) {
                                player.speedInAir = if (reverseHurt) reverse2Strength else 0.02F
                            } else if (velocityTimer.hasTimePassed(80)) {
                                hasReceivedVelocity = false
                                player.speedInAir = 0.02F
                                reverseHurt = false
                            }
                        }
                    }
                }
            }

            "aac" -> if (hasReceivedVelocity && velocityTimer.hasTimePassed(80)) {
                player.velocityX *= horizontal
                player.velocityZ *= horizontal
                //mc.player.velocityY *= vertical ?
                hasReceivedVelocity = false
            }

            "aacv4" ->
                if (player.hurtTime > 0 && !player.onGround) {
                    val reduce = aacv4MotionReducer
                    player.velocityX *= reduce
                    player.velocityZ *= reduce
                }

            "aacpush" -> {
                if (jump) {
                    if (player.onGround)
                        jump = false
                } else {
                    // Strafe
                    if (player.hurtTime > 0 && player.velocityX != 0.0 && player.velocityZ != 0.0)
                        player.onGround = true

                    // Reduce Y
                    if (player.hurtTime > 0 && aacPushYReducer && !Speed.handleEvents())
                        player.velocityY -= 0.014999993
                }

                // Reduce XZ
                if (player.hurtTime >= 19) {
                    val reduce = aacPushXZReducer

                    player.velocityX /= reduce
                    player.velocityZ /= reduce
                }
            }

            "aaczero" ->
                if (player.hurtTime > 0) {
                    if (!hasReceivedVelocity || player.onGround || player.fallDistance > 2F)
                        return

                    player.velocityY -= 1.0
                    player.velocityDirty = true
                    player.onGround = true
                } else
                    hasReceivedVelocity = false

            "legit" -> {
                if (legitDisableInAir && !isOnGround(0.5))
                    return

                if (mc.player.maxHurtTime != mc.player.hurtTime || mc.player.maxHurtTime == 0)
                    return

                if (nextInt(endExclusive = 100) < chance) {
                    val horizontal = horizontal / 100f
                    val vertical = vertical / 100f

                    player.velocityX *= horizontal.toDouble()
                    player.velocityZ *= horizontal.toDouble()
                    player.velocityY *= vertical.toDouble()
                }
            }

            "intave" -> {
                intaveTick++
                if (hasReceivedVelocity && mc.player.hurtTime == 2) {
                    if (player.onGround && intaveTick % 2 == 0) {
                        player.tryJump()
                        intaveTick = 0
                    }
                    hasReceivedVelocity = false
                }
            }

            "hypixelair" -> {
                if (hasReceivedVelocity) {
                    if (player.onGround) {
                        player.tryJump()
                    }
                    hasReceivedVelocity = false
                }
            }
        }
    }

    private fun checkAir(blockPos: BlockPos): Boolean {
        val world = mc.world ?: return false

        if (!world.isAir(blockPos)) {
            return false
        }

        timerTicks = 20

        sendPackets(
            PlayerMoveC2SPacket(true),
            PlayerActionC2SPacket(STOP_DESTROY_BLOCK, blockPos, Direction.DOWN)
        )

        world.setAir(blockPos)

        return true
    }

    // TODO: Recode
    private fun getDirection(): Double {
        var moveYaw = mc.player.yaw
        if (mc.player.input.movementForward  != 0f && mc.player.input.movementSideways == 0f) {
            moveYaw += if (mc.player.input.movementForward  > 0) 0 else 180
        } else if (mc.player.input.movementForward  != 0f && mc.player.input.movementSideways != 0f) {
            if (mc.player.input.movementForward  > 0) moveYaw += if (mc.player.input.movementSideways > 0) -45 else 45 else moveYaw -= if (mc.player.input.movementSideways > 0) -45 else 45
            moveYaw += if (mc.player.input.movementForward  > 0) 0 else 180
        } else if (mc.player.input.movementSideways != 0f && mc.player.input.movementForward  == 0f) {
            moveYaw += if (mc.player.input.movementSideways > 0) -90 else 90
        }
        return Math.floorMod(moveYaw.toInt(), 360).toDouble()
    }

    @EventTarget(priority = 1)
    fun onPacket(event: PacketEvent) {
        val player = mc.player ?: return

        val packet = event.packet

        if (!handleEvents())
            return

        if (pauseTicks > 0) {
            pauseTicks--
            return
        }

        if (event.isCancelled)
            return

        if ((packet is EntityVelocityUpdateS2CPacket && player.entityId == packet.id && packet.velocityY > 0 && (packet.velocityX != 0 || packet.velocityZ != 0))
            || (packet is ExplosionS2CPacket && (player.velocityY + packet.playerVelocityY) > 0.0
                && ((player.velocityX + packet.playerVelocityX) != 0.0 || (player.velocityZ + packet.playerVelocityZ) != 0.0))) {
            velocityTimer.reset()

            if (pauseOnExplosion && packet is ExplosionS2CPacket  && (player.velocityY + packet.playerVelocityY) > 0.0
                && ((player.velocityX + packet.playerVelocityX) != 0.0 || (player.velocityZ + packet.playerVelocityZ) != 0.0)) {
                pauseTicks = ticksToPause
            }

            when (mode.lowercase()) {
                "simple" -> handleVelocity(event)

                "aac", "reverse", "smoothreverse", "aaczero", "ghostblock", "intave" -> hasReceivedVelocity = true

                "jump" -> {
                    // TODO: Recode and make all velocity modes support velocity direction checks
                    var packetDirection = 0.0
                    when (packet) {
                        is EntityVelocityUpdateS2CPacket -> {
                            val velocityX = packet.velocityX.toDouble()
                            val velocityZ = packet.velocityZ.toDouble()

                            packetDirection = atan2(velocityX, velocityZ)
                        }

                        is ExplosionS2CPacket -> {
                            val velocityX = player.velocityX + packet.playerVelocityX
                            val velocityZ = player.velocityZ + packet.playerVelocityZ

                            packetDirection = atan2(velocityX, velocityZ)
                        }
                    }
                    val degreePlayer = getDirection()
                    val degreePacket = Math.floorMod(packetDirection.toDegrees().toInt(), 360).toDouble()
                    var angle = abs(degreePacket + degreePlayer)
                    val threshold = 120.0
                    angle = Math.floorMod(angle.toInt(), 360).toDouble()
                    val inRange = angle in 180 - threshold / 2..180 + threshold / 2
                    if (inRange)
                        hasReceivedVelocity = true
                }

                "glitch" -> {
                    if (!player.onGround)
                        return

                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "matrixreduce" -> {
                    if (packet is EntityVelocityUpdateS2CPacket) {
                        packet.velocityX = (packet.getVelocityX() * 0.33).toInt()
                        packet.velocityZ = (packet.getVelocityZ() * 0.33).toInt()

                        if (player.onGround) {
                            packet.velocityX = (packet.getVelocityX() * 0.86).toInt()
                            packet.velocityZ = (packet.getVelocityZ() * 0.86).toInt()
                        }
                    }
                }

                "grimc03" -> {
                    // Checks to prevent from getting flagged (BadPacketsE)
                    if (isMoving) {
                        hasReceivedVelocity = true
                        event.cancelEvent()
                    }
                }

                "hypixelair" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "vulcan" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "s32packet" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }
            }
        }

        if (mode == "Vulcan" && packet is ConfirmGuiActionC2SPacket) {

            // prevent for vulcan transaction timeout
            if (!hasReceivedVelocity)
                return

            event.cancelEvent()
            hasReceivedVelocity = false
        }

        if (mode == "S32Packet" && packet is ConfirmGuiActionS2CPacket) {

            if (!hasReceivedVelocity)
                return

            event.cancelEvent()
            hasReceivedVelocity = false
        }
    }

    /**
     * Tick Event (Abuse Timer Balance)
     */
    @EventTarget
    fun onTick(event: GameTickEvent) {
        val player = mc.player ?: return

        if (mode != "GrimC03")
            return

        // Timer Abuse (https://github.com/CCBlueX/LiquidBounce/issues/2519)
        if (timerTicks > 0 && mc.ticker.timerSpeed!! <= 1) {
            val timerSpeed = 0.8f + (0.2f * (20 - timerTicks) / 20)
            mc.ticker.timerSpeed = timerSpeed.coerceAtMost(1f)
            --timerTicks
        } else if (mc.ticker.timerSpeed!! <= 1) {
            mc.ticker.timerSpeed = 1f
        }

        if (hasReceivedVelocity) {
            val pos = BlockPos(player.x, player.y, player.z)

            if (checkAir(pos))
                hasReceivedVelocity = false
        }
    }

    /**
     * Delay Mode
     */
    @EventTarget
    fun onDelayPacket(event: PacketEvent) {
        val packet = event.packet

        if (event.isCancelled )
            return

        if (mode == "Delay") {
            if (packet is ConfirmGuiActionS2CPacket || packet is EntityVelocityUpdateS2CPacket) {

                event.cancelEvent()

                // Delaying packet like PingSpoof
                synchronized(packets) {
                    packets[packet] = System.currentTimeMillis()
                }
            }
            delayMode = true
        } else {
            delayMode = false
        }
    }

    /**
     * Reset on world change
     */
    @EventTarget
    fun onWorld(event: WorldEvent) {
        packets.clear()
    }

    @EventTarget
    fun onGameLoop(event: GameLoopEvent) {
        if (mode == "Delay")
            sendPacketsByOrder(false)
    }

    private fun sendPacketsByOrder(velocity: Boolean) {
        synchronized(packets) {
            packets.entries.removeAll { (packet, timestamp) ->
                if (velocity || timestamp <= (System.currentTimeMillis() - spoofDelay)) {
                    queuedPackets.add(packet)
                    true
                } else false
            }
        }
    }

    private fun reset() {
        sendPacketsByOrder(true)

        packets.clear()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val player = mc.player

        if (player == null || player.isTouchingWater || player.isTouchingLava || player.isInWeb())
            return

        when (mode.lowercase()) {
            "aacpush" -> {
                jump = true

                if (!player.horizontalCollision)
                    event.cancelEvent()
            }

            "aaczero" ->
                if (player.hurtTime > 0)
                    event.cancelEvent()
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val player = mc.player ?: return

        if (mode == "Jump" && hasReceivedVelocity) {
            if (!player.jumping && nextInt(endExclusive = 100) < chance && shouldJump() && player.isSprinting && player.onGround && player.hurtTime == 9) {
                player.tryJump()
                limitUntilJump = 0
            }
            hasReceivedVelocity = false
            return
        }

        when (jumpCooldownMode.lowercase()) {
            "ticks" -> limitUntilJump++
            "receivedhits" -> if (player.hurtTime == 9) limitUntilJump++
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        val player = mc.player ?: return

        if (mode == "GhostBlock") {
            if (hasReceivedVelocity) {
                if (player.hurtTime in minHurtTime.get()..maxHurtTime.get()) {
                    // Check if there is air exactly 1 level above the player's Y position
                    if (event.block is AirBlock && event.y == mc.player.y.toInt() + 1) {
                        event.boundingBox = Box(event.x.toDouble(),
                            event.y.toDouble(),
                            event.z.toDouble(),
                            event.x + 1.0,
                            event.y + 1.0,
                            event.z + 1.0
                        )
                    }
                } else if (player.hurtTime == 0) {
                    hasReceivedVelocity = false
                }
            }
        }
    }

    private fun shouldJump() = when (jumpCooldownMode.lowercase()) {
        "ticks" -> limitUntilJump >= ticksUntilJump
        "receivedhits" -> limitUntilJump >= hitsUntilJump
        else -> false
    }

    private fun handleVelocity(event: PacketEvent) {
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket) {
            // Always cancel event and handle motion from here
            event.cancelEvent()

            if (horizontal == 0f && vertical == 0f)
                return

            // Don't modify player's motionXZ when horizontal value is 0
            if (horizontal != 0f) {
                var velocityX = packet.realMotionX
                var velocityZ = packet.realMotionZ

                if (limitMaxMotionValue.get()) {
                    val distXZ = sqrt(velocityX * velocityX + velocityZ * velocityZ)

                    if (distXZ > maxXZMotion) {
                        val ratioXZ = maxXZMotion / distXZ

                        velocityX *= ratioXZ
                        velocityZ *= ratioXZ
                    }
                }

                mc.player.velocityX = velocityX * horizontal
                mc.player.velocityZ = velocityZ * horizontal
            }

            // Don't modify player's velocityY when vertical value is 0
            if (vertical != 0f) {
                var velocityY = packet.realMotionY

                if (limitMaxMotionValue.get())
                    velocityY = velocityY.coerceAtMost(maxYMotion + 0.00075)

                mc.player.velocityY = velocityY * vertical
            }
        } else if (packet is ExplosionS2CPacket) {
            // Don't cancel explosions, modify them, they could change blocks in the world
            if (horizontal != 0f && vertical != 0f) {
                packet.playerVelocityX = 0f
                packet.playerVelocityY = 0f
                packet.playerVelocityZ = 0f

                return
            }

            // Unlike with EntityVelocityUpdateS2CPacket explosion packet motions get added to player motion, doesn't replace it
            // Velocity might behave a bit differently, especially LimitMaxMotion
            packet.playerVelocityX *= horizontal // velocityX
            packet.playerVelocityY *= vertical // velocityY
            packet.playerVelocityZ *= horizontal // velocityZ

            if (limitMaxMotionValue.get()) {
                val distXZ = sqrt(packet.playerVelocityX * packet.playerVelocityX + packet.playerVelocityZ * packet.playerVelocityZ)
                val distY = packet.playerVelocityY
                val maxYMotion = maxYMotion + 0.00075f

                if (distXZ > maxXZMotion) {
                    val ratioXZ = maxXZMotion / distXZ

                    packet.playerVelocityX *= ratioXZ
                    packet.playerVelocityZ *= ratioXZ
                }

                if (distY > maxYMotion) {
                    packet.playerVelocityY *= maxYMotion / distY
                }
            }
        }
    }

    private fun getAllEntities(): List<Entity> {
        return mc.world.entities
            .filter { isSelected(it, true) }
            .toList()
    }

    private fun getNearestEntityInRange(): Entity? {
        val player = mc.player

        val entitiesInRange = getAllEntities()
            .filter {
                val distance = player.getDistanceToEntityBox(it)
                (distance <= range)
            }

        return entitiesInRange.minByOrNull { player.getDistanceToEntityBox(it) }
    }
}
