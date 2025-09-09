package net.ccbluex.liquidbounce.features.module.modules.combat.tpbow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.*

@Suppress("TooManyFunctions")
object ModuleBowAura : ClientModule("TPBowAura", Category.COMBAT, disableOnQuit = true) {

    private val mode by enumChoice("Mode", Mode.VELOCITY_1_9, Mode.entries.toSet())
    private val packetMode by enumChoice("PacketMode", PacketMode.C06, PacketMode.entries.toSet())
    private val delay by intRange("Delay", 200..5000, 10..10000, "ms")
    private val maxTargets by int("MaxTargets", 1, 1..150)
    private val moveDistance by float("MoveDistance", 2f, 0.1f..20f, "m")
    private val antiLag by boolean("AntiLag", false)
    private val antiLagHighVersion by boolean("AntiLag-1.9+", false)
    private val throughWalls by boolean("IgnoreWalls", false)

    internal val targetTracker = tree(object : TargetTracker(
        TargetPriority.DISTANCE,
        floatRange("Range", 3.0f..6f, 1f..50f)
    ) {
        override fun validate(entity: LivingEntity): Boolean {
            return entity.isAlive &&
                entity != player &&
                super.validate(entity)
        }
    })



    init {
        tree(targetTracker)
    }

    private val clickTimer = object {
        private var lastMS = System.currentTimeMillis()

        fun reset() {
            lastMS = System.currentTimeMillis()
        }

        fun hasTimePassed(time: Long): Boolean {
            return System.currentTimeMillis() - lastMS >= time
        }

    }
    private var currentPath: List<Vec3d> = emptyList()

    private enum class Mode(override val choiceName: String) : NamedChoice {
        VELOCITY_1_8("1.8"),
        VELOCITY_1_9("1.9")
    }

    private enum class PacketMode(override val choiceName: String) : NamedChoice {
        C04("C04"),
        C06("C06")
    }


    @Suppress("unused")
    private val repeatable = tickHandler {
        val bowItem = player.inventory.main.firstOrNull { it?.item == Items.BOW } ?: return@tickHandler
        if (!clickTimer.hasTimePassed(delay.random().toLong())) return@tickHandler
        val targets = targetTracker.targets().take(maxTargets)
        if (targets.isEmpty()) return@tickHandler

        targets.forEach { target ->
            when (mode) {
                Mode.VELOCITY_1_8 -> handleVelocity18(target)
                Mode.VELOCITY_1_9 -> handleVelocity19(target)
            }
        }
        clickTimer.reset()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (antiLag && packet is PlayerPositionLookS2CPacket) {
            event.cancelEvent()


            val playerPosition = packet.change

            val diffX = player.x - playerPosition.position.x
            val diffY = player.y - playerPosition.position.y
            val diffZ = player.z - playerPosition.position.z
            val distance = sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ)

            network.sendPacket(
                PlayerMoveC2SPacket.Full(
                    player.x, player.y, player.z,
                    player.yaw, player.pitch,
                    player.isOnGround,
                    false
                )
            )

            if (antiLagHighVersion) {

                repeat((distance / 10 + 1).roundToInt()) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            player.x, player.y, player.z,
                            player.yaw, player.pitch,
                            player.isOnGround,
                            false
                        )
                    )
                }
            }
        }
    }
    private fun findPath(start: Vec3d, end: Vec3d, maxDistance: Double): List<Vec3d> {
        val distance = start.distanceTo(end)
        val steps = (distance / maxDistance).toInt().coerceAtLeast(1)
        return (1..steps).map { step ->
            val ratio = step.toDouble() / steps
            Vec3d(start.x + (end.x - start.x) * ratio,
                start.y + (end.y - start.y) * ratio,
                start.z + (end.z - start.z) * ratio)
        }.plus(end)
    }

    private suspend fun travelPath(path: List<Vec3d>, sendPacket: (Vec3d) -> Unit) {
        for (point in path) {
            sendPacket(point)
            delay(10)
        }
    }

    private fun findPathWithObstacles(start: Vec3d, target: LivingEntity, maxDistance: Double): List<Vec3d> {

        val startPos = BlockPos(start.x.toInt(), start.y.toInt(), start.z.toInt())

        val targetPos = Vec3d(target.x, target.boundingBox.minY + target.height / 2, target.z)

        val path = mutableListOf<Vec3d>()
        var currentPos = startPos
        val maxSteps = 1000

        while (currentPos.getSquaredDistance(targetPos) > maxDistance * maxDistance && path.size < maxSteps) {
            val direction = Vec3d(
                (targetPos.x - currentPos.x),
                (targetPos.y - currentPos.y),
                (targetPos.z - currentPos.z)
            ).normalize().multiply(maxDistance)

            val nextPos = currentPos.add(direction.x.toInt(), direction.y.toInt(), direction.z.toInt())

            if (isPassable(nextPos)) {
                path.add(Vec3d(nextPos.x + 0.5, nextPos.y.toDouble(), nextPos.z + 0.5))
                currentPos = nextPos
            } else {
                val alternatePos = findAlternatePath(currentPos, targetPos)
                if (alternatePos == currentPos) break
                path.add(Vec3d(alternatePos.x + 0.5, alternatePos.y.toDouble(), alternatePos.z + 0.5))
                currentPos = alternatePos
            }
        }
        path.add(target.boundingBox.center)
        return path
    }

    private fun findAlternatePath(current: BlockPos, target: Vec3d): BlockPos {
        val directions = listOf(
            Vec3i(1, 0, 0), Vec3i(-1, 0, 0), Vec3i(0, 0, 1), Vec3i(0, 0, -1),
            Vec3i(0, 1, 0), Vec3i(0, -1, 0)
        )


        val sortedDirections = directions.sortedBy { dir ->
            val newPos = current.add(dir)
            newPos.getSquaredDistance(target)
        }

        for (dir in sortedDirections) {
            val newPos = current.add(dir)
            if (isPassable(newPos)) return newPos
        }

        return current
    }
    private suspend fun handleVelocity18(target: LivingEntity) {
        currentPath = withContext(Dispatchers.Default) { findPath(player.pos, target.pos, moveDistance.toDouble()) }
        if (currentPath.isEmpty()) return

        val maxSendsPerAttack = 3
        val sendStep = max(1, currentPath.size / maxSendsPerAttack)

        currentPath.forEachIndexed { idx, vec ->
            if (idx % sendStep != 0 && idx != currentPath.lastIndex) return@forEachIndexed
            sendPositionPacket(vec)
        }
        sendPositionPacket(currentPath.last())

        doAttack(target, currentPath.last().x, currentPath.last().z)
        sendPositionPacket(player.pos)
    }


    private fun isPassable(pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block

        return state.isAir ||
            block.defaultState.isReplaceable ||
            state.fluidState.isStill
    }

    private fun handleVelocity19(target: LivingEntity) {
        val diff = player.distanceTo(target)
        val times = (diff / moveDistance).toInt()
        repeat(times) { sendPositionPacket(Vec3d(target.x, target.y, target.z)) }
        doAttack(target, target.x, target.z)
        repeat(times) { sendPositionPacket(player.pos) }
    }

    private fun sendPositionPacket(vec: Vec3d) {
        when (packetMode) {
            PacketMode.C04 -> network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z,
                    true, false))
            PacketMode.C06 -> network.sendPacket(
                PlayerMoveC2SPacket.Full(vec.x, vec.y, vec.z,
                    player.yaw, player.pitch, true, false))
        }
    }

    private fun doAttack(target: Entity, x: Double, z: Double) {
        // 1. Switch to bow slot if not already holding it
        val bowSlot = (0..8).firstOrNull { player.inventory.getStack(it).item == Items.BOW } ?: return
        if (player.inventory.selectedSlot != bowSlot) {
            SilentHotbar.selectSlotSilently(this, HotbarItemSlot(bowSlot), 0)
        }

        // 2. Start using the bow (equivalent to C08PacketPlayerBlockPlacement)
        network.sendPacket(
            PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,
                player.yaw,
                player.pitch
            )
        )

        // 3. Aim at target
        repeat(20) {
            val (yaw, pitch) = calculateBowAngles(target, x, z)
            network.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, player.isOnGround, player.horizontalCollision
                )
            )
        }

        // 4. Release the bow (equivalent to C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                Direction.DOWN,
                0
            )
        )

        // 5. Reset position packets
        repeat(2) {
            network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(
                    player.x, player.y, player.z,
                    player.isOnGround,
                    false
                )
            )
        }

        // 6. Reset rotation
        network.sendPacket(
            PlayerMoveC2SPacket.LookAndOnGround(
                player.yaw,
                player.pitch,
                player.isOnGround,
                false
            )
        )
    }

    private fun calculateBowAngles(entity: Entity, x: Double, z: Double): Pair<Float, Float> {
        val posX = entity.x - x
        val posY = entity.boundingBox.minY + entity.eyeY - 0.15 -
            player.boundingBox.minY - player.eyeY
        val posZ = entity.z - z
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        val yaw = (atan2(posZ, posX) * 180.0 / PI - 90.0).toFloat()
        val pitch = (-Math.toDegrees(
            atan(
                (1 - sqrt(abs(1 - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * 1))) /
                    (0.006f * posSqrt)
                    )
            )
        ).toFloat())

        return Pair(yaw, pitch.coerceIn(-90f, 90f))
    }

    @Suppress("unused")
    private suspend fun handleMovement(target: LivingEntity) {
        when (mode) {
            Mode.VELOCITY_1_8 -> handleVelocity18Movement(target)
            Mode.VELOCITY_1_9 -> handleVelocity19Movement(target)
        }
        clickTimer.reset()
    }

    private suspend fun handleVelocity18Movement(target: LivingEntity) {
        currentPath = if (throughWalls) {
            findPath(player.pos, target.pos, moveDistance.toDouble())
        } else {
            findPathWithObstacles(player.pos, target, moveDistance.toDouble())
        }

        if (currentPath.isEmpty()) return

        travelToTarget(currentPath)
        doAttack(target, currentPath.last().x, currentPath.last().z)
        travelBack(currentPath.reversed())
    }

    private fun handleVelocity19Movement(target: LivingEntity) {
        val diff = player.distanceTo(target)
        val times = (diff / moveDistance).toInt()

        repeatMoveToTarget(times, target)
        currentPath = listOf(Vec3d(target.x, target.y, target.z))
        doAttack(target, target.x, target.z)
        repeatMoveBack(times)
    }

    private suspend fun travelToTarget(path: List<Vec3d>) {
        travelPath(path) { vec -> sendMovementPacket(vec) }
    }

    private suspend fun travelBack(path: List<Vec3d>) {
        travelPath(path) { vec -> sendMovementPacket(vec) }
    }

    private fun repeatMoveToTarget(times: Int, target: LivingEntity) {
        repeat(times) { sendMovementPacket(Vec3d(target.x, target.y, target.z)) }
    }

    private fun repeatMoveBack(times: Int) {
        repeat(times) { sendMovementPacket(Vec3d(player.x, player.y, player.z)) }
    }

    private fun sendMovementPacket(vec: Vec3d) {
        when (packetMode) {
            PacketMode.C04 -> network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(
                    vec.x, vec.y, vec.z, true, false
                )
            )

            PacketMode.C06 -> network.sendPacket(
                PlayerMoveC2SPacket.Full(
                    vec.x, vec.y, vec.z,
                    player.yaw, player.pitch,
                    true, false
                )
            )
        }
    }
}
