/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

val globalEnemyConfigurable = EnemyConfigurable()

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as enemy
 *
 * TODO: Might make enemy configurable per module (like use global target settings or not)
 *   This is the reason why enemy configurable is not a object
 */
class EnemyConfigurable : Configurable("enemies") {

    // Players should be considered as a enemy
    val players by boolean("Players", true)

    // Hostile mobs (like skeletons and zombies) should be considered as a enemy
    val mobs by boolean("Mobs", true)

    // Animals (like cows, pigs and so on) should be considered as a enemy
    val animals by boolean("Animals", false)

    // Invisible entities should be also considered as a enemy
    var invisible by boolean("Invisible", true)

    // Dead entities should be also considered as a enemy to bypass modern anti cheat techniques
    var dead by boolean("Dead", false)

    // Friends (client friends - other players) should be also considered as enemy
    val friends by boolean("Friends", false)

    // Should bots be blocked to bypass anti cheat techniques
    val antibot = tree(AntiBotConfigurable())

    class AntiBotConfigurable : Configurable("AntiBot") {

        /**
         * Should always be enabled. A good antibot should never detect a real player as a bot (on default settings).
         */
        val enabled by boolean("Enabled", true)

        /**
         * Check if player might be a bot
         */
        fun isBot(player: ClientPlayerEntity): Boolean {
            if (!enabled)
                return false


            return false
        }

    }

    init {
        ConfigSystem.root(this)
    }

    /**
     * Check if entity is considered a enemy
     */
    fun isEnemy(enemy: Entity, attackable: Boolean = false): Boolean {
        // Check if enemy is living and not dead (or ignore being dead)
        if (enemy is LivingEntity && (dead || enemy.isAlive)) {
            // Check if enemy is invisible (or ignore being invisible)
            if (invisible || !enemy.isInvisible) {
                // Check if enemy is a player and should be considered as enemy
                if (enemy is PlayerEntity && players && enemy != mc.player) {
                    // TODO: Check friends because there is no friend system right now

                    // Check if player might be a bot
                    if (enemy is ClientPlayerEntity && antibot.isBot(enemy)) {
                        return false
                    }

                    return true
                } else if (enemy is PassiveEntity && animals) {
                    return true
                } else if (enemy is MobEntity && mobs) {
                    return true
                }
            }
        }

        return false
    }

}

/**
 * Configurable to configure the dynamic rotation engine
 */
class RotationsConfigurable : Configurable("rotations") {
    val turnSpeed by curve("TurnSpeed", arrayOf(4f, 7f, 10f, 3f, 2f, 0.7f))
    val predict by boolean("Predict", true)
}

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("inventoryConstraints") {
    internal var delay by intRange("Delay", 2..4, 0..20)
    internal val invOpen by boolean("InvOpen", false)
    internal val simulateInventory by boolean("SimulateInventory", true)
    internal val noMove by boolean("NoMove", false)
}

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetTracker(defaultPriority: PriorityEnum = PriorityEnum.HEALTH) : Configurable("target"), Iterable<Entity> {

    var possibleTargets: Array<Entity> = emptyArray()
    var lockedOnTarget: Entity? = null

    val priority by enumChoice("Priority", PriorityEnum.HEALTH, PriorityEnum.values())
    val lockOnTarget by boolean("LockOnTarget", false)
    val sortOut by boolean("SortOut", true)
    val delayableSwitch by intRange("DelayableSwitch", 10..20, 0..40)

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun update(enemyConf: EnemyConfigurable = globalEnemyConfigurable) {
        possibleTargets = emptyArray()

        val entities = (mc.world ?: return).entities
            .filter { it.shouldBeAttacked(enemyConf) }
            .sortedBy { mc.player!!.squaredBoxedDistanceTo(it) } // Sort by distance

        val eyePos = mc.player!!.eyesPos

        // default

        when (priority) {
            PriorityEnum.HEALTH -> entities.sortedBy { if (it is LivingEntity) it.health else 0f } // Sort by health
            PriorityEnum.DIRECTION -> entities.sortedBy {
                RotationManager.rotationDifference(
                    RotationManager.makeRotation(
                        eyePos,
                        it.boundingBox.center
                    )
                )
            } // Sort by FOV
            PriorityEnum.AGE -> entities.sortedBy { -it.age } // Sort by existence
        }

        possibleTargets = entities.toTypedArray()
    }

    fun cleanup() {
        possibleTargets = emptyArray()
        lockedOnTarget = null
    }

    fun lock(entity: Entity) {
        lockedOnTarget = entity
    }

    override fun iterator() = possibleTargets.iterator()

}

enum class PriorityEnum(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    AGE("Age")
}

/**
 * A rotation manager to
 */
object RotationManager : Listenable {

    var targetRotation: Rotation? = null
    var serverRotation: Rotation? = null

    // rotation engine
    private var activeConfigurable: RotationsConfigurable? = null
    var currRotation: Rotation? = null

    private var ticksUntilReset = -1

    // useful for something like autopot
    var deactivateManipulation = false

    private var x = Random.nextDouble()
    private var y = Random.nextDouble()
    private var z = Random.nextDouble()

    fun raytraceBlock(
        eyes: Vec3d,
        pos: BlockPos,
        state: BlockState,
        throughWalls: Boolean,
        range: Double
    ): VecRotation? {
        val offset = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val shape = state.getVisualShape(mc.world, pos, ShapeContext.of(mc.player))

        for (box in shape.boundingBoxes.sortedBy { -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ) }) {
            return raytraceBox(eyes, box.offset(offset), throughWalls, range, pos) ?: continue
        }

        return null
    }

    /**
     * Find the best spot of a box to aim at.
     */
    fun raytraceBox(
        eyes: Vec3d,
        box: Box,
        throughWalls: Boolean,
        range: Double,
        expectedTarget: BlockPos? = null
    ): VecRotation? {
        val preferredSpot = Vec3d(
            box.minX + (box.maxX - box.minX) * x * 0.8, box.minY + (box.maxY - box.minY) * y * 0.8,
            box.minZ + (box.maxZ - box.minZ) * z * 0.8
        )
        val preferredRotation = makeRotation(eyes, preferredSpot)

        var bestRotation: VecRotation? = null

        for (x in 0.1..0.9 step 0.1) {
            for (y in 0.1..0.9 step 0.1) {
                for (z in 0.1..0.9 step 0.1) {
                    val vec3 = Vec3d(
                        box.minX + (box.maxX - box.minX) * x, box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z
                    )

                    // skip because of out of range
                    if (eyes.distanceTo(vec3) > range)
                        continue

                    // todo: prefer visible spots even when through walls is turned on
                    if (if (expectedTarget != null) facingBlock(eyes, vec3, expectedTarget) else isVisible(
                            eyes,
                            vec3
                        ) || throughWalls
                    ) {
                        val rotation = makeRotation(vec3, eyes)

                        // Calculate next spot to preferred spot
                        if (bestRotation == null || rotationDifference(
                                rotation,
                                preferredRotation
                            ) < rotationDifference(bestRotation.rotation, preferredRotation)
                        ) {
                            bestRotation = VecRotation(rotation, vec3)
                        }
                    }
                }
            }
        }

        return bestRotation
    }

    fun outborder(box: Box): Vec3d? {
        return null
    }

    fun aimAt(vec: Vec3d, eyes: Vec3d, ticks: Int = 5, configurable: RotationsConfigurable) =
        aimAt(makeRotation(vec, eyes), ticks, configurable)

    fun aimAt(rotation: Rotation, ticks: Int = 5, configurable: RotationsConfigurable) {
        activeConfigurable = configurable
        rotation.fixedSensitivity() // todo: move directly into rotation
        targetRotation = rotation
        ticksUntilReset = ticks
    }

    fun makeRotation(vec: Vec3d, eyes: Vec3d): Rotation {
        val diffX = vec.x - eyes.x
        val diffY = vec.y - eyes.y
        val diffZ = vec.z - eyes.z

        return Rotation(
            MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
        )
    }

    /**
     * Update current rotation to new rotation step
     */
    private fun update() {
        // Update how long the rotation should last
        if (ticksUntilReset > 0) {
            ticksUntilReset--

            if (ticksUntilReset == 0) {
                targetRotation = null
            }
        }

        if (Random.nextDouble() > 0.6) {
            x = Random.nextDouble()
        }
        if (Random.nextDouble() > 0.6) {
            y = Random.nextDouble()
        }
        if (Random.nextDouble() > 0.6) {
            z = Random.nextDouble()
        }

        // todo: update curr rotation to target rotation and check for active configurable
        currRotation = targetRotation
    }

    fun needsUpdate(lastYaw: Float, lastPitch: Float): Boolean {
        // Update current rotation
        update()

        // Check if something changed
        val (currYaw, currPitch) = currRotation ?: return false

        return lastYaw != currYaw || lastPitch != currPitch
    }

    /**
     * Allows you to check if a point is behind a wall
     */
    private fun isVisible(eyes: Vec3d, vec3: Vec3d) = mc.world?.raycast(
        RaycastContext(
            eyes,
            vec3,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        )
    )?.type == HitResult.Type.MISS

    /**
     * Allows you to check if a point is behind a wall
     */
    private fun facingBlock(eyes: Vec3d, vec3: Vec3d, blockPos: BlockPos): Boolean {
        val searchedPos = mc.world?.raycast(
            RaycastContext(
                eyes,
                vec3,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )
        ) ?: return false

        if (searchedPos.type != HitResult.Type.BLOCK)
            return false

        return searchedPos.blockPos == blockPos
    }

    /**
     * Allows you to check if your enemy is behind a wall
     */
    fun facingEnemy(enemy: Entity, range: Double): Boolean {
        return raytraceEntity(range, serverRotation ?: return false) { it == enemy } != null
    }

    /**
     * Calculate difference between the server rotation and your rotation
     */
    fun rotationDifference(rotation: Rotation): Double {
        return if (serverRotation == null) 0.0 else rotationDifference(rotation, serverRotation!!)
    }

    /**
     * Calculate difference between two rotations
     */
    fun rotationDifference(a: Rotation, b: Rotation) =
        hypot(angleDifference(a.yaw, b.yaw).toDouble(), (a.pitch - b.pitch).toDouble())

    /**
     * Limit your rotations
     */
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float): Rotation {
        val yawDifference = angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = angleDifference(targetRotation.pitch, currentRotation.pitch)

        return Rotation(
            currentRotation.yaw + if (yawDifference > turnSpeed) turnSpeed else yawDifference.coerceAtLeast(-turnSpeed),
            currentRotation.pitch + if (pitchDifference > turnSpeed) turnSpeed else pitchDifference.coerceAtLeast(-turnSpeed)
        )
    }

    /**
     * Calculate difference between two angle points
     */
    private fun angleDifference(a: Float, b: Float) = ((a - b) % 360f + 540f) % 360f - 180f

    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket) {
            if (!deactivateManipulation) {
                currRotation?.run {
                    val (serverYaw, serverPitch) = serverRotation ?: Rotation(0f, 0f)

                    if (yaw != serverYaw || pitch != serverPitch) {
                        packet.yaw = yaw
                        packet.pitch = pitch
                        packet.changeLook = true
                    }
                }
            }

            // Update current rotation
            if (packet.changeLook) {
                serverRotation = Rotation(packet.yaw, packet.pitch)
            }
        }
    }

}

data class Rotation(var yaw: Float, var pitch: Float) {

    val rotationVec: Vec3d
        get() {
            val yawCos = MathHelper.cos(-yaw * 0.017453292f)
            val yawSin = MathHelper.sin(-yaw * 0.017453292f)
            val pitchCos = MathHelper.cos(pitch * 0.017453292f)
            val pitchSin = MathHelper.sin(pitch * 0.017453292f)
            return Vec3d((yawSin * pitchCos).toDouble(), (-pitchSin).toDouble(), (yawCos * pitchCos).toDouble())
        }

    /**
     * Set rotations to [player]
     */
    fun toPlayer(player: PlayerEntity) {
        if (yaw.isNaN() || pitch.isNaN())
            return

        player.yaw = yaw
        player.pitch = pitch
    }

    /**
     * Patch GCD aiming
     */
    fun fixedSensitivity(sensitivity: Float = mc.options.mouseSensitivity.toFloat()) {
        val f = sensitivity * 0.6F + 0.2F
        val gcd = f * f * f * 1.2F

        // get previous rotation
        val rotation = RotationManager.serverRotation ?: return

        // fix yaw
        var deltaYaw = yaw - rotation.yaw
        deltaYaw -= deltaYaw % gcd
        yaw = rotation.yaw + deltaYaw

        // fix pitch
        var deltaPitch = pitch - rotation.pitch
        deltaPitch -= deltaPitch % gcd
        pitch = rotation.pitch + deltaPitch
    }

}

data class VecRotation(val rotation: Rotation, val vec: Vec3d)

// Extensions

fun Entity.shouldBeShown(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(this)

fun Entity.shouldBeAttacked(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(
    this,
    true
)

/**
 * Find the best emeny in current world in a specific range.
 */
fun ClientWorld.findEnemy(
    range: Float,
    player: Entity = mc.player!!,
    enemyConf: EnemyConfigurable = globalEnemyConfigurable
) = entities.filter { enemyConf.isEnemy(it, true) }
    .map { Pair(it, it.boxedDistanceTo(player)) }
    .filter { (_, distance) -> distance <= range }
    .minByOrNull { (_, distance) -> distance }

fun raytraceEntity(range: Double, rotation: Rotation, filter: (Entity) -> Boolean): Entity? {
    val entity: Entity = mc.cameraEntity ?: return null

    val cameraVec = entity.getCameraPosVec(1f)
    val rotationVec = rotation.rotationVec

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = entity.boundingBox.stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0)

    val entityHitResult = ProjectileUtil.raycast(
        entity,
        cameraVec,
        vec3d3,
        box,
        { !it.isSpectator && it.collides() && filter(it) },
        range * range
    )

    return entityHitResult?.entity
}
