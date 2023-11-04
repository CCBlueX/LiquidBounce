package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil

val KeyBinding.pressedOnKeyboard
    get() = InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)

var KeyBinding.enforced: Boolean?
    get() =
        when (this) {
            mc.options.jumpKey -> {
                TickStateManager.enforcedState.enforceJump
            }
            mc.options.forwardKey -> {
                TickStateManager.enforcedState.enforceForward
            }

            mc.options.backKey -> {
                TickStateManager.enforcedState.enforceBackward
            }

            mc.options.leftKey -> {
                TickStateManager.enforcedState.enforceLeft
            }

            mc.options.rightKey -> {
                TickStateManager.enforcedState.enforceRight
            }
            else -> {
                null
            }
        }
    set(value) {
        when (this) {
            mc.options.jumpKey -> {
                TickStateManager.enforcedState.enforceJump = value
            }
            mc.options.forwardKey -> {
                TickStateManager.enforcedState.enforceForward = value
            }

            mc.options.backKey -> {
                TickStateManager.enforcedState.enforceBackward = value
            }

            mc.options.leftKey -> {
                TickStateManager.enforcedState.enforceLeft = value
            }

            mc.options.rightKey -> {
                TickStateManager.enforcedState.enforceRight = value
            }
        }
    }

val KeyBinding.opposite
    get() =
        when (this) {
            mc.options.forwardKey -> {
                mc.options.backKey
            }

            mc.options.backKey -> {
                mc.options.forwardKey
            }

            mc.options.leftKey -> {
                mc.options.rightKey
            }

            mc.options.rightKey -> {
                mc.options.leftKey
            }

            mc.options.useKey -> {
                mc.options.attackKey
            }

            mc.options.attackKey -> {
                mc.options.useKey
            }

            else -> {
                null
            }
        }

val moveKeys =
    mutableListOf<KeyBinding>(mc.options.forwardKey, mc.options.rightKey, mc.options.backKey, mc.options.leftKey)
