/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.file.FileManager.friendsConfig
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getFixedSensitivityAngle
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.event.ForgeEventFactory

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity) = eyes.distanceTo(getNearestPointBB(eyes, entity.hitBox))

fun Entity.getDistanceToBox(box: AxisAlignedBB) = eyes.distanceTo(getNearestPointBB(eyes, box))

fun EntityPlayerSP.isNearEdge(threshold: Float): Boolean {
    val playerPos = Vec3(this.posX, this.posY, this.posZ)
    val blockPos = BlockPos(playerPos)

    for (x in -3..3) {
        for (z in -3..3) {
            val checkPos = blockPos.add(x, -1, z)
            if (this.worldObj.isAirBlock(checkPos)) {
                val checkPosCenter = Vec3(checkPos.x + 0.5, checkPos.y.toDouble(), checkPos.z + 0.5)
                val distance = playerPos.distanceTo(checkPosCenter)
                if (distance <= threshold) {
                    return true
                }
            }
        }
    }
    return false
}

fun getNearestPointBB(eye: Vec3, box: AxisAlignedBB): Vec3 {
    val origin = doubleArrayOf(eye.xCoord, eye.yCoord, eye.zCoord)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)
    for (i in 0..2) {
        if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i]
    }
    return Vec3(origin[0], origin[1], origin[2])
}

fun EntityPlayer.getPing() = mc.netHandler.getPlayerInfo(uniqueID)?.responseTime ?: 0

fun Entity.isAnimal() =
    this is EntityAnimal
        || this is EntitySquid
        || this is EntityGolem
        || this is EntityBat

fun Entity.isMob() =
    this is EntityMob
        || this is EntityVillager
        || this is EntitySlime
        || this is EntityGhast
        || this is EntityDragon

fun EntityPlayer.isClientFriend(): Boolean {
    val entityName = name ?: return false

    return friendsConfig.isFriend(stripColor(entityName))
}

val Entity?.rotation
    get() = Rotation(this?.rotationYaw ?: 0f, this?.rotationPitch ?: 0f)

val Entity.hitBox: AxisAlignedBB
    get() {
        val borderSize = collisionBorderSize.toDouble()
        return entityBoundingBox.expand(borderSize, borderSize, borderSize)
    }

val Entity.eyes: Vec3
    get() = getPositionEyes(1f)

val Entity.prevPos: Vec3
    get() = Vec3(this.prevPosX, this.prevPosY, this.prevPosZ)

val Entity.currPos: Vec3
    get() = this.positionVector

fun Entity.setPosAndPrevPos(currPos: Vec3, prevPos: Vec3 = currPos) {
    setPosition(currPos.xCoord, currPos.yCoord, currPos.zCoord)
    prevPosX = prevPos.xCoord
    prevPosY = prevPos.yCoord
    prevPosZ = prevPos.zCoord
}

fun EntityPlayerSP.setFixedSensitivityAngles(yaw: Float? = null, pitch: Float? = null) {
    if (yaw != null) fixedSensitivityYaw = yaw

    if (pitch != null) fixedSensitivityPitch = pitch
}

var EntityPlayerSP.fixedSensitivityYaw
    get() = getFixedSensitivityAngle(mc.thePlayer.rotationYaw)
    set(yaw) {
        rotationYaw = getFixedSensitivityAngle(yaw, rotationYaw)
    }

var EntityPlayerSP.fixedSensitivityPitch
    get() = getFixedSensitivityAngle(rotationPitch)
    set(pitch) {
        rotationPitch = getFixedSensitivityAngle(pitch.coerceIn(-90f, 90f), rotationPitch)
    }

// Makes fixedSensitivityYaw, ... += work
operator fun EntityPlayerSP.plusAssign(value: Float) {
    fixedSensitivityYaw += value
    fixedSensitivityPitch += value
}

fun Entity.interpolatedPosition() = Vec3(
    prevPosX + (posX - prevPosX) * mc.timer.renderPartialTicks,
    prevPosY + (posY - prevPosY) * mc.timer.renderPartialTicks,
    prevPosZ + (posZ - prevPosZ) * mc.timer.renderPartialTicks
)

fun EntityPlayerSP.stopXZ() {
    motionX = 0.0
    motionZ = 0.0
}

fun EntityPlayerSP.stop() {
    stopXZ()
    motionY = 0.0
}

// Modified mc.playerController.onPlayerRightClick() that sends correct stack in its C08
fun EntityPlayerSP.onPlayerRightClick(
    clickPos: BlockPos, side: EnumFacing, clickVec: Vec3,
    stack: ItemStack? = inventory.mainInventory[serverSlot],
): Boolean {
    if (clickPos !in worldObj.worldBorder)
        return false

    val (facingX, facingY, facingZ) = (clickVec - clickPos.toVec()).toFloatTriple()

    val sendClick = {
        sendPacket(C08PacketPlayerBlockPlacement(clickPos, side.index, stack, facingX, facingY, facingZ))
        true
    }

    // If player is a spectator, send click and return true
    if (mc.playerController.isSpectator)
        return sendClick()

    val item = stack?.item

    if (item?.onItemUseFirst(stack, this, worldObj, clickPos, side, facingX, facingY, facingZ) == true)
        return true

    val blockState = getState(clickPos)

    // If click had activated a block, send click and return true
    if ((!isSneaking || item == null || item.doesSneakBypassUse(worldObj, clickPos, this))
        && blockState?.block?.onBlockActivated(worldObj,
            clickPos,
            blockState,
            this,
            side,
            facingX,
            facingY,
            facingZ
        ) == true)
        return sendClick()

    if (item is ItemBlock && !item.canPlaceBlockOnSide(worldObj, clickPos, side, this, stack))
        return false

    sendClick()

    if (stack == null)
        return false

    val prevMetadata = stack.metadata
    val prevSize = stack.stackSize

    return stack.onItemUse(this, worldObj, clickPos, side, facingX, facingY, facingZ).also {
        if (mc.playerController.isInCreativeMode) {
            stack.itemDamage = prevMetadata
            stack.stackSize = prevSize
        } else if (stack.stackSize <= 0) {
            ForgeEventFactory.onPlayerDestroyItem(this, stack)
        }
    }
}

// Modified mc.playerController.sendUseItem() that sends correct stack in its C08
fun EntityPlayerSP.sendUseItem(stack: ItemStack): Boolean {
    if (mc.playerController.isSpectator)
        return false

    sendPacket(C08PacketPlayerBlockPlacement(stack))

    val prevSize = stack.stackSize

    val newStack = stack.useItemRightClick(worldObj, this)

    return if (newStack != stack || newStack.stackSize != prevSize) {
        if (newStack.stackSize <= 0) {
            mc.thePlayer.inventory.mainInventory[serverSlot] = null
            ForgeEventFactory.onPlayerDestroyItem(mc.thePlayer, newStack)
        } else
            mc.thePlayer.inventory.mainInventory[serverSlot] = newStack

        true
    } else false
}

fun EntityPlayerSP.tryJump() {
    if (!mc.gameSettings.keyBindJump.isKeyDown) {
        this.jump()
    }
}