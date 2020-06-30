/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.projectile.IEntityFishHook
import net.minecraft.entity.projectile.EntityFishHook

class EntityFishHookImpl(wrapped: EntityFishHook) : EntityImpl<EntityFishHook>(wrapped), IEntityFishHook