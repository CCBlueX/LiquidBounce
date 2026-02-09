/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.common.ShapeFlag
import net.ccbluex.liquidbounce.interfaces.ClientInputAddition
import net.ccbluex.liquidbounce.interfaces.LocalPlayerAddition
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_UP
import net.ccbluex.liquidbounce.utils.block.isBlastResistant
import net.ccbluex.liquidbounce.utils.block.raycast
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.findEdgeCollision
import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket
import net.minecraft.tags.DamageTypeTags
import net.minecraft.util.Mth
import net.minecraft.world.Difficulty
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.player.Input
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.component.UseEffects
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerExplosion
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.EntityCollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.scores.DisplaySlot
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// Copied from 1.21.4
val Entity.isInsideWaterOrBubbleColumn: Boolean
    get() = this.isInWater || this.inBlockState.`is`(Blocks.BUBBLE_COLUMN)

inline var ClientInput.movementForward: Float
    get() = moveVector.y
    set(value) {
        (this as ClientInputAddition).`liquid_bounce$setMovementInput`(moveVector.copy(y = value))
    }

inline var ClientInput.movementSideways: Float
    get() = moveVector.x
    set(value) {
        (this as ClientInputAddition).`liquid_bounce$setMovementInput`(moveVector.copy(x = value))
    }

val LivingEntity.handItems: Array<ItemStack>
    get() = arrayOf(mainHandItem, offhandItem)

val LivingEntity.armorItems: Array<ItemStack>
    get() = arrayOf(
        getItemBySlot(EquipmentSlot.FEET),
        getItemBySlot(EquipmentSlot.LEGS),
        getItemBySlot(EquipmentSlot.CHEST),
        getItemBySlot(EquipmentSlot.HEAD),
    )

fun LivingEntity.blockedByShield(source: DamageSource): Boolean {
    val entity = source.directEntity
    var bl = false
    if (entity is AbstractArrow) {
        if (entity.pierceLevel > 0.toByte()) {
            bl = true
        }
    }

    val itemStack = itemBlockingWith
    if (!source.`is`(DamageTypeTags.BYPASSES_SHIELD) && itemStack?.item is ShieldItem && !bl) {
        val vec3d = source.sourcePosition
        if (vec3d != null) {
            val vec3d2 = calculateViewVector(0f, yHeadRot)
            val vec3d3 = vec3d.vectorTo(position()).copy(y = 0.0).normalize()
            return vec3d3.dot(vec3d2) < 0.0
        }
    }

    return false
}

// Copied from 1.21.4 END

val Entity.netherPosition: Vec3
    get() = if (this.level().dimension() == Level.NETHER) {
        Vec3(x, y, z)
    } else {
        Vec3(x / 8.0, y, z / 8.0)
    }

val LocalPlayer.moving
    get() = input.movementForward != 0.0f || input.movementSideways != 0.0f

val ClientInput.untransformed: Input
    get() = (this as ClientInputAddition).`liquid_bounce$getUntransformed`()

val ClientInput.initial: Input
    get() = (this as ClientInputAddition).`liquid_bounce$getInitial`()

val Player.ping: Int
    get() = mc.connection?.getPlayerInfo(uuid)?.latency ?: 0

val LocalPlayer.airTicks: Int
    get() = (this as LocalPlayerAddition).`liquid_bounce$getAirTicks`()

val LocalPlayer.onGroundTicks: Int
    get() = (this as LocalPlayerAddition).`liquid_bounce$getOnGroundTicks`()

val LocalPlayer.direction: Float
    get() = getMovementDirectionOfInput(DirectionalInput(input))

/**
 * Check if the attack speed is below 1 tick. If so, we have a cooldown.
 */
val LocalPlayer.hasCooldown: Boolean
    get() = !isOlderThanOrEqual1_8 && this.getAttributeValue(Attributes.ATTACK_SPEED) < 20.0

fun LocalPlayer.getMovementDirectionOfInput(input: DirectionalInput): Float {
    return getMovementDirectionOfInput(this.yRot, input)
}

val Player.isBlockAction: Boolean
    get() = isUsingItem && useItem.has(DataComponents.BLOCKS_ATTACKS)

inline fun LocalPlayer.setDeltaMovement(block: (Vec3) -> Vec3) {
    this.deltaMovement = block(this.deltaMovement)
}

/**
 * @see LocalPlayer.isSlowDueToUsingItem
 */
val Player.isSlowDueToUsingItem: Boolean
    get() = isUsingItem && !(useItem[DataComponents.USE_EFFECTS] ?: UseEffects.DEFAULT).canSprint

fun Entity.lastRenderPos() = Vec3(this.xOld, this.yOld, this.zOld)

fun Player.wouldBeCloseToFallOff(position: Vec3): Boolean {
    val hitbox =
        this.dimensions
            .makeBoundingBox(position)
            .inflate(-0.05, 0.0, -0.05)
            .move(0.0, this.fallDistance - this.maxUpStep(), 0.0)

    return this.level().noCollision(this, hitbox)
}

fun LocalPlayer.isCloseToEdge(
    directionalInput: DirectionalInput,
    distance: Double = 0.1,
    pos: Vec3 = this.position(),
): Boolean {
    val alpha = (getMovementDirectionOfInput(this.yRot, directionalInput) + 90.0F).toRadians()
    val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
    simulatedInput.set(
        jump = false,
        sneak = false
    )

    val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
        simulatedInput
    )

    simulatedPlayer.pos = pos
    simulatedPlayer.tick()

    val nextVelocity = simulatedPlayer.deltaMovement
    val direction = if (nextVelocity.horizontalDistanceSqr() > 0.003 * 0.003) {
        nextVelocity.multiply(1.0, 0.0, 1.0).normalize()
    } else {
        Vec3(cos(alpha).toDouble(), 0.0, sin(alpha).toDouble())
    }

    val from = pos.add(0.0, -0.1, 0.0)
    val to = from + direction.scale(distance)

    if (findEdgeCollision(from, to) != null) {
        return true
    }

    val playerPosInTwoTicks = simulatedPlayer.pos.add(nextVelocity.multiply(1.0, 0.0, 1.0))

    return wouldBeCloseToFallOff(pos) || wouldBeCloseToFallOff(playerPosInTwoTicks)
}

/**
 * Check if the player can step up by [height] blocks.
 *
 * TODO: Use Minecraft Step logic instead of this basic collision check.
 */
fun LocalPlayer.canStep(height: Double = 1.0): Boolean {
    if (!horizontalCollision || isDescending || !onGround()) {
        // If we are not colliding with anything, we are not meant to step
        return false
    }

    val box = this.boundingBox

    val angle = Math.toRadians(this.yRot.toDouble())
    val xOffset = -sin(angle) * 0.1
    val zOffset = cos(angle) * 0.1

    val offsetBox = box.move(xOffset, 0.0, zOffset)
    val stepBox = offsetBox.move(0.0, height, 0.0)

    return this.level().getBlockCollisions(this, stepBox).all { shape ->
        shape == Shapes.empty()
    } && this.level().getBlockCollisions(this, offsetBox).all { shape ->
        shape != Shapes.empty()
    }
}

fun getMovementDirectionOfInput(facingYaw: Float, input: DirectionalInput): Float {
    val forwards = input.forwards && !input.backwards
    val backwards = input.backwards && !input.forwards
    val left = input.left && !input.right
    val right = input.right && !input.left

    var actualYaw = facingYaw
    var forward = 1f

    if (backwards) {
        actualYaw += 180f
        forward = -0.5f
    } else if (forwards) {
        forward = 0.5f
    }

    if (left) {
        actualYaw -= 90f * forward
    }
    if (right) {
        actualYaw += 90f * forward
    }

    return actualYaw
}

inline val Entity.horizontalSpeed: Double
    get() = deltaMovement.horizontalDistance()

fun Vec3.withStrafe(
    speed: Double = horizontalDistance(),
    strength: Double = 1.0,
    input: DirectionalInput? = DirectionalInput(player.input),
    yaw: Float = player.getMovementDirectionOfInput(input ?: DirectionalInput(player.input)),
): Vec3 {
    if (input?.isMoving == false) {
        return Vec3(0.0, y, 0.0)
    }

    val prevX = x * (1.0 - strength)
    val prevZ = z * (1.0 - strength)
    val useSpeed = speed * strength

    val angle = Math.toRadians(yaw.toDouble())
    val x = (-sin(angle) * useSpeed) + prevX
    val z = (cos(angle) * useSpeed) + prevZ
    return Vec3(x, y, z)
}

val Entity.lastPos: Vec3
    get() = Vec3(xo, yo, zo)

val Entity.rotation: Rotation
    get() = Rotation(this.yRot, this.xRot, true)

val LocalPlayer.lastRotation: Rotation
    get() = Rotation(this.yRotLast, this.xRotLast, true)

val Entity.box: AABB
    get() = boundingBox.inflate(pickRadius.toDouble())

private val cameraPos: Vec3 get() = mc.gameRenderer.mainCamera.position()

fun Position.cameraDistanceSq() = cameraPos.distanceToSqr(x(), y(), z())

fun Position.cameraDistance() = sqrt(cameraDistanceSq())

fun Vec3i.cameraDistanceSq() = cameraPos.distanceToSqr(x.toDouble(), y.toDouble(), z.toDouble())

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    return sqrt(squaredBoxedDistanceTo(entity))
}

fun Entity.squaredBoxedDistanceTo(entity: Entity): Double {
    return this.squaredBoxedDistanceTo(entity.eyePosition)
}

fun Entity.squaredBoxedDistanceTo(otherPos: Vec3): Double {
    return this.box.squaredBoxedDistanceTo(otherPos)
}

fun Entity.squareBoxedDistanceTo(entity: Entity, offsetPos: Vec3): Double {
    return this.box.move(offsetPos - this.position()).squaredBoxedDistanceTo(entity.eyePosition)
}

fun AABB.squaredBoxedDistanceTo(otherPos: Vec3): Double {
    val pos = getNearestPoint(otherPos)

    return pos.distanceToSqr(otherPos)
}

fun Entity.interpolateCurrentPosition(tickDelta: Float): Vec3 {
    if (this.tickCount == 0) {
        return this.position()
    }

    return Vec3(
        this.xOld + (this.x - this.xOld) * tickDelta,
        this.yOld + (this.y - this.yOld) * tickDelta,
        this.zOld + (this.z - this.zOld) * tickDelta
    )
}

fun Entity.interpolateCurrentRotation(tickDelta: Float): Rotation {
    if (this.tickCount == 0) {
        return this.rotation
    }

    return Rotation(
        yRotO + (this.yRot - yRotO) * tickDelta,
        xRotO + (this.xRot - xRotO) * tickDelta,
    )
}

/**
 * Get the nearest point of a box. Very useful to calculate the distance of an enemy.
 */
fun AABB.getNearestPoint(from: Position): Vec3 {
    return Vec3(
        from.x().coerceIn(minX, maxX),
        from.y().coerceIn(minY, maxY),
        from.z().coerceIn(minZ, maxZ),
    )
}

fun getNearestPointOnSide(from: Vec3, box: AABB, side: Direction): Vec3 {
    val nearestPointInBlock = box.getNearestPoint(from)

    val x = nearestPointInBlock.x
    val y = nearestPointInBlock.y
    val z = nearestPointInBlock.z

    val nearestPointOnSide =
        when (side) {
            Direction.DOWN -> Vec3(x, box.minY, z)
            Direction.UP -> Vec3(x, box.maxY, z)
            Direction.NORTH -> Vec3(x, y, box.minZ)
            Direction.SOUTH -> Vec3(x, y, box.maxZ)
            Direction.WEST -> Vec3(box.maxX, y, z)
            Direction.EAST -> Vec3(box.minX, y, z)
        }

    return nearestPointOnSide

}

/**
 * Applies armor, enchantments, effects, etc. to the damage and returns the damage
 * that is actually applied. This function is so damn ugly that I turned off code smell analysis for it.
 */
@Suppress("detekt:all")
fun LivingEntity.getEffectiveDamage(source: DamageSource, damage: Float, ignoreShield: Boolean = false): Float {
    val level = this.level()

    if (this.isInvulnerableToBase(source)) {
        return 0.0F
    }

    // EDGE CASE!!! Might cause weird bugs
    if (this.isDeadOrDying) {
        return 0.0F
    }

    var amount = damage

    if (this is Player) {
        if (this.abilities.invulnerable && source.type().msgId != level.damageSources().fellOutOfWorld().type().msgId)
            return 0.0F

        if (source.scalesWithDifficulty()) {
            when (level.difficulty) {
                Difficulty.PEACEFUL -> {
                    amount = 0.0f
                }
                Difficulty.EASY -> {
                    amount = (amount / 2.0f + 1.0f).coerceAtMost(amount)
                }
                Difficulty.HARD -> {
                    amount = amount * 3.0f / 2.0f
                }
                else -> {}
            }
        }
    }

    if (amount == 0.0F)
        return 0.0F

    if (source == level.damageSources().onFire() && this.hasEffect(MobEffects.FIRE_RESISTANCE))
        return 0.0F

    if (!ignoreShield && blockedByShield(source))
        return 0.0F

    // Do we need to take the timeUntilRegen mechanic into account?

    amount = this.getDamageAfterArmorAbsorb(source, amount)
    amount = this.getDamageAfterMagicAbsorb(source, amount)

    return amount
}

fun LivingEntity.getExplosionDamageFromEntity(entity: Entity): Float {
    return when (entity) {
        is EndCrystal -> getDamageFromExplosion(entity.position(), 6f, 12f, 144f)
        is PrimedTnt -> getDamageFromExplosion(entity.position().add(0.0, 0.0625, 0.0), 4f, 8f, 64f)
        is MinecartTNT -> {
            val d = 5f
            getDamageFromExplosion(entity.position(), 4f + d * 1.5f)
        }

        is Creeper -> {
            val f = if (entity.isPowered) 2f else 1f
            getDamageFromExplosion(entity.position(), entity.explosionRadius * f)
        }

        else -> 0f
    }
}

/**
 * See [ExplosionDamageCalculator.getEntityDamageAmount].
 */
@Suppress("LongParameterList")
fun LivingEntity.getDamageFromExplosion(
    pos: Vec3,
    power: Float = 6f,
    explosionRange: Float = power * 2f, // allows setting precomputed values
    damageDistance: Float = explosionRange * explosionRange,
    exclude: Array<BlockPos>? = null,
    include: BlockPos? = null,
    maxBlastResistance: Float? = null,
    entityBoundingBox: AABB? = null
): Float {
    // no damage will be dealt if the entity is outside the explosion range or when the difficulty is peaceful
    if (this.distanceToSqr(pos) > damageDistance || this.level().difficulty == Difficulty.PEACEFUL) {
        return 0f
    }

    try {
        ShapeFlag.noShapeChange = true

        val useTweakedMethod = exclude != null ||
            maxBlastResistance != null ||
            include != null ||
            entityBoundingBox != null

        val exposure = if (useTweakedMethod) {
            getExposureToExplosion(pos, exclude, include, maxBlastResistance, entityBoundingBox)
        } else {
            ServerExplosion.getSeenPercent(pos, this)
        }

        val distanceDecay = 1.0 - (sqrt(this.distanceToSqr(pos)) / explosionRange.toDouble())
        val pre1 = exposure.toDouble() * distanceDecay

        val preprocessedDamage = (pre1 * pre1 + pre1) / 2.0 * 7.0 * explosionRange.toDouble() + 1.0
        if (preprocessedDamage == 0.0) {
            return 0f
        }

        return getEffectiveDamage(this.level().damageSources().explosion(null), preprocessedDamage.toFloat())
    } finally {
        ShapeFlag.noShapeChange = false
    }
}

/**
 * Basically [ServerExplosion.getSeenPercent] but this method allows us to exclude blocks using [exclude].
 */
@Suppress("NestedBlockDepth")
fun LivingEntity.getExposureToExplosion(
    source: Vec3,
    exclude: Array<BlockPos>?,
    include: BlockPos?,
    maxBlastResistance: Float?,
    entityBoundingBox: AABB?
): Float {
    val entityBoundingBox1 = entityBoundingBox ?: boundingBox
    val shapeContext = EntityCollisionContext(
        isDescending,
        false, // TODO: is this correct?
        entityBoundingBox1.minY,
        mainHandItem,
        false,
        this
    )

    val stepX = 1.0 / ((entityBoundingBox1.maxX - entityBoundingBox1.minX) * 2.0 + 1.0)
    val stepY = 1.0 / ((entityBoundingBox1.maxY - entityBoundingBox1.minY) * 2.0 + 1.0)
    val stepZ = 1.0 / ((entityBoundingBox1.maxZ - entityBoundingBox1.minZ) * 2.0 + 1.0)

    val offsetX = (1.0 - floor(1.0 / stepX) * stepX) / 2.0
    val offsetZ = (1.0 - floor(1.0 / stepZ) * stepZ) / 2.0

    if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) {
        return 0f
    }

    var hits = 0
    var totalRays = 0

    var currentXStep = 0.0
    while (currentXStep <= 1.0) {
        var currentYStep = 0.0
        while (currentYStep <= 1.0) {
            var currentZStep = 0.0
            while (currentZStep <= 1.0) {
                val sampleX = Mth.lerp(currentXStep, entityBoundingBox1.minX, entityBoundingBox1.maxX)
                val sampleY = Mth.lerp(currentYStep, entityBoundingBox1.minY, entityBoundingBox1.maxY)
                val sampleZ = Mth.lerp(currentZStep, entityBoundingBox1.minZ, entityBoundingBox1.maxZ)

                val samplePoint = Vec3(sampleX + offsetX, sampleY, sampleZ + offsetZ)
                val hitResult = this.level().raycast(
                    ClipContext(
                        samplePoint,
                        source,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        shapeContext
                    ),
                    exclude,
                    include,
                    maxBlastResistance
                )

                if (hitResult.type == HitResult.Type.MISS) {
                    hits++
                }

                totalRays++
                currentZStep += stepZ
            }
            currentYStep += stepY
        }
        currentXStep += stepX
    }

    return hits.toFloat() / totalRays.toFloat()
}

/**
 * Sometimes the server does not publish the actual entity health with its metadata.
 * This function incorporates other sources to get the actual value.
 *
 * Currently, uses the following sources:
 * 1. Scoreboard
 */
fun LivingEntity.getActualHealth(fromScoreboard: Boolean = true): Float {
    if (fromScoreboard) {
        val health = getHealthFromScoreboard()

        if (health != null) {
            return health
        }
    }


    return health
}

private val HEALTH_KEYWORDS = listOf("❤", "HP", "Health", "Здоровья", "Здоровье")

fun LivingEntity.hasHealthScoreboard(): Boolean {
    if (this == player) return false

    val objective = this.level().scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME) ?: return false
    val displayName = objective.displayName.string

    return HEALTH_KEYWORDS.any { displayName.contains(it) }
}

private fun LivingEntity.getHealthFromScoreboard(): Float? {
    if (!this.hasHealthScoreboard()) return null
    val objective = this.level().scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME)
    val score = objective?.scoreboard?.getPlayerScoreInfo(this, objective) ?: return null

    return score.value().toFloat()
}

fun Entity.getBoundingBoxAt(pos: Vec3): AABB {
    return boundingBox.move(pos - this.position())
}

/**
 * Check if the entity collides with anything below his bounding box.
 */
fun Entity.doesNotCollideBelow(until: Double = -64.0): Boolean {
    if (this.y < until || boundingBox.minY < until) {
        return true
    }

    val offsetBb = boundingBox.setMinY(until)
    return this.level().getBlockCollisions(this, offsetBb)
        .all(Shapes.empty()::equals)
}

/**
 * Check if the entity box collides with any block in the world at the given [pos].
 */
fun Entity.doesCollideAt(pos: Vec3 = player.position()): Boolean {
    return !this.level().getBlockCollisions(this, getBoundingBoxAt(pos)).all(Shapes.empty()::equals)
}

/**
 * Check if the entity is likely falling to the void based on the given position and bounding box.
 */
fun Entity.wouldFallIntoVoid(pos: Vec3, voidLevel: Double = -64.0, safetyExpand: Double = 0.0): Boolean {
    val offsetBb = boundingBox.move(pos - this.position())

    if (pos.y < voidLevel || offsetBb.minY < voidLevel) {
        return true
    }

    // If there is no collision to void threshold, we don't want to teleport down.
    val boundingBox = offsetBb
        // Set the minimum Y to the void threshold to check for collisions below the player
        .setMinY(voidLevel)
        // Expand the bounding box to check if there might be blocks to safely land on
        .inflate(safetyExpand, 0.0, safetyExpand)
    return this.level().getBlockCollisions(this, boundingBox)
        .all(Shapes.empty()::equals)
}


fun LocalPlayer.warp(pos: Vec3? = null, onGround: Boolean = false) {
    val vehicle = this.vehicle

    if (vehicle != null) {
        pos?.let(vehicle::setPos)
        connection.send(ServerboundMoveVehiclePacket.fromEntity(vehicle))
        return
    }

    if (pos != null) {
        connection.send(ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, onGround, horizontalCollision))
    } else {
        connection.send(ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision))
    }
}

fun LocalPlayer.isInHole(feetBlockPos: BlockPos = getFeetBlockPos()): Boolean {
    return DIRECTIONS_EXCLUDING_UP.all {
        feetBlockPos.relative(it).isBlastResistant()
    }
}

fun LocalPlayer.isBurrowed(): Boolean {
    return getFeetBlockPos().isBlastResistant()
}

fun LocalPlayer.getFeetBlockPos(): BlockPos {
    val bb = boundingBox
    return BlockPos(
        Mth.floor(Mth.lerp(0.5, bb.minX, bb.maxX)),
        Mth.ceil(bb.minY),
        Mth.floor(Mth.lerp(0.5, bb.minZ, bb.maxZ))
    )
}

val LivingEntity.wouldBlockHit
    get() = !isOlderThanOrEqual1_8 &&
        this.blockedByShield(this.level().damageSources().playerAttack(player))

/**
 * @see <a href="https://minecraft.fandom.com/wiki/Magma_Block#Damage">Magma Block — Damage</a>
 */
val LocalPlayer.immuneToMagmaBlocks
    get() = this.hasEffect(MobEffects.FIRE_RESISTANCE)
        || (this.getEffect(MobEffects.RESISTANCE)?.amplifier ?: -1) >= 4
        || this.isCreative
        || this.isSpectator
        || this.getItemBySlot(EquipmentSlot.FEET).getEnchantment(Enchantments.FROST_WALKER) > 0
