package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil

val KeyBinding.pressedOnKeyboard
    get() = InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)
