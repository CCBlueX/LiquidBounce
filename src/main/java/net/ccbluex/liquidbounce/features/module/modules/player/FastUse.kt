/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.AsyncUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.item.Item
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.Timer
import kotlin.math.ceil

@ModuleInfo(name = "FastUse", description = "Allows you to use items faster.", category = ModuleCategory.PLAYER)
class FastUse : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Instant", "NCP", "AAC", "Custom"), "NCP")

    private val ncpGroup = object : ValueGroup("NCP")
    {
        override fun showCondition() = modeValue.get().equals("NCP", ignoreCase = true)
    }
    private val ncpModeValue = ListValue("Mode", arrayOf("AtOnce", "Constant"), "AtOnce", "NCP-Mode")

    private val ncpAtOnceGroup = object : ValueGroup("AtOnce")
    {
        override fun showCondition() = ncpModeValue.get().equals("AtOnce", ignoreCase = true)
    }
    private val ncpAtOnceWaitTicksValue = IntegerValue("WaitTicks", 14, 0, 25, "NCP-AtOnce-WaitTicks")
    private val ncpAtOncePacketsValue = IntegerValue("Packets", 20, 12, 100, "NCP-AtOnce-Packets")

    private val ncpConstantGroup = object : ValueGroup("Constant")
    {
        override fun showCondition() = ncpModeValue.get().equals("Constant", ignoreCase = true)
    }
    private val ncpConstantPacketsValue = IntegerValue("Packets", 1, 1, 10, "NCP-Constant-Packets")

    private val ncpTimerValue = FloatValue("Timer", 1.0f, 0.2f, 1.5f, "NCP-Timer")

    private val aacTimerValue = object : FloatValue("AACTimer", 1.22f, 1.1f, 1.5f, "AAC-Timer")
    {
        override fun showCondition() = modeValue.get().equals("AAC", ignoreCase = true)
    }

    private val customGroup = object : ValueGroup("Custom")
    {
        override fun showCondition() = modeValue.get().equals("Custom", ignoreCase = true)
    }
    private val customDelayValue = IntegerValue("Delay", 0, 0, 300, "CustomDelay")
    private val customSpeedValue = IntegerValue("Speed", 2, 1, 35, "CustomSpeed")
    private val customTimer = FloatValue("Timer", 1.1f, 0.5f, 2f, "CustomTimer")

    private val noMoveValue = BoolValue("NoMove", false)

    private val msTimer = MSTimer()
    private var usedTimer = false

    init
    {
        ncpAtOnceGroup.addAll(ncpAtOnceWaitTicksValue, ncpAtOncePacketsValue)
        ncpConstantGroup.add(ncpConstantPacketsValue)
        ncpGroup.addAll(ncpModeValue, ncpAtOnceGroup, ncpConstantGroup, ncpTimerValue)

        customGroup.addAll(customDelayValue, customSpeedValue, customTimer)
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        perform(mc.thePlayer ?: return, mc.timer)
    }

    fun perform(thePlayer: EntityPlayerSP, timer: Timer, customItem: Item? = null, usingItemTicks: Int? = null): Int
    {
        if (!state) return 32

        if (usedTimer)
        {
            timer.timerSpeed = 1F
            usedTimer = false
        }

        if (customItem == null && !thePlayer.isUsingItem)
        {
            msTimer.reset()
            return -1
        }
        val itemInUse = customItem ?: thePlayer.itemInUse?.item
        val itemInUseDuration = usingItemTicks ?: thePlayer.itemInUseDuration

        if (itemInUse is ItemFood || itemInUse is ItemBucketMilk || itemInUse is ItemPotion)
        {
            val workers = AsyncUtils.workers

            val netHandler = mc.netHandler
            val onGround = thePlayer.onGround

            when (modeValue.get().toLowerCase())
            {
                "instant" ->
                {
                    repeat(35) {
                        netHandler.addToSendQueue(C03PacketPlayer(onGround))
                    }

                    mc.playerController.onStoppedUsingItem(thePlayer)

                    return 0
                }

                "ncp" ->
                {
                    timer.timerSpeed = ncpTimerValue.get()

                    usedTimer = true

                    when (ncpModeValue.get().toLowerCase())
                    {
                        "atonce" ->
                        {
                            if (itemInUseDuration > ncpAtOnceWaitTicksValue.get())
                            {
                                repeat(ncpAtOncePacketsValue.get()) {
                                    netHandler.addToSendQueue(C03PacketPlayer(onGround))
                                }

                                mc.playerController.onStoppedUsingItem(thePlayer)
                            }

                            return ncpAtOnceWaitTicksValue.get() + 2
                        }

                        "constant" ->
                        {
                            repeat(ncpConstantPacketsValue.get()) {
                                netHandler.addToSendQueue(C03PacketPlayer(onGround))
                            }

                            return 32 / (ncpConstantPacketsValue.get() + 1)
                        }
                    }
                }

                "aac" ->
                {
                    timer.timerSpeed = aacTimerValue.get()
                    usedTimer = true

                    return 32
                }

                "custom" ->
                {
                    timer.timerSpeed = customTimer.get()
                    usedTimer = true

                    if (msTimer.hasTimePassed(customDelayValue.get().toLong()))
                    {
                        workers.execute {
                            repeat(customSpeedValue.get()) {
                                netHandler.addToSendQueue(C03PacketPlayer(onGround))
                            }
                        }

                        msTimer.reset()
                    }

                    return ceil(32.0F / ((customSpeedValue.get().toFloat() + 1) * (1600.0F * (customDelayValue.get().toFloat() / 1600.0F)))).coerceAtMost(32.0F).toInt()
                }
            }
        }

        return -1
    }

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (!state || !thePlayer.isUsingItem || !noMoveValue.get()) return

        val usingItem = (thePlayer.itemInUse ?: return).item

        if (usingItem is ItemFood || usingItem is ItemBucketMilk || usingItem is ItemPotion) event.zero()
    }

    override fun onDisable()
    {
        if (usedTimer)
        {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }
    }

    override val tag: String
        get() = "${modeValue.get()}${if (modeValue.get().equals("NCP", ignoreCase = true)) "-${ncpModeValue.get()}" else ""}"
}
