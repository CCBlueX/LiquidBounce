/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.boost
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.runAsyncDelayed
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard

// Original author: turtl (https://github.com/chocopie69/Liquidbounce-Scripts/blob/main/combat/superKB.js and https://github.com/CzechHek/Core/blob/master/Scripts/SuperKnock.js)
@ModuleInfo(name = "SuperKnockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module()
{
    /**
     * Mode
     */
    private val modeValue = ListValue("Mode", arrayOf("Packet", "Packet_W-Tap", "W-Tap", "SuperPacket", "Deprecated"), "Packet")

    /**
     * Hurt-time
     */
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    /**
     * Delay in ticks
     */
    private val ticksDelayValue = IntegerValue("TicksDelay", 0, 0, 60)

    /**
     * Exploits
     */
    private val exploitGroup = ValueGroup("Exploits")
    private val exploitNoMoveValue = object : BoolValue("NoMove", true, "NoMoveExploit") // NoMove is not applicable with W-Tap mode
    {
        override fun showCondition() = !modeValue.get().equals("W-Tap", ignoreCase = true)
    }
    private val exploitWTapNoMoveValue = object : BoolValue("NoMove_W-Tap", true, "NoMoveExploit_W-Tap")
    {
        override fun showCondition() = modeValue.get().equals("W-Tap", ignoreCase = true)
    }
    private val exploitNoSprintValue = BoolValue("NoSprint", true, "NoSprintExploit")

    private val notSprintingSlowdownValue = BoolValue("NotSprintingSlowdown", true)

    /**
     * Delay
     */
    private val delayValue = IntegerRangeValue("Delay", 45, 55, 1, 1000, "MaxDelay" to "MinDelay")

    /**
     * Multiplier
     */
    private val multiplierValue = FloatRangeValue("Multiplier", 2f, 2.3f, 1.1f, 3f, "MaxMultiplier" to "MinMultiplier")

    private val packetOverride = BoolValue("PacketOverride", true)

    private var knockTicks = 0
    private var superKnockback = false
    private var sprinting = false

    init
    {
        exploitGroup.addAll(exploitNoMoveValue, exploitWTapNoMoveValue, exploitNoSprintValue)
    }

    @EventTarget
    fun onAttack(event: AttackEvent)
    {
        val provider = classProvider

        val targetEntity = event.targetEntity

        if (modeValue.get().equals("Deprecated", ignoreCase = true) && targetEntity != null && provider.isEntityLivingBase(targetEntity))
        {
            if (targetEntity.asEntityLivingBase().hurtTime > hurtTimeValue.get()) return

            val thePlayer = mc.thePlayer ?: return
            val netHandler = mc.netHandler

            if (thePlayer.sprinting) netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

            netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))
            netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
            netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

            thePlayer.sprinting = true
            thePlayer.serverSprintState = true
        }
    }

    override fun onEnable()
    {
        knockTicks = 0
        superKnockback = false
        sprinting = false
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        if (knockTicks > 0) knockTicks -= 1
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val packet = event.packet

        val netHandler = mc.netHandler

        // Packet Override
        if ((classProvider.isCPacketPlayerPosition(packet) || classProvider.isCPacketPlayerLook(packet) || classProvider.isCPacketPlayerPosLook(packet)) && packetOverride.get())
        {
            val movePacket = packet.asCPacketPlayer()
            if (!movePacket.moving && (thePlayer.onGround || movePacket.onGround))
            {
                movePacket.x = thePlayer.posX
                movePacket.y = thePlayer.posY
                movePacket.z = thePlayer.posZ
                movePacket.moving = true
            }
        }
        else if (classProvider.isCPacketUseEntity(packet))
        {
            val mode = modeValue.get().toLowerCase()
            if (mode.equals("Deprecated", ignoreCase = true)) return

            val attackPacket = packet.asCPacketUseEntity()
            if (attackPacket.action == ICPacketUseEntity.WAction.ATTACK)
            {
                val target = attackPacket.getEntityFromWorld(theWorld)?.asEntityLivingBase() ?: return

                val gameSettings = mc.gameSettings

                val noMoveExploit = exploitNoMoveValue.get()

                val movementInput = thePlayer.isMoving
                val positionChanged = thePlayer.posX - thePlayer.lastTickPosX + thePlayer.posZ - thePlayer.lastTickPosZ == 0.0

                sprinting = thePlayer.sprinting
                superKnockback = (sprinting || exploitNoSprintValue.get()) && (movementInput || noMoveExploit)

                if (target.hurtTime <= hurtTimeValue.get() && knockTicks <= ticksDelayValue.get() && superKnockback)
                {
                    val notSprintingSlowdown = notSprintingSlowdownValue.get()

                    val delay = delayValue.getRandomLong()
                    val multipliedDelay = (delay * multiplierValue.getRandom()).toLong()

                    when (mode)
                    {
                        "packet" ->
                        {
                            // NoMove exploit

                            if (!movementInput && noMoveExploit)
                            {
                                if (!thePlayer.sprinting) thePlayer.sprinting = true

                                thePlayer.boost(1.0E-5F)
                            }

                            netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
                            netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                            // Restore the original sprinting state
                            if (!sprinting) runAsyncDelayed(1L) {
                                netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
                                thePlayer.sprinting = false
                            }
                        }

                        "superpacket" ->
                        {
                            if (!thePlayer.sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                            netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
                            netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                            // Restore the original sprinting state
                            if (!sprinting)
                            {
                                thePlayer.sprinting = false
                                runAsyncDelayed(1L) { netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING)) }
                            }
                        }

                        "w-tap" ->
                        {
                            if ((!movementInput || !positionChanged) && exploitWTapNoMoveValue.get())
                            {
                                // NoMove exploit for W-Tap

                                if (!sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                                runAsyncDelayed(delay) {
                                    netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

                                    if (notSprintingSlowdown && !thePlayer.sprinting) thePlayer.sprinting = false
                                }

                                // Restore the original sprinting state
                                if (sprinting) runAsyncDelayed(multipliedDelay) {
                                    netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                                    if (notSprintingSlowdown) thePlayer.sprinting = true
                                }
                            }
                            else
                            {
                                // A legit W-Tap
                                if (!sprinting && !thePlayer.sprinting) thePlayer.sprinting = true

                                // Stop sprinting
                                if (gameSettings.keyBindForward.pressed) gameSettings.keyBindForward.pressed = false

                                // Start sprinting after some delay
                                runAsyncDelayed(delay) {
                                    gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(gameSettings.keyBindForward.keyCode)
                                }

                                // Restore the original sprinting state
                                if (!sprinting) runAsyncDelayed(multipliedDelay) {
                                    if (thePlayer.sprinting) thePlayer.sprinting = false
                                }
                            }
                        }

                        "packet_w-tap" ->
                        {
                            // Start sprinting
                            if (!sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                            // Stop sprinting after some delay
                            runAsyncDelayed(delay) {
                                netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
                                if (notSprintingSlowdown && thePlayer.sprinting) thePlayer.sprinting = false
                            }

                            // Restore the original sprinting state
                            if (sprinting) runAsyncDelayed(multipliedDelay) {
                                netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

                                if (notSprintingSlowdown && !thePlayer.sprinting) thePlayer.sprinting = true
                            }
                        }
                    }
                }

                if (knockTicks == 0) knockTicks = ticksDelayValue.get()
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
