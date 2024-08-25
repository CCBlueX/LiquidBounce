/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.rotationPitch
import net.ccbluex.liquidbounce.file.FileManager.friendsConfig
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getFixedSensitivityAngle
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.dragon.EnderDragonEntity
import net.minecraft.entity.mob.GhastEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.BatEntity
import net.minecraft.entity.passive.GolemEntity
import net.minecraft.entity.passive.SquidEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity) = eyes.distanceTo(getNearestPointBB(eyes, entity.hitBox))

fun Entity.getDistanceToBox(box: Box) = eyes.distanceTo(getNearestPointBB(eyes, box))

fun PlayerEntity.isNearEdge(threshold: Float): Boolean {
    val playerPos = Vec3d(this.x, this.y, this.z)
    val blockPos = BlockPos(playerPos)

    for (x in -3..3) {
        for (z in -3..3) {
            val checkPos = blockPos.add(x, -1, z)
            if (this.world.isAir(checkPos)) {
                val checkPosCenter = Vec3d(checkPos.x + 0.5, checkPos.y.toDouble(), checkPos.z + 0.5)
                val distance = playerPos.distanceTo(checkPosCenter)
                if (distance <= threshold) {
                    return true
                }
            }
        }
    }
    return false
}

fun PlayerEntity.isInWeb(): Boolean {
    val block = this.world.getBlockState(this.blockPos).block
    return block == Blocks.COBWEB
}

fun getNearestPointBB(eye: Vec3d, box: Box): Vec3d {
    val origin = doubleArrayOf(eye.x, eye.y, eye.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)
    for (i in 0..2) {
        if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i]
    }
    return Vec3d(origin[0], origin[1], origin[2])
}

fun ClientPlayerEntity.getPing() = mc.networkHandler?.playerList?.find { it.profile.id == this.uuid }?.latency ?: 0

fun Entity.isAnimal() =
    this is AnimalEntity
        || this is SquidEntity
        || this is GolemEntity
        || this is BatEntity

fun Entity.isMob() =
    this is MobEntity
        || this is VillagerEntity
        || this is SlimeEntity
        || this is GhastEntity
        || this is EnderDragonEntity

fun ClientPlayerEntity.isClientFriend(): Boolean {
    val entityName = name ?: return false

    return friendsConfig.isFriend(stripColor(entityName.toString()))
}

val Entity?.rotations
    get() = Rotation(this?.yaw ?: 0f, this?.pitch ?: 0f)

val Entity.hitBox: Box
    get() {
        val borderSize = collisionBorderSize.toDouble()
        return entityBoundingBox.expand(borderSize, borderSize, borderSize)
    }

val Entity.eyes: Vec3d
    get() = getPositionEyes(1f)

val Entity.prevPos: Vec3d
    get() = Vec3d(this.prevX, this.prevY, this.prevZ)

val Entity.currPos: Vec3d
    get() = this.pos

fun Entity.setPosAndPrevPos(currPos: Vec3d, prevPos: Vec3d = currPos) {
    setPosition(currPos.x, currPos.y, currPos.z)
    prevX = prevPos.x
    prevY = prevPos.y
    prevZ = prevPos.z
}

fun ClientPlayerEntity.setFixedSensitivityAngles(yaw: Float? = null, pitch: Float? = null) {
    if (yaw != null) fixedSensitivityYaw = yaw

    if (pitch != null) fixedSensitivityPitch = pitch
}

var ClientPlayerEntity.fixedSensitivityYaw
    get() = getFixedSensitivityAngle(mc.player.yaw)
    set(rotationYaw) {
        yaw = getFixedSensitivityAngle(yaw, yaw)
    }

var ClientPlayerEntity.fixedSensitivityPitch
    get() = getFixedSensitivityAngle(rotationPitch)
    set(rotationPitch) {
        pitch = getFixedSensitivityAngle(pitch.coerceIn(-90f, 90f), pitch)
    }

// Makes fixedSensitivityYaw, ... += work
operator fun ClientPlayerEntity.plusAssign(value: Float) {
    fixedSensitivityYaw += value
    fixedSensitivityPitch += value
}

fun Entity.interpolatedPosition() = Vec3d(
    prevX + (x - prevX) * mc.timer.renderPartialTicks,
    prevY + (y - prevY) * mc.timer.renderPartialTicks,
    prevZ + (z - prevZ) * mc.timer.renderPartialTicks
)

fun ClientPlayerEntity.stopY() {
    velocityY = 0.0
}

fun ClientPlayerEntity.stopXZ() {
    velocityY = 0.0
    velocityZ = 0.0
}

fun ClientPlayerEntity.stop() {
    stopXZ()
    stopY()
}

// Modified mc.interactionManager.onPlayerRightClick() that sends correct stack in its C08
fun ClientPlayerEntity.onPlayerRightClick(
    clickPos: BlockPos, side: Direction, clickVec: Vec3d,
    stack: ItemStack? = inventory.main[serverSlot],
): Boolean {
    if (clickPos !in world.worldBorder)
        return false

    val (facingX, facingY, facingZ) = (clickVec - clickPos.toVec()).toFloatTriple()

    val sendClick = {
        sendPacket(C08PacketPlayerBlockPlacement(clickPos, side.id, stack, facingX, facingY, facingZ))
        true
    }

    // If player is a spectator, send click and return true
    if (mc.interactionManager.isSpectator)
        return sendClick()

    val item = stack?.item

    if (item?.onItemUseFirst(stack, this, world, clickPos, side, facingX, facingY, facingZ) == true)
        return true

    val blockState = getState(clickPos)

    // If click had activated a block, send click and return true
    if ((!isSneaking || item == null || item.doesSneakBypassUse(world, clickPos, this))
        && blockState?.block?.onBlockActivated(world,
            clickPos,
            blockState,
            this,
            side,
            facingX,
            facingY,
            facingZ
        ) == true)
        return sendClick()

    if (item is BlockItem && !item.canPlaceBlockOnSide(world, clickPos, side, this, stack))
        return false

    sendClick()

    if (stack == null)
        return false

    val prevMetadata = stack.data
    val prevSize = stack.count

    return stack.onItemUse(this, world, clickPos, side, facingX, facingY, facingZ).also {
        if (mc.interactionManager.isInCreativeMode) {
            stack.damage = prevMetadata
            stack.count = prevSize
        } else if (stack.count <= 0) {
            ForgeEventFactory.onPlayerDestroyItem(this, stack)
        }
    }
}

// Modified mc.interactionManager.sendUseItem() that sends correct stack in its C08
fun ClientPlayerEntity.sendUseItem(stack: ItemStack): Boolean {
    if (mc.interactionManager.isSpectator)
        return false

    sendPacket(C08PacketPlayerBlockPlacement(stack))

    val prevSize = stack.count

    val newStack = stack.useItemRightClick(world, this)

    return if (newStack != stack || newStack.stackSize != prevSize) {
        if (newStack.stackSize <= 0) {
            mc.player.inventory.main[serverSlot] = null
            ForgeEventFactory.onPlayerDestroyItem(mc.player, newStack)
        } else
            mc.player.inventory.main[serverSlot] = newStack

        true
    } else false
}

fun ClientPlayerEntity.tryJump() {
    if (!mc.options.jumpKey.isPressed) {
        this.jump()
    }
}