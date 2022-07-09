/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IAbstractClientPlayer
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.player.EntityPlayerImpl
import net.minecraft.client.entity.AbstractClientPlayer

open class AbstractClientPlayerImpl<out T : AbstractClientPlayer>(wrapped: T) : EntityPlayerImpl<T>(wrapped), IAbstractClientPlayer
