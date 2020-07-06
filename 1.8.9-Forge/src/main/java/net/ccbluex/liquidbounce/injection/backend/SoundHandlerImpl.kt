/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.audio.ISoundHandler
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.audio.SoundHandler
import net.minecraft.util.ResourceLocation

class SoundHandlerImpl(val wrapped: SoundHandler) : ISoundHandler {
    override fun playSound(name: String, pitch: Float) = wrapped.playSound(PositionedSoundRecord.create(ResourceLocation(name), pitch))


    override fun equals(other: Any?): Boolean {
        return other is SoundHandlerImpl && other.wrapped == this.wrapped
    }
}

inline fun ISoundHandler.unwrap(): SoundHandler = (this as SoundHandlerImpl).wrapped
inline fun SoundHandler.wrap(): ISoundHandler = SoundHandlerImpl(this)