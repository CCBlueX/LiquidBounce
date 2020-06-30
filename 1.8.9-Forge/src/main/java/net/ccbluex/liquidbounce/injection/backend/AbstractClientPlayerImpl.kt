/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IAbstractClientPlayer
import net.minecraft.client.entity.AbstractClientPlayer

open class AbstractClientPlayerImpl<T : AbstractClientPlayer>(wrapped: T) : EntityPlayerImpl<T>(wrapped), IAbstractClientPlayer