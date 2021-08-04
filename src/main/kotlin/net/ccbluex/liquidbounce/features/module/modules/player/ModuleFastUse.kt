package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.minecraft.item.MilkBucketItem
import net.minecraft.item.PotionItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * FastUse module
 *
 * Allows you to use items faster.
 */

object ModuleFastUse : Module("FastUse", Category.PLAYER) {

    private val modes = choices("Mode", Instant, arrayOf(Instant, NCP, AAC, Custom))
    private val noMove by boolean("NoMove", false)

    private object Instant : Choice("Instant") {
        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (!player.isUsingItem) {
                return@repeatable
            }
            if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                if (player.isUsingItem) {
                    repeat(35) {
                        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround))
                    }
                    player.stopUsingItem()
                }
            }
        }

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (noMove) {
                if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                    if (player.isUsingItem) {
                        event.movement.x = 0.0
                        event.movement.y = 0.0
                        event.movement.z = 0.0
                    }
                }
            }
        }
    }

    private object NCP : Choice("NCP") {
        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (!player.isUsingItem) {
                return@repeatable
            }
            if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                if (player.itemUseTime > 14) {
                    repeat(20) {
                        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround))
                    }
                    player.stopUsingItem()
                }
            }
        }

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (noMove) {
                if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                    if (player.isUsingItem) {
                        event.movement.x = 0.0
                        event.movement.y = 0.0
                        event.movement.z = 0.0
                    }
                }
            }
        }
    }

    private object AAC : Choice("AAC") {
        override val parent: ChoiceConfigurable
            get() = modes

        var usedTimer = false

        val repeatable = repeatable {
            if (usedTimer) {
                mc.timer.timerSpeed = 1F
                usedTimer = false
            }

            if (!player.isUsingItem) {
                return@repeatable
            }

            if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                if (player.isUsingItem) {
                    mc.timer.timerSpeed = 1.22f
                    usedTimer = true
                }
            }
        }

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (noMove) {
                if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                    if (player.isUsingItem) {
                        event.movement.x = 0.0
                        event.movement.y = 0.0
                        event.movement.z = 0.0
                    }
                }
            }
        }

        override fun disable() {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }
    }

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        val delay by int("Delay", 0, 0..10)
        val timer by float("Timer", 1f, 0.1f..5f)
        val speed by int("Speed", 2, 1..35)

        var usedTimer = false

        val repeatable = repeatable {
            if (usedTimer) {
                mc.timer.timerSpeed = 1F
                usedTimer = false
            }

            if (!player.isUsingItem) {
                return@repeatable
            }

            mc.timer.timerSpeed = timer
            usedTimer = true

            if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                if (player.isUsingItem) {
                    wait(delay)
                    repeat(speed) {
                        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround))
                    }
                    player.stopUsingItem()
                }
            }
        }

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (noMove) {
                if (player.activeItem.isFood || player.activeItem.item is MilkBucketItem || player.activeItem.item is PotionItem) {
                    if (player.isUsingItem) {
                        event.movement.x = 0.0
                        event.movement.y = 0.0
                        event.movement.z = 0.0
                    }
                }
            }
        }

        override fun disable() {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }
    }
}
