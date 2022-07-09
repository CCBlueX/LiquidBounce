/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.projectile

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityFishHook
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImpl
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.wrap
import net.minecraft.entity.projectile.EntityFishHook

class EntityFishHookImpl(wrapped: EntityFishHook) : EntityImpl<EntityFishHook>(wrapped), IEntityFishHook
{
    override val inGround: Boolean
        get() = wrapped.inGround

    override val caughtEntity: IEntity?
        get() = wrapped.caughtEntity?.wrap()
}
