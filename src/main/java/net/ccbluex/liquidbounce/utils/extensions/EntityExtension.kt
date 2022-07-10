/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntityLivingBase
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val Entity.isFriend: Boolean
    get() = this is EntityPlayer && isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity): Double
{
    val eyes = getPositionEyes(1F)
    val pos = getNearestPointBB(eyes, entity.entityBoundingBox)

    val xDelta = abs(pos.xCoord - eyes.xCoord)
    val yDelta = abs(pos.yCoord - eyes.yCoord)
    val zDelta = abs(pos.zCoord - eyes.zCoord)

    return sqrt(xDelta.pow(2) + yDelta.pow(2) + zDelta.pow(2))
}

fun getNearestPointBB(eye: Vec3, box: AxisAlignedBB): Vec3
{
    val origin = doubleArrayOf(eye.xCoord, eye.yCoord, eye.zCoord)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

    repeat(3) { i -> if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i] }

    return Vec3(origin[0], origin[1], origin[2])
}

fun Entity.isAnimal(): Boolean = this is EntityAnimal || this is EntitySquid || this is EntityGolem || this is EntityBat

fun Entity.isMob(): Boolean = this is EntityMob || this is EntityVillager || this is EntitySlime || this is EntityGhast || this is EntityDragon

fun Entity.isArmorStand(): Boolean = this is EntityArmorStand

fun Entity.isClientTarget(): Boolean = this is EntityPlayer && LiquidBounce.fileManager.targetsConfig.isTarget(stripColor(name))

/**
 * Check if entity is alive
 */
fun EntityLivingBase.isAlive(aac: Boolean = false) = isEntityAlive && health > 0 || aac && hurtTime > 5 // AAC Raycast bots

fun Entity?.isSelected(attackableCheck: Boolean = false): Boolean
{
    val thePlayer = mc.thePlayer

    if (this != null && this is EntityLivingBase && (EntityUtils.targetDead || isEntityAlive) && this != thePlayer && entityId >= 0)
    {
        if (!EntityUtils.targetInvisible && isInvisible) return false

        if (EntityUtils.targetPlayer && this is EntityPlayer)
        {
            if (!attackableCheck) return true

            // Spectator check
            if (this.isSpectator) return false

            // Friend check
            if (this.isFriend) return false

            // Team check
            val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams
            if (teams.state && teams.isInYourTeam(this)) return false

            // Bot check
            return !AntiBot.isBot(mc.theWorld ?: return false, thePlayer ?: return false, this)
        }

        return EntityUtils.targetMobs && isMob() || EntityUtils.targetAnimals && isAnimal() || EntityUtils.targetArmorStand && isArmorStand()
    }

    return false
}

/**
 * Check if entity is selected as enemy with current target options and other modules
 */
fun Entity?.isEnemy(aac: Boolean = false): Boolean
{
    val thePlayer = mc.thePlayer ?: return false

    if (this != null && this is EntityLivingBase && (EntityUtils.targetDead || isAlive(aac)) && this != thePlayer && entityId >= 0)
    {
        if (!EntityUtils.targetInvisible && isInvisible) return false

        if (EntityUtils.targetPlayer && this is EntityPlayer)
        {
            // Spectator check
            if (this.isSpectator) return false

            // Friend check
            if (this.isFriend) return false

            // Team check
            val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams
            if (teams.state && teams.isInYourTeam(this)) return false

            // Bot check
            return !AntiBot.isBot(mc.theWorld ?: return false, thePlayer, this)
        }

        return EntityUtils.targetMobs && isMob() || EntityUtils.targetAnimals && isAnimal() || EntityUtils.targetArmorStand && isArmorStand()
    }

    return false
}

fun EntityLivingBase.setCanBeCollidedWith(value: Boolean) = (this as IMixinEntityLivingBase).setCanBeCollidedWith(value)
