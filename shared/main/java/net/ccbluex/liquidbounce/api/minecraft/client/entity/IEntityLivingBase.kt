/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam

interface IEntityLivingBase : IEntity {
    val maxHealth: Float
    var prevRotationYawHead: Float
    var renderYawOffset: Float
    val activePotionEffects: Collection<IPotionEffect>
    val isSwingInProgress: Boolean
    var cameraPitch: Float
    val team: ITeam?
    val creatureAttribute: IEnumCreatureAttribute
    val hurtTime: Int
    val isOnLadder: Boolean
    var jumpMovementFactor: Float
    val moveStrafing: Float
    val moveForward: Float
    var health: Float
    var rotationYawHead: Float

    fun canEntityBeSeen(it: IEntity): Boolean
    fun isPotionActive(potion: IPotion): Boolean
    fun swingItem()
    fun getActivePotionEffect(potion: IPotion): IPotionEffect?
    fun removePotionEffectClient(id: Int)
    fun addPotionEffect(effect: IPotionEffect)
    fun getEquipmentInSlot(index: Int): IItemStack?
}