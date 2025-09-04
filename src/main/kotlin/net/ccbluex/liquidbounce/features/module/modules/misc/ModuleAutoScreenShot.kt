package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.OverlayTitleEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.session.GameWins
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.text.Text
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ModuleAutoScreenShot : ClientModule("AutoScreenShot", Category.MISC) {

    private val perspectives = multiEnumChoice(
        "Perspective",
        Perspective.FIRST_PERSON,
        Perspective.THIRD_PERSON_FRONT,
        canBeNone = false
    )

    @Suppress("Unused")
    enum class Perspective(override val choiceName: String) : NamedChoice {
        FIRST_PERSON("FirstPerson"),
        THIRD_PERSON_BACK("ThirdPersonBack"),
        THIRD_PERSON_FRONT("ThirdPersonFront")
    }

    private val swordBlock by boolean("SwordBlock", true)
    private val autoSwitchSword by boolean("AutoSwitchSword", true)
    private val sneaking by boolean("Sneaking", false)

    private val delayBetweenShots by int("DelayBetween", 100, 100..1000, "ms")
    private val holdDuration by int("HoldDuration", 50, 50..500, "ms")

    private var lastWinCount = 0
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var isProcessing = false
    private var isSneaking = false
    private var isBlocking = false
    private var originalSlot = 0

    init {
        handler<PerspectiveEvent> { event ->
            if (isProcessing) {
                return@handler
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<OverlayTitleEvent> {
        val currentWins = GameWins.victoryCount

        if (currentWins > lastWinCount && !isProcessing) {
            lastWinCount = currentWins
            startScreenshotSequence()
        }
    }

    private fun startScreenshotSequence() {
        isProcessing = true
        originalSlot = player.inventory.selectedSlot

        val originalState = OriginalState(
            mc.options.perspective,
            mc.options.sneakKey.isPressed,
            isHoldingSword() && mc.options.useKey.isPressed,
            originalSlot
        )

        var delay = 100L

        Perspective.entries.forEach { perspective ->
            if (perspective in perspectives) {
                schedulePerspectiveShot(perspective, originalState, delay)
                delay += holdDuration * 2 + delayBetweenShots
            }
        }

        scheduleCleanup(originalState, delay)
    }

    private data class OriginalState(
        val perspective: net.minecraft.client.option.Perspective?,
        val sneaking: Boolean,
        val blocking: Boolean,
        val slot: Int
    )

    private fun schedulePerspectiveShot(perspective: Perspective, originalState: OriginalState, baseDelay: Long) {
        schedulePerspectiveChange(perspective, baseDelay)
        scheduleScreenshotCapture(perspective, baseDelay + holdDuration)
        scheduleStateReset(originalState, baseDelay + holdDuration * 2)
    }

    private fun schedulePerspectiveChange(perspective: Perspective, delay: Long) {
        executor.schedule({
            mc.execute {
                // Switch to sword first if needed
                if (swordBlock && autoSwitchSword && !isHoldingSword()) {
                    (0..8).firstOrNull { i ->
                        player.inventory.getStack(i).let {
                            it.item is SwordItem || it.item == Items.NETHERITE_SWORD
                        }
                    }?.let { slot ->
                        player.inventory.selectedSlot = slot
                        interaction.syncSelectedSlot()
                    }
                }
                mc.options.perspective = when (perspective) {
                    Perspective.FIRST_PERSON -> net.minecraft.client.option.Perspective.FIRST_PERSON
                    Perspective.THIRD_PERSON_BACK -> net.minecraft.client.option.Perspective.THIRD_PERSON_BACK
                    Perspective.THIRD_PERSON_FRONT -> net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT
                }

                if (sneaking) {
                    mc.options.sneakKey.isPressed = true
                    isSneaking = true
                }

                if (swordBlock && isHoldingSword()) {
                    mc.options.useKey.isPressed = true
                    isBlocking = true
                }
            }
        }, delay, TimeUnit.MILLISECONDS)
    }
    private fun scheduleScreenshotCapture(perspective: Perspective, delay: Long) {
        executor.schedule({
            mc.execute {
                takeScreenshot(perspective.choiceName)
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun scheduleStateReset(originalState: OriginalState, delay: Long) {
        executor.schedule({
            mc.execute {
                if (sneaking) {
                    mc.options.sneakKey.isPressed = originalState.sneaking
                    isSneaking = false
                }

                if (swordBlock && isBlocking) {
                    mc.options.useKey.isPressed = originalState.blocking
                    isBlocking = false
                }
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun scheduleCleanup(originalState: OriginalState, delay: Long) {
        executor.schedule({
            mc.execute {
                mc.options.perspective = originalState.perspective
                player.inventory.selectedSlot = originalState.slot
                interaction.syncSelectedSlot()

                if (isSneaking) {
                    network.sendPacket(
                        ClientCommandC2SPacket(
                            player,
                            ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
                        )
                    )
                    isSneaking = false
                }

                if (isBlocking) {
                    mc.options.useKey.isPressed = originalState.blocking
                    isBlocking = false
                }

                mc.inGameHud.setTitle(Text.literal("Screenshots captured!"))
                isProcessing = false
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun takeScreenshot(perspectiveName: String) {
        val timestamp = System.currentTimeMillis()
        val fileName = "win_${GameWins.victoryCount}_${perspectiveName}_$timestamp.png"

        ScreenshotRecorder.saveScreenshot(
            mc.runDirectory,
            fileName,
            mc.framebuffer
        ) { text: Text ->
            mc.execute {
                mc.inGameHud.setTitle(text)
            }
        }
    }

    private fun isHoldingSword(): Boolean {
        val mainHand: ItemStack = player.mainHandStack
        return mainHand.item is SwordItem || mainHand.item == Items.NETHERITE_SWORD
    }

    override fun onEnabled()  {
        lastWinCount = GameWins.victoryCount
    }

    override fun onDisabled() {
        executor.shutdownNow()
        if (isSneaking) {
            network.sendPacket(
                ClientCommandC2SPacket(
                    player,
                    ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
                )
            )
            isSneaking = false
        }
        isBlocking = false
    }
}
