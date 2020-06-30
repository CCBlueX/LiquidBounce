/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.entity.EntityLivingBase

open class EntityLivingBaseImpl<T : EntityLivingBase>(wrapped: T) : EntityImpl<T>(wrapped), IEntityLivingBase {
    override val activePotionEffects: Collection<IPotionEffect>
        get() = TODO("Not yet implemented")
    override val isSwingInProgress: Boolean
        get() = wrapped.isSwingInProgress
    override var cameraPitch: Float
        get() = wrapped.cameraPitch
        set(value) {
            wrapped.cameraPitch = value
        }
    override val team: ITeam?
        get() = TODO("Not yet implemented")
    override val creatureAttribute: IEnumCreatureAttribute
        get() = TODO("Not yet implemented")
    override val hurtTime: Int
        get() = wrapped.hurtTime
    override val isOnLadder: Boolean
        get() = wrapped.isOnLadder
    override var jumpMovementFactor: Float
        get() = wrapped.jumpMovementFactor
        set(value) {
            wrapped.jumpMovementFactor = value
        }
    override val moveStrafing: Float
        get() = wrapped.moveStrafing
    override val moveForward: Float
        get() = wrapped.moveForward
    override var health: Float
        get() = wrapped.health
        set(value) {
            wrapped.health = value
        }
    override var rotationYawHead: Float
        get() = wrapped.rotationYawHead
        set(value) {
            wrapped.rotationYawHead = value
        }

    override fun canEntityBeSeen(it: IEntity): Boolean = wrapped.canEntityBeSeen(it.unwrap())

    override fun isPotionActive(potion: IPotion): Boolean {
        TODO("Not yet implemented")
    }

    override fun swingItem() = wrapped.swingItem()

    override fun getActivePotionEffect(potion: IPotion): IPotionEffect {
        TODO("Not yet implemented")
    }
}