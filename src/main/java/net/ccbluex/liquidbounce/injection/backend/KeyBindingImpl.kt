/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.settings.IKeyBinding
import net.minecraft.client.settings.KeyBinding

class KeyBindingImpl(val wrapped: KeyBinding) : IKeyBinding {
    override val keyCode: Int
        get() = wrapped.keyCode
    override var pressed: Boolean
        get() = wrapped.pressed
        set(value) {
            wrapped.pressed = value
        }
    override val isKeyDown: Boolean
        get() = wrapped.isKeyDown

    override fun onTick(keyCode: Int) = KeyBinding.onTick(keyCode)

    override fun equals(other: Any?): Boolean {
        return other is KeyBindingImpl && other.wrapped == this.wrapped
    }
}

inline fun IKeyBinding.unwrap(): KeyBinding = (this as KeyBindingImpl).wrapped
inline fun KeyBinding.wrap(): IKeyBinding = KeyBindingImpl(this)