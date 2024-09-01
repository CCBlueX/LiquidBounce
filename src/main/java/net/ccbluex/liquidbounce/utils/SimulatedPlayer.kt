/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import com.google.common.base.Predicate
import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay
import net.ccbluex.liquidbounce.utils.extensions.isInWeb
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.client.input.Input
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.ProtectionEnchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.AbstractEntityAttribute
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.ClientPlayerEntity
import net.minecraft.entity.player.HungerManager
import net.minecraft.entity.player.PlayerAbilities
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.entity.vehicle.MinecartEntity
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockPos.Mutable
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraft.world.border.WorldBorder
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkProvider
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Compatible with client user ONLY. Useful for predicting movement ticks ahead.
 */
@Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")
class SimulatedPlayer(
    private val player: ClientPlayerEntity,
    var box: Box,
    var movementInput: Input,
    private var jumpingCooldown: Int,
    var velocityZ: Double,
    var velocityY: Double,
    var velocityX: Double,
    var inWater: Boolean,
    var onGround: Boolean,
    private var isAirBorne: Boolean,
    var rotationYaw: Float,
    var posX: Double,
    var posY: Double,
    var posZ: Double,
    private val abilities: PlayerAbilities,
    private val vehicle: Entity?,
    private var flyingSpeed: Float,
    private val world: World,
    var isCollidedHorizontally: Boolean,
    var horizontalCollision: Boolean,
    private val worldBorder: WorldBorder,
    private val chunkProvider: ChunkProvider,
    private var isOutsideBorder: Boolean,
    private var riddenByEntity: Entity?,
    private var attributeMap: AbstractEntityAttribute?,
    private val isSpectator: Boolean,
    var fallDistance: Float,
    private val stepHeight: Float,
    var isCollided: Boolean,
    private var fire: Int,
    private var distanceWalkedModified: Float,
    private var distanceWalkedOnStepModified: Float,
    private var nextStepDistance: Int,
    private val height: Float,
    private val width: Float,
    private val fireResistance: Int,
    var isInWeb: Boolean,
    private var noClip: Boolean,
    private var isSprinting: Boolean,
    private val foodStats: HungerManager,
) : MinecraftInstance() {
    val pos: Vec3d
        get() = Vec3d(posX, posY, posZ)

    private var moveForward = 0f
    private var moveStrafing = 0f
    private var isJumping = false

    companion object {

        private const val SPEED_IN_AIR = 0.02F

        fun fromClientPlayer(input: Input): SimulatedPlayer {
            val player = mc.player

            val abilities = createCapabilitiesCopy(player)
            val foodStats = createFoodStatsCopy(player)

            val movementInput = Input().apply {
                this.jumping = input.jumping
                this.movementForward = input.movementForward
                this.movementSideways = input.movementSideways
                this.sneaking = input.sneaking
            }

            return SimulatedPlayer(player,
                player.boundingBox,
                movementInput,
                player.jumpingCooldown,
                player.velocityZ,
                player.velocityY,
                player.velocityX,
                player.isTouchingWater,
                player.onGround,
                player.velocityDirty,
                player.yaw,
                player.x,
                player.y,
                player.z,
                abilities,
                player.rider,
                player.flyingSpeed,
                player.world,
                player.horizontalCollision,
                player.verticalCollision,
                player.world.worldBorder,
                player.world.chunkProvider,
                player.isOutsideWorldBorder,
                player.riddenByEntity,
                player.attributeMap,
                player.isSpectator,
                player.fallDistance,
                player.stepHeight,
                player.colliding,
                player.fire,
                player.distanceWalkedModified,
                player.distanceWalkedOnStepModified,
                player.nextStepDistance,
                player.height,
                player.width,
                player.fireResistance,
                player.isInWeb(),
                player.noClip,
                player.isSprinting,
                foodStats
            )
        }

        private fun createFoodStatsCopy(player: ClientPlayerEntity): HungerManager {
            val foodStatsNBT = NBTTagCompound()
            val foodStats = HungerManager()

            player.hungerManager.writeNBT(foodStatsNBT)
            foodStats.readNBT(foodStatsNBT)
            return foodStats
        }

        private fun createCapabilitiesCopy(player: ClientPlayerEntity): PlayerAbilities {
            val capabilitiesNBT = NBTTagCompound()
            val abilities = PlayerCapabilities()

            player.abilities.writeCapabilitiesToNBT(capabilitiesNBT)
            abilities.readCapabilitiesFromNBT(capabilitiesNBT)

            return abilities
        }
    }

    fun tick() {
        if (!onEntityUpdate() || player.rider != null) {
            return
        }

        playerUpdate(false)
        clientPlayerLivingUpdate()
        playerUpdate(true)
    }

    private fun clientPlayerLivingUpdate() {
        pushOutOfBlocks(posX - width.toDouble() * 0.35,
            getEntityBoundingBox().minY + 0.5,
            posZ + width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX - width.toDouble() * 0.35,
            getEntityBoundingBox().minY + 0.5,
            posZ - width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX + width.toDouble() * 0.35,
            getEntityBoundingBox().minY + 0.5,
            posZ - width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX + width.toDouble() * 0.35,
            getEntityBoundingBox().minY + 0.5,
            posZ + width.toDouble() * 0.35
        )

        val flag3 = this.foodStats.foodLevel.toFloat() > 6.0f || abilities.allowFlying
        val f = 0.8

        val shouldSprint = player.isSprinting

        if (onGround && movementInput.movementForward >= f && !isSprinting() && flag3 && !player.isUsingItem && !isPotionActive(
                Potion.blindness
            ) && shouldSprint) {
            setSprinting(true)
        }

        if (!isSprinting() && movementInput.movementForward >= f && flag3 && !player.isUsingItem && !isPotionActive(Potion.blindness) && shouldSprint) {
            setSprinting(true)
        }

        if (movementInput.sneak) {
            setSprinting(false)
        }

        if (isSprinting() && (movementInput.movementForward < 0.8 || isCollidedHorizontally || !flag3)) {
            setSprinting(false)
        }

        if (abilities.allowFlying) {
            if (mc.interactionManager.isSpectator) {
                if (!abilities.flying) {
                    abilities.flying = true
                }
            }
        }

        if (abilities.flying) {
            if (movementInput.sneaking) {
                velocityY -= (abilities.flySpeed * 3.0f).toDouble()
            }
            if (movementInput.jumping) {
                velocityY += (abilities.flySpeed * 3.0f).toDouble()
            }
        }

        livingEntityUpdate()
    }

    private fun playerUpdate(post: Boolean) {
        if (!post) {
            noClip = this.isSpectator

            if (this.isSpectator) {
                onGround = false
            }
        } else {
            clampPositionFromEntityPlayer()
        }
    }

    private fun livingEntityUpdate() {
        if (this.jumpingCooldown > 0) {
            --this.jumpingCooldown
        }

        if (abs(this.velocityX) < 0.005) {
            this.velocityX = 0.0
        }

        if (abs(this.velocityY) < 0.005) {
            this.velocityY = 0.0
        }

        if (abs(this.velocityZ) < 0.005) {
            this.velocityZ = 0.0
        }

        if (this.isMovementBlocked()) {
            this.isJumping = false
            this.moveStrafing = 0.0f
            this.moveForward = 0.0f
        } else if (this.isServerWorld()) {
            this.updateLivingEntityInput()
        }

        if (this.isJumping) {
            if (this.isTouchingWater() || this.isTouchingLava()) {
                this.updateAITick()
            } else if (this.onGround && this.jumpingCooldown == 0) {
                this.jump()
                if (NoJumpDelay.handleEvents()) {
                    this.jumpingCooldown = 10
                }
            }
        } else {
            this.jumpingCooldown = 0
        }

        this.moveStrafing *= 0.98f
        this.moveForward *= 0.98f
        this.playerSideMoveEntityWithHeading(this.moveStrafing, this.moveForward)

        // EntityPlayer post onLivingUpdate
        flyingSpeed = SPEED_IN_AIR
        if (isSprinting()) {
            flyingSpeed = (flyingSpeed.toDouble() + SPEED_IN_AIR.toDouble() * 0.3).toFloat()
        }

        // EntityPlayerSP post onLivingUpdate
        if (this.onGround && this.abilities.flying && !isSpectator) {
            this.abilities.flying = false
        }
    }

    // Entity version of onEntityUpdate
    private fun onEntityUpdate(): Boolean {
        handleWaterMovement()
        if (world.isClient) {
            fire = 0
        } else if (fire > 0) {
            /*if (this.isImmuneToFire()) {
                fire -= 4
                if (fire < 0) {
                    fire = 0
                }
            } else {*/
            --fire
            //}
        }

        if (isTouchingLava()) {
            setOnFireFromLava()
            fallDistance *= 0.5f
        }

        // If player is below world then just ignore
        if (posY < -64.0) {
            return false
        }

        return true
    }

    private fun clampPositionFromEntityPlayer() {
        // Post EntityPlayer onUpdate
        val d3 = MathHelper.clamp(posX, -2.9999999E7, 2.9999999E7)
        val d4 = MathHelper.clamp(posZ, -2.9999999E7, 2.9999999E7)
        if (d3 != posX || d4 != posZ) {
            setPosition(d3, posY, d4)
        }
    }

    private fun setPosition(x: Double, y: Double, z: Double) {
        posX = x
        posY = y
        posZ = z
        val f = width / 2.0f
        val f1 = height
        setEntityBoundingBox(Box(x - f.toDouble(),
            y,
            z - f.toDouble(),
            x + f.toDouble(),
            y + f1.toDouble(),
            z + f.toDouble()
        )
        )
    }

    private fun setSprinting(state: Boolean) {
        isSprinting = state
    }

    private fun pushOutOfBlocks(x: Double, y: Double, z: Double): Boolean {
        return if (noClip) {
            false
        } else {
            val blockPos = BlockPos(x, y, z)
            val d0 = x - blockPos.x.toDouble()
            val d1 = z - blockPos.z.toDouble()
            val entHeight = ceil(height.toDouble()).toInt().coerceAtLeast(1)
            val inTranslucentBlock: Boolean = !this.isHeadspaceFree(blockPos, entHeight)
            if (inTranslucentBlock) {
                var i = -1
                var d2 = 9999.0
                if (this.isHeadspaceFree(blockPos.west(), entHeight) && d0 < d2) {
                    d2 = d0
                    i = 0
                }
                if (this.isHeadspaceFree(blockPos.east(), entHeight) && 1.0 - d0 < d2) {
                    d2 = 1.0 - d0
                    i = 1
                }
                if (this.isHeadspaceFree(blockPos.north(), entHeight) && d1 < d2) {
                    d2 = d1
                    i = 4
                }
                if (this.isHeadspaceFree(blockPos.south(), entHeight) && 1.0 - d1 < d2) {
                    i = 5
                }

                val f = 0.1f
                if (i == 0) {
                    velocityX = (-f).toDouble()
                }
                if (i == 1) {
                    velocityX = f.toDouble()
                }
                if (i == 4) {
                    velocityZ = (-f).toDouble()
                }
                if (i == 5) {
                    velocityZ = f.toDouble()
                }
            }
            false
        }
    }

    private fun isHeadspaceFree(pos: BlockPos, height: Int): Boolean {
        for (y in 0 until height) {
            if (!this.isOpenBlockSpace(pos.add(0, y, 0))) {
                return false
            }
        }
        return true
    }

    private fun isOpenBlockSpace(pos: BlockPos): Boolean {
        return getBlockState(pos)?.block?.isNormalCube == false
    }

    private fun playerSideMoveEntityWithHeading(moveStrafing: Float, moveForward: Float) {
        if (abilities.flying && vehicle == null) {
            val d3 = velocityY
            val f = flyingSpeed
            flyingSpeed = abilities.flySpeed * (if (isSprinting()) 2 else 1).toFloat()
            livingEntitySideMoveEntityWithHeading(moveStrafing, moveForward)
            velocityY = d3 * 0.6
            flyingSpeed = f
        } else {
            livingEntitySideMoveEntityWithHeading(moveStrafing, moveForward)
        }
    }

    private fun livingEntitySideMoveEntityWithHeading(strafing: Float, forwards: Float) {
        val d0: Double
        var f3: Float
        if (isServerWorld()) {
            var f5: Float
            var f6: Float
            if (!isTouchingWater() || this.abilities.flying) {
                if (!isTouchingLava() || this.abilities.flying) {
                    var f4 = 0.91f
                    if (onGround) {
                        f4 = world.getBlockState(BlockPos(MathHelper.floor(posX),
                            MathHelper.floor(this.getEntityBoundingBox().minY) - 1,
                            MathHelper.floor(posZ)
                        )
                        ).block.slipperiness * 0.91f
                    }

                    val f = 0.16277136f / (f4 * f4 * f4)
                    f5 = if (onGround) {
                        getAIMoveSpeed() * f
                    } else {
                        flyingSpeed
                    }

                    moveFlying(strafing, forwards, f5)
                    f4 = 0.91f
                    if (onGround) {
                        f4 = world.getBlockState(BlockPos(MathHelper.floor(posX),
                            MathHelper.floor(this.getEntityBoundingBox().minY) - 1,
                            MathHelper.floor(posZ)
                        )
                        ).block.slipperiness * 0.91f
                    }

                    if (isClimbing()) {
                        f6 = 0.15f
                        velocityX = MathHelper.clamp(velocityX, (-f6).toDouble(), f6.toDouble())
                        velocityZ = MathHelper.clamp(velocityZ, (-f6).toDouble(), f6.toDouble())
                        fallDistance = 0.0f
                        if (velocityY < -0.15) {
                            velocityY = -0.15
                        }

                        val flag = isSneaking()
                        if (flag && velocityY < 0.0) {
                            velocityY = 0.0
                        }
                    }

                    moveEntity(velocityX, velocityY, velocityZ)
                    if (isCollidedHorizontally && isClimbing()) {
                        velocityY = 0.2
                    }

                    if (world.isClient && (!world.isLoaded(BlockPos(posX.toInt(),
                            0,
                            posZ.toInt()
                        ), false
                        ) || !world.getChunk(BlockPos(posX.toInt(), 0, posZ.toInt())).isLoaded)) {
                        velocityY = if (posY > 0.0) {
                            -0.1
                        } else {
                            0.0
                        }
                    } else {
                        velocityY -= 0.08
                    }

                    velocityY *= 0.9800000190734863
                    velocityX *= f4.toDouble()
                    velocityZ *= f4.toDouble()
                } else {
                    d0 = posY
                    moveFlying(strafing, forwards, 0.02f)
                    moveEntity(velocityX, velocityY, velocityZ)
                    velocityX *= 0.5
                    velocityY *= 0.5
                    velocityZ *= 0.5
                    velocityY -= 0.02
                    if (isCollidedHorizontally && isOffsetPositionInLiquid(velocityX,
                            velocityY + 0.6000000238418579 - posY + d0,
                            velocityZ
                        )) {
                        velocityY = 0.30000001192092896
                    }
                }
            } else {
                d0 = posY
                f5 = 0.8f
                f6 = 0.02f
                f3 = EnchantmentHelper.getDepthStrider(player).toFloat()
                if (f3 > 3.0f) {
                    f3 = 3.0f
                }

                if (!onGround) {
                    f3 *= 0.5f
                }

                if (f3 > 0.0f) {
                    f5 += (0.54600006f - f5) * f3 / 3.0f
                    f6 += (getAIMoveSpeed() * 1.0f - f6) * f3 / 3.0f
                }

                moveFlying(strafing, forwards, f6)
                moveEntity(velocityX, velocityY, velocityZ)
                velocityX *= f5.toDouble()
                velocityY *= 0.800000011920929
                velocityZ *= f5.toDouble()
                velocityY -= 0.02
                if (isCollidedHorizontally && isOffsetPositionInLiquid(velocityX,
                        velocityY + 0.6000000238418579 - posY + d0,
                        velocityZ
                    )) {
                    velocityY = 0.30000001192092896
                }
            }
        }
    }

    private fun moveEntity(xMotion: Double, yMotion: Double, zMotion: Double) {
        var velocityX = xMotion
        var velocityY = yMotion
        var velocityZ = zMotion
        if (noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velocityX, velocityY, velocityZ))
            resetPositionToBB()
        } else {
            val d0 = posX
            val d1 = posY
            val d2 = posZ
            if (isInWeb()) {
                isInWeb() = false
                velocityX *= 0.25
                velocityY *= 0.05000000074505806
                velocityZ *= 0.25
                velocityX = 0.0
                velocityY = 0.0
                velocityZ = 0.0
            }
            var d3 = velocityX
            val d4 = velocityY
            var d5 = velocityZ
            val flag = onGround && isSneaking()
            if (flag) {
                val d6 = 0.05
                while (velocityX != 0.0 && world.doesBoxCollide(player,
                        this.getEntityBoundingBox().offset(velocityX, -1.0, 0.0)
                    ).isEmpty()) {
                    if (velocityX < d6 && velocityX >= -d6) {
                        velocityX = 0.0
                    } else if (velocityX > 0.0) {
                        velocityX -= d6
                    } else {
                        velocityX += d6
                    }
                    d3 = velocityX
                }
                while (velocityZ != 0.0 && world.doesBoxCollide(player,
                        this.getEntityBoundingBox().offset(0.0, -1.0, velocityZ)
                    ).isEmpty()) {
                    if (velocityZ < d6 && velocityZ >= -d6) {
                        velocityZ = 0.0
                    } else if (velocityZ > 0.0) {
                        velocityZ -= d6
                    } else {
                        velocityZ += d6
                    }
                    d5 = velocityZ
                }
                while (velocityX != 0.0 && velocityZ != 0.0 && world.doesBoxCollide(player,
                        this.getEntityBoundingBox().offset(velocityX, -1.0, velocityZ)
                    ).isEmpty()) {
                    if (velocityX < d6 && velocityX >= -d6) {
                        velocityX = 0.0
                    } else if (velocityX > 0.0) {
                        velocityX -= d6
                    } else {
                        velocityX += d6
                    }
                    d3 = velocityX
                    if (velocityZ < d6 && velocityZ >= -d6) {
                        velocityZ = 0.0
                    } else if (velocityZ > 0.0) {
                        velocityZ -= d6
                    } else {
                        velocityZ += d6
                    }
                    d5 = velocityZ
                }
            }
            val list1 = world.doesBoxCollide(player,
                this.getEntityBoundingBox().expand(velocityX, velocityY, velocityZ)
            )
            val box: Box = this.getEntityBoundingBox()
            var box1: Box
            val var22: Iterator<*> = list1.iterator()
            while (var22.hasNext()) {
                box1 = var22.next() as Box
                velocityY = box1.calculateYOffset(this.getEntityBoundingBox(), velocityY)
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, velocityY, 0.0))
            val flag1 = onGround || d4 != velocityY && d4 < 0.0
            var box13: Box
            var var55: Iterator<*>
            var55 = list1.iterator()
            while (var55.hasNext()) {
                box13 = var55.next()
                velocityX = box13.calculateXOffset(this.getEntityBoundingBox(), velocityX)
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velocityX, 0.0, 0.0))
            var55 = list1.iterator()
            while (var55.hasNext()) {
                box13 = var55.next()
                velocityZ = box13.calculateZOffset(this.getEntityBoundingBox(), velocityZ)
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, velocityZ))
            if (stepHeight > 0.0f && flag1 && (d3 != velocityX || d5 != velocityZ)) {
                val d11 = velocityX
                val d7 = velocityY
                val d8 = velocityZ
                val Box3: Box = this.getEntityBoundingBox()
                this.setEntityBoundingBox(box)
                velocityY = stepHeight.toDouble()
                val list = world.doesBoxCollide(player,
                    this.getEntityBoundingBox().addCoord(d3, velocityY, d5)
                )
                var Box4: Box = this.getEntityBoundingBox()
                val Box5 = Box4.addCoord(d3, 0.0, d5)
                var d9 = velocityY
                var Box6: Box
                val var35: Iterator<*> = list.iterator()
                while (var35.hasNext()) {
                    Box6 = var35.next() as Box
                    d9 = Box6.calculateYOffset(Box5, d9)
                }
                Box4 = Box4.offset(0.0, d9, 0.0)
                var d15 = d3
                var Box7: Box
                val var37: Iterator<*> = list.iterator()
                while (var37.hasNext()) {
                    Box7 = var37.next() as Box
                    d15 = Box7.calculateXOffset(Box4, d15)
                }
                Box4 = Box4.offset(d15, 0.0, 0.0)
                var d16 = d5
                var Box8: Box
                val var39: Iterator<*> = list.iterator()
                while (var39.hasNext()) {
                    Box8 = var39.next() as Box
                    d16 = Box8.calculateZOffset(Box4, d16)
                }
                Box4 = Box4.offset(0.0, 0.0, d16)
                var Box14: Box = this.getEntityBoundingBox()
                var d17 = velocityY
                var Box9: Box
                val var42: Iterator<*> = list.iterator()
                while (var42.hasNext()) {
                    Box9 = var42.next() as Box
                    d17 = Box9.calculateYOffset(Box14, d17)
                }
                Box14 = Box14.offset(0.0, d17, 0.0)
                var d18 = d3
                var Box10: Box
                val var44: Iterator<*> = list.iterator()
                while (var44.hasNext()) {
                    Box10 = var44.next() as Box
                    d18 = Box10.calculateXOffset(Box14, d18)
                }
                Box14 = Box14.offset(d18, 0.0, 0.0)
                var d19 = d5
                var Box11: Box
                val var46: Iterator<*> = list.iterator()
                while (var46.hasNext()) {
                    Box11 = var46.next() as Box
                    d19 = Box11.calculateZOffset(Box14, d19)
                }
                Box14 = Box14.offset(0.0, 0.0, d19)
                val d20 = d15 * d15 + d16 * d16
                val d10 = d18 * d18 + d19 * d19
                if (d20 > d10) {
                    velocityX = d15
                    velocityZ = d16
                    velocityY = -d9
                    this.setEntityBoundingBox(Box4)
                } else {
                    velocityX = d18
                    velocityZ = d19
                    velocityY = -d17
                    this.setEntityBoundingBox(Box14)
                }
                var Box12: Box
                val var50: Iterator<*> = list.iterator()
                while (var50.hasNext()) {
                    Box12 = var50.next() as Box
                    velocityY = Box12.calculateYOffset(this.getEntityBoundingBox(), velocityY)
                }
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, velocityY, 0.0))
                if (d11 * d11 + d8 * d8 >= velocityX * velocityX + velocityZ * velocityZ) {
                    velocityX = d11
                    velocityY = d7
                    velocityZ = d8
                    this.setEntityBoundingBox(Box3)
                }
            }
            resetPositionToBB()
            isCollidedHorizontally = d3 != velocityX || d5 != velocityZ
            horizontalCollision = d4 != velocityY
            onGround = horizontalCollision && d4 < 0.0
            isCollided = isCollidedHorizontally || horizontalCollision
            val i = MathHelper.floor(posX)
            val j = MathHelper.floor(posY - 0.20000000298023224)
            val k = MathHelper.floor(posZ)
            val blockPos = BlockPos(i, j, k)
            var block1 = world.getBlockState(blockPos).block
            if (block1.material === Material.AIR) {
                val block = world.getBlockState(blockPos.down()).block
                if (block is FenceBlock || block is WallBlock || block is FenceGateBlock) {
                    block1 = block
                }
            }
            updateFallState(velocityY, onGround)
            if (d3 != velocityX) {
                velocityX = 0.0
            }
            if (d5 != velocityZ) {
                velocityZ = 0.0
            }
            if (d4 != velocityY) {
                onLanded(block1)
            }
            if (canTriggerWalking() && !flag && vehicle == null) {
                val d12 = posX - d0
                var d13 = posY - d1
                val d14 = posZ - d2
                if (block1 !== Blocks.LADDER) {
                    d13 = 0.0
                }
                if (block1 != null && onGround) {
                    onEntityCollidedWithBlock(block1)
                }
                distanceWalkedModified = (distanceWalkedModified.toDouble() + MathHelper.sqrt(d12 * d12 + d14 * d14)
                    .toDouble() * 0.6).toFloat()
                distanceWalkedOnStepModified = (distanceWalkedOnStepModified.toDouble() + MathHelper.sqrt(d12 * d12 + d13 * d13 + d14 * d14)
                    .toDouble() * 0.6).toFloat()
                if (distanceWalkedOnStepModified > nextStepDistance.toFloat() && block1.material !== Material.AIR) {
                    nextStepDistance = distanceWalkedOnStepModified.toInt() + 1
                }
            }

            try {
                doBlockCollisions()
            } catch (var52: Throwable) {
                var52.printStackTrace()
            }

            val flag2 = isWet()

            if (world.isFlammableWithin(this.getEntityBoundingBox().contract(0.001, 0.001, 0.001))) {
                //this.dealFireDamage(1)
                if (!flag2) {
                    ++fire
                    if (fire == 0) {
                        setFire(8)
                    }
                }
            } else if (fire <= 0) {
                fire = -fireResistance
            }

            if (flag2 && fire > 0) {
                fire = -fireResistance
            }
        }
    }

    private fun getEntityBoundingBox(): Box {
        return box
    }

    private fun setEntityBoundingBox(box: Box) {
        this.box = box
    }

    private fun setOnFireFromLava() {
        setFire(15)
    }

    private fun setFire(seconds: Int) {
        var i = seconds * 20
        i = ProtectionEnchantment.method_4659(player, i)
        if (fire < i) {
            fire = i
        }
    }

    private fun isWet(): Boolean {
        return inWater || isRainingAt(BlockPos(posX, posY, posZ))
            || isRainingAt(BlockPos(posX, posY + this.height.toDouble(), posZ))
    }

    private fun doBlockCollisions() {
        val blockpos = BlockPos(this.getEntityBoundingBox().minX + 0.001,
            this.getEntityBoundingBox().minY + 0.001,
            this.getEntityBoundingBox().minZ + 0.001
        )
        val blockpos1 = BlockPos(this.getEntityBoundingBox().maxX - 0.001,
            this.getEntityBoundingBox().maxY - 0.001,
            this.getEntityBoundingBox().maxZ - 0.001
        )
        if (isAreaLoaded(blockpos.x, blockpos.y, blockpos.z, blockpos1.x, blockpos.y, blockpos.z, true)) {
            for (i in blockpos.x..blockpos1.x) {
                for (j in blockpos.y..blockpos1.y) {
                    for (k in blockpos.z..blockpos1.z) {
                        val pos = BlockPos(i, j, k)
                        val state = world.getBlockState(pos)
                        try {
                            val block = state.block
                            // We don't want things to negatively interact back to us (cactus, tripwire, tnt or whatever)
                            if (block is CobwebBlock) {
                                isInWeb = true
                            } else if (block is SoulSandBlock) {
                                velocityX *= 0.4
                                velocityZ *= 0.4
                            }
                        } catch (var11: Throwable) {
                            var11.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun updateFallState(velocityY: Double, onGround: Boolean) {
        if (!isTouchingWater()) {
            this.handleWaterMovement()
        }

        if (onGround) {
            if (fallDistance > 0.0f) {
                fallDistance = 0.0f
            }
        } else if (velocityY < 0.0) {
            fallDistance = (fallDistance.toDouble() - velocityY).toFloat()
        }
    }

    private fun handleWaterMovement(): Boolean {
        if (handleMaterialAcceleration(getEntityBoundingBox().expand(0.0, -0.4000000059604645, 0.0)
                .contract(0.001, 0.001, 0.001), Material.WATER
            )) {
            /*if (!inWater && !this.firstUpdate) {
                 this.resetHeight()
            }*/
            fallDistance = 0.0f
            inWater = true
            fire = 0
        } else {
            inWater = false
        }

        return inWater
    }

    private fun handleMaterialAcceleration(boundingBox: Box, material: Material): Boolean {
        val i = MathHelper.floor(boundingBox.minX)
        val j = MathHelper.floor(boundingBox.maxX + 1.0)
        val k = MathHelper.floor(boundingBox.minY)
        val l = MathHelper.floor(boundingBox.maxY + 1.0)
        val i1 = MathHelper.floor(boundingBox.minZ)
        val j1 = MathHelper.floor(boundingBox.maxZ + 1.0)
        return if (!isAreaLoaded(i, k, i1, j, l, j1, true)) {
            false
        } else {
            var flag = false
            var vec3d = Vec3d(0.0, 0.0, 0.0)
            val blockPos = Mutable()
            for (k1 in i until j) {
                for (l1 in k until l) {
                    for (i2 in i1 until j1) {
                        blockPos[k1, l1] = i2
                        val state = getBlockState(blockPos) ?: continue
                        val block = state.block ?: continue
                        // val result = null
                        // ^^ block.isEntityInsideMaterial(world, blockPos, state, player, l.toDouble(), material, false) always null
                        if (block.material === material) {
                            val d0 = ((l1 + 1).toFloat() - BlockLiquid.getLiquidHeightPercent((state.getValue(
                                BlockLiquid.LEVEL
                            ) as Int)
                            )).toDouble()
                            if (l.toDouble() >= d0) {
                                flag = true
                                vec3d = block.modifyAcceleration(world, blockPos, player, vec3d)
                            }
                        }
                    }
                }
            }
            if (vec3d.length() > 0.0 && isPushedByWater()) {
                vec3d = vec3d.normalize()
                val d1 = 0.014
                velocityX += vec3d.x * d1
                velocityY += vec3d.y * d1
                velocityZ += vec3d.z * d1
            }
            flag
        }
    }

    private fun isAreaLoaded(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, idfk: Boolean): Boolean {
        var minX1 = minX
        var minZ1 = minZ
        var maxX1 = maxX
        var maxZ1 = maxZ
        return if (maxY >= 0 && minY < 256) {
            minX1 = minX1 shr 4
            minZ1 = minZ1 shr 4
            maxX1 = maxX1 shr 4
            maxZ1 = maxZ1 shr 4
            for (i in minX1..maxX1) {
                for (j in minZ1..maxZ1) {
                    if (!isChunkLoaded(i, j, idfk)) {
                        return false
                    }
                }
            }
            true
        } else {
            false
        }
    }

    private fun onEntityCollidedWithBlock(block: Block) {
        if (block is SlimeBlock) {
            if (abs(velocityY) < 0.1 && !isSneaking()) {
                val motion = 0.4 + abs(velocityY) * 0.2

                velocityX *= motion
                velocityZ *= motion
            }
        }
    }

    private fun canTriggerWalking(): Boolean {
        return !abilities.flying
    }

    fun isClimbing(): Boolean {
        val i = MathHelper.floor(posX)
        val j = MathHelper.floor(box.minY)
        val k = MathHelper.floor(posZ)
        val block = world.getBlockState(BlockPos(i, j, k)).block
        return isLivingOnLadder(block, world, BlockPos(i, j, k), player)
    }

    private fun moveFlying(strafe: Float, forward: Float, friction: Float) {
        var newStrafe = strafe
        var newForward = forward
        var f = newStrafe * newStrafe + newForward * newForward
        if (f >= 1.0E-4f) {
            f = MathHelper.sqrt(f)
            if (f < 1.0f) {
                f = 1.0f
            }
            f = friction / f
            newStrafe *= f
            newForward *= f
            val f1 = MathHelper.sin(rotationYaw * 3.1415927f / 180.0f)
            val f2 = MathHelper.cos(rotationYaw * 3.1415927f / 180.0f)
            velocityX += (newStrafe * f2 - newForward * f1).toDouble()
            velocityZ += (newForward * f2 + newStrafe * f1).toDouble()
        }
    }

    private fun jump() {
        velocityY = getJumpUpwardsMotion().toDouble()
        if (isPotionActive(StatusEffect.JUMP_BOOST)) {
            velocityY += ((getActivePotionEffect(StatusEffect.JUMP_BOOST).amplifier + 1).toFloat() * 0.1f).toDouble()
        }

        if (isSprinting()) {
            val f = rotationYaw * 0.017453292f
            velocityX -= (MathHelper.sin(f) * 0.2f).toDouble()
            velocityZ += (MathHelper.cos(f) * 0.2f).toDouble()
        }

        isAirBorne = true
    }

    private fun isSprinting(): Boolean {
        return isSprinting
    }

    fun isPotionActive(statusEffect: StatusEffect): Boolean {
        return player.getEffectInstance(statusEffect) != null
    }

    fun getActivePotionEffect(statusEffect: StatusEffect): StatusEffectInstance {
        return player.getEffectInstance(statusEffect)
    }

    private fun getJumpUpwardsMotion(): Float {
        return 0.42f
    }

    private fun isTouchingWater(): Boolean {
        return inWater
    }

    private fun updateLivingEntityInput() {
        moveForward = movementInput.movementForward
        moveStrafing = movementInput.movementSideways
        isJumping = movementInput.jumping
    }

    private fun isServerWorld(): Boolean {
        return true
    }

    private fun isMovementBlocked(): Boolean {
        return player.health <= 0f || player.isSleeping
    }

    fun isTouchingLava(): Boolean {
        return this.world.isMaterialInBB(this.getEntityBoundingBox()
            .expand(-0.10000000149011612, -0.4000000059604645, -0.10000000149011612), Material.lava
        )
    }

    private fun updateAITick() {
        velocityY += 0.03999999910593033
    }

    private fun isOffsetPositionInLiquid(x: Double, y: Double, z: Double): Boolean {
        val box = this.getEntityBoundingBox().offset(x, y, z)

        return this.isLiquidPresentInAABB(box)
    }

    private fun isLiquidPresentInAABB(box: Box): Boolean {
        return world.doesBoxCollide(player, box).isEmpty() && !world.isAnyLiquid(box)
    }

    fun getCollidingBoundingBoxes(box: Box): List<Box> {
        val list: MutableList<Box> = Lists.newArrayList()
        val i = MathHelper.floor(box.minX)
        val j = MathHelper.floor(box.maxX + 1.0)
        val k = MathHelper.floor(box.minY)
        val l = MathHelper.floor(box.maxY + 1.0)
        val i1 = MathHelper.floor(box.minZ)
        val j1 = MathHelper.floor(box.maxZ + 1.0)
        val worldborder: WorldBorder = this.getWorldBorder()
        val flag = this.isOutsideBorder
        val flag1 = isInsideBorder(worldborder, flag)
        val iblockstate = Blocks.STONE.defaultState
        val blockPos = Mutable()
        for (k1 in i until j) {
            for (l1 in i1 until j1) {
                if (this.isBlockLoaded(blockPos.setPosition(k1, 64, l1))) {
                    for (i2 in k - 1 until l) {
                        blockPos[k1, i2] = l1
                        if (flag && flag1) {
                            isOutsideBorder = false
                        } else if (!flag && !flag1) {
                            isOutsideBorder = true
                        }
                        var state = iblockstate
                        if (worldborder.contains(blockPos) || !flag1) {
                            state = this.getBlockState(blockPos)
                        }
                        state.block.addCollisionBoxesToList(world,
                            blockPos,
                            state,
                            box,
                            list,
                            player
                        )
                    }
                }
            }
        }
        val d0 = 0.25
        val entities = this.getEntitiesWithinAABBExcludingEntity(player, box.expand(d0, d0, d0))
        for (size in entities.indices) {
            if (riddenByEntity !== entities && vehicle !== entities) {
                var boundingBox = entities[size].boundingBox

                if (boundingBox != null && boundingBox.intersects(box)) {
                    list.add(boundingBox)
                }
                boundingBox = getCollisionBox(player, entities[size])
                if (boundingBox != null && boundingBox.intersects(box)) {
                    list.add(boundingBox)
                }
            }
        }
        return list
    }

    fun getBlockState(blockPos: BlockPos): BlockState? {
        return world.getBlockState(blockPos)
    }

    private fun getChunkFromBlockCoords(blockPos: BlockPos): Chunk {
        return this.getChunkFromChunkCoords(blockPos.x shr 4, blockPos.z shr 4)
    }

    private fun getChunkFromChunkCoords(x: Int, z: Int): Chunk {
        return this.chunkProvider.provideChunk(x, z)
    }

    private fun isValid(pos: BlockPos): Boolean {
        return pos.x >= -30000000 && pos.z >= -30000000 && pos.x < 30000000 && pos.z < 30000000 && pos.y >= 0 && pos.y < 256
    }

    private fun getWorldBorder(): WorldBorder {
        return this.worldBorder
    }

    private fun isInsideBorder(border: WorldBorder, insideBorder: Boolean): Boolean {
        var d0 = border.minX()
        var d1 = border.minZ()
        var d2 = border.maxX()
        var d3 = border.maxZ()
        if (insideBorder) {
            ++d0
            ++d1
            --d2
            --d3
        } else {
            --d0
            --d1
            ++d2
            ++d3
        }
        return posX > d0 && posX < d2 && posZ > d1 && posZ < d3
    }

    private fun isBlockLoaded(pos: BlockPos): Boolean {
        return isBlockLoaded(pos, true)
    }

    private fun isBlockLoaded(pos: BlockPos, check2: Boolean): Boolean {
        return if (!isValid(pos)) false else isChunkLoaded(pos.x shr 4,
            pos.z shr 4,
            check2
        )
    }

    private fun isChunkLoaded(x: Int, z: Int, flag: Boolean): Boolean {
        return chunkProvider.chunkExists(x, z) && (flag || !chunkProvider.provideChunk(x, z).isEmpty)
    }

    private fun getEntitiesWithinAABBExcludingEntity(entity: Entity, box: Box): List<Entity> {
        return this.getEntitiesInAABBexcluding(entity,
            box,
            EntitySelectors.NOT_SPECTATING
        )
    }

    private fun getEntitiesInAABBexcluding(
        entity: Entity, bb: Box, predicate: Predicate<in Entity?>?,
    ): List<Entity> {
        val list: List<Entity> = Lists.newArrayList()
        val i = MathHelper.floor((bb.minX - World.MAX_ENTITY_RADIUS) / 16.0)
        val j = MathHelper.floor((bb.maxX + World.MAX_ENTITY_RADIUS) / 16.0)
        val k = MathHelper.floor((bb.minZ - World.MAX_ENTITY_RADIUS) / 16.0)
        val l = MathHelper.floor((bb.maxZ + World.MAX_ENTITY_RADIUS) / 16.0)
        for (i1 in i..j) {
            for (j1 in k..l) {
                if (isChunkLoaded(i1, j1, true)) {
                    getChunkFromChunkCoords(i1, j1).getEntitiesWithinAABBForEntity(entity, bb, list, predicate)
                }
            }
        }
        return list
    }

    private fun getCollisionBox(player: Entity, entity: Entity): Box? {
        return when (entity) {
            is BoatEntity -> {
                entity.boundingBox
            }

            is MinecartEntity -> {
                player.getHardCollisionBox(entity)
            }

            else -> null
        }
    }

    private fun getAIMoveSpeed(): Float {
        return this.getEntityAttribute(EntityAttributes.GENERIC_MOVEMENT_SPEED).attributeValue.toFloat()
    }

    private fun getEntityAttribute(entityAttribute: EntityAttribute?): EntityAttributeInstance {
        return this.getAttributeMap().getAttributeInstance(entityAttribute)
    }

    private fun getAttributeMap(): AbstractEntityAttribute {
        if (this.attributeMap == null) {
            this.attributeMap = ServersideAttributeMap()
        }

        return this.attributeMap!!
    }

    private fun isLivingOnLadder(block: Block?, world: World, pos: BlockPos?, entity: LivingEntity): Boolean {
        val isSpectator = this.isSpectator
        return if (isSpectator) {
            false
        } else if (!ForgeModContainer.fullBoundingBoxLadders) {
            block != null && block.isLadder(world, pos, entity)
        } else {
            val bb = this.box
            val mX = MathHelper.floor(bb.minX)
            val mY = MathHelper.floor(bb.minY)
            val mZ = MathHelper.floor(bb.minZ)
            var y2 = mY
            while (y2.toDouble() < bb.maxY) {
                var x2 = mX
                while (x2.toDouble() < bb.maxX) {
                    var z2 = mZ
                    while (z2.toDouble() < bb.maxZ) {
                        val tmp = BlockPos(x2, y2, z2)
                        if (world.getBlockState(tmp).block.isLadder(world, tmp, entity)) {
                            return true
                        }
                        ++z2
                    }
                    ++x2
                }
                ++y2
            }
            false
        }
    }

    private fun resetPositionToBB() {
        posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0
        posY = this.getEntityBoundingBox().minY
        posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0
    }

    private fun onLanded(block: Block) {
        if (block is SlimeBlock) {
            if (isSneaking()) {
                velocityY = 0.0
            } else if (velocityY < 0.0) {
                velocityY = -velocityY
            }
        } else {
            velocityY = 0.0
        }
    }

    fun isSneaking(): Boolean {
        return movementInput.sneaking && !player.isSleeping
    }

    private fun isRainingAt(pos: BlockPos): Boolean {
        return if (world.getRainStrength(1.0F) <= 0.2) {
            false
        } else if (!this.canSeeSky(pos)) {
            false
        } else if (world.getPrecipitationHeight(pos).y > pos.y) {
            false
        } else {
            val base: BiomeGenBase = world.getBiomeGenForCoords(pos)

            if (base.enableSnow) false else if (world.canSnowAt(pos, false)) false else base.canRain()
        }
    }

    private fun canSeeSky(pos: BlockPos): Boolean {
        return getChunkFromBlockCoords(pos).canSeeSky(pos)
    }

    private fun isPushedByWater(): Boolean {
        return !abilities.flying
    }

}