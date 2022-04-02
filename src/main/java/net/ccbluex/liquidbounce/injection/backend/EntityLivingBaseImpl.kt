/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.PotionEffect

open class EntityLivingBaseImpl<T : EntityLivingBase>(wrapped: T) : EntityImpl<T>(wrapped), IEntityLivingBase {
    override val maxHealth: Float
        get() = wrapped.maxHealth
    override var prevRotationYawHead: Float
        get() = wrapped.prevRotationYawHead
        set(value) {
            wrapped.prevRotationYawHead = value
        }
    override var renderYawOffset: Float
        get() = wrapped.renderYawOffset
        set(value) {
            wrapped.renderYawOffset = value
        }
    override val activePotionEffects: Collection<IPotionEffect>
        get() = WrappedCollection<PotionEffect, IPotionEffect, Collection<PotionEffect>>(wrapped.activePotionEffects, IPotionEffect::unwrap, PotionEffect::wrap)
    override val isSwingInProgress: Boolean
        get() = wrapped.isSwingInProgress
    override var cameraPitch: Float
        get() = wrapped.cameraPitch
        set(value) {
            wrapped.cameraPitch = value
        }
    override val team: ITeam?
        get() = wrapped.team?.wrap()
    override val creatureAttribute: IEnumCreatureAttribute
        get() = wrapped.creatureAttribute.wrap()
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

    override fun isPotionActive(potion: IPotion): Boolean = wrapped.isPotionActive(potion.unwrap())

    override fun swingItem() = wrapped.swingItem()

    override fun getActivePotionEffect(potion: IPotion): IPotionEffect = wrapped.getActivePotionEffect(potion.unwrap()).wrap()

    override fun removePotionEffectClient(id: Int) = wrapped.removePotionEffectClient(id)

    override fun addPotionEffect(effect: IPotionEffect) = wrapped.addPotionEffect(effect.unwrap())

    override fun getEquipmentInSlot(index: Int): IItemStack? = wrapped.getEquipmentInSlot(index)?.wrap()
}

inline fun IEntityLivingBase.unwrap(): EntityLivingBase = (this as EntityLivingBaseImpl<*>).wrapped
inline fun EntityLivingBase.wrap(): IEntityLivingBase = EntityLivingBaseImpl(this)