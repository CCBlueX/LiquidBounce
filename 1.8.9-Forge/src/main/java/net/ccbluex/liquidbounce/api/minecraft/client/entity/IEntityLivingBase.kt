/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion

interface IEntityLivingBase : IEntity {
    val creatureAttribute: IEnumCreatureAttribute
    val hurtTime: Int
    val isOnLadder: Boolean
    val jumpMovementFactor: Float
    val moveStrafing: Float
    val moveForward: Float
    var health: Float

    fun canEntityBeSeen(it: IEntity): Boolean
    fun isPotionActive(potion: IPotion): Boolean
    fun jump()
    fun swingItem()
}