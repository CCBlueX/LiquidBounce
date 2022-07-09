/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.projectile

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPotion
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.minecraft.entity.projectile.EntityPotion

class EntityPotionImpl(wrapped: EntityPotion) : EntityThrowableImpl<EntityPotion>(wrapped), IEntityPotion
{
	override val potionDamage: Int
		get() = wrapped.potionDamage
	override val potionItem: IItemStack?
		get() = Backend.BACKEND_UNSUPPORTED()
}
