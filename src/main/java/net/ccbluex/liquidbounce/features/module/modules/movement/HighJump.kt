/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Tower
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.block.BlockPane
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import java.util.*

@ModuleInfo(name = "HighJump", description = "Allows you to jump higher.", category = ModuleCategory.MOVEMENT)
class HighJump : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Damage", "AAC3.0.1", "DAC", "Mineplex", "OldMineplex"), "Vanilla")

    private val baseHeightValue = object : FloatValue("Height", 2f, 1.1f, 5f)
    {
        override fun showCondition() = modeValue.get().equals("Vanilla", ignoreCase = true) || modeValue.get().equals("Damage", ignoreCase = true)
    }

    private val vanillaGlassValue = object : BoolValue("OnlyGlassPane", false)
    {
        override fun showCondition() = modeValue.get().equals("Vanilla", ignoreCase = true)
    }

    private val voidGroup = object : ValueGroup("Void")
    {
        override fun showCondition() = modeValue.get().equals("Damage", ignoreCase = true)
    }
    private val voidEnabledValue = BoolValue("Enabled", true)
    private val voidYValue = FloatValue("VoidY", -64F, 0F, -100F)

    private val mineplexHeightValue = object : FloatValue("MineplexHeight", 0.1f, 5.0f, 10.0f)
    {
        override fun showCondition() = modeValue.get().equals("Mineplex", ignoreCase = true)
    }
    private val autodisable = BoolValue("AutoDisable", true)
    private val autoDisableScaffoldValue = BoolValue("DisableScaffoldAndTower", true)

    private var jumped = false
    private var mineplexStage = 0

    init
    {
        voidGroup.addAll(voidEnabledValue, voidYValue)
    }

    override fun onEnable()
    {
        mineplexStage = -1
        jumped = false

        if (autoDisableScaffoldValue.get())
        {
            val moduleManager = LiquidBounce.moduleManager

            val scaffold = moduleManager[Scaffold::class.java]
            val tower = moduleManager[Tower::class.java]

            val disableScaffold = scaffold.state
            val disableTower = tower.state

            if (disableScaffold) scaffold.state = false
            if (disableTower) tower.state = false

            if (disableScaffold || disableTower) LiquidBounce.hud.addNotification(Notification(NotificationIcon.INFORMATION, "HighJump", "Disabled ${if (disableScaffold && disableTower) "Scaffold and Tower" else if (disableScaffold) "Scaffold" else "Tower"}", 1000))
        }

        if (modeValue.get().equals("mineplex", ignoreCase = true)) ClientUtils.displayChatMessage(mc.thePlayer, "\u00A78[\u00A7c\u00A7lMineplex Highjump\u00A78] \u00A7cWalk off an island to highjump.")
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val onGround = thePlayer.onGround

        if (onGround && jumped)
        {
            jumped = false
            if (autodisable.get()) state = false
        }

        if (vanillaGlassValue.get() && theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)) !is BlockPane) return // 'AAC Ground-check always returns true when player is collided with glass pane or iron bars, etc.' bug exploit

        when (modeValue.get().lowercase(Locale.getDefault()))
        {
            "damage" -> if (thePlayer.hurtTime > 0 && onGround)
            {
                thePlayer.motionY += 0.42f * baseHeightValue.get()
                if (autodisable.get()) state = false
            }
            else if (voidEnabledValue.get() && thePlayer.posY <= voidYValue.get())
            {
                thePlayer.motionY = 0.42 * baseHeightValue.get()
                if (autodisable.get()) state = false
            }

            "aac3.0.1" -> if (!onGround)
            {
                thePlayer.motionY += 0.059
                jumped = true
            }

            "dac" -> if (!onGround)
            {
                thePlayer.motionY += 0.049999
                jumped = true
            }

            "mineplex" -> mineplexHighJump(theWorld, thePlayer)

            "oldmineplex" -> if (!onGround) thePlayer.strafe(0.35f)
        }
    }

    private fun mineplexHighJump(theWorld: World, thePlayer: EntityPlayerSP)
    {
        val networkManager = mc.netHandler.networkManager

        val posY = thePlayer.posY

        val dir = thePlayer.moveDirectionRadians
        val nextX = thePlayer.posX - dir.sin * 0.45f
        val nextZ = thePlayer.posZ + dir.cos * 0.45f

        if (jumped) if (!thePlayer.onGround)
        {
            if (!thePlayer.isMoving) thePlayer.strafe(0.05f)

            thePlayer.strafe((0.55f - mineplexStage / 650.0f).coerceAtLeast(thePlayer.speed))
            mineplexStage++
        }

        if (thePlayer.onGround && theWorld.getCollidingBoundingBoxes(thePlayer, AxisAlignedBB(nextX - 0.3, posY - 1.0, nextZ - 0.3, nextX + 0.3, posY + 1.0, nextZ + 0.3)).isEmpty())
        {
            val noCollisionYOffset: Double = findNoCollisionYOffset(theWorld, thePlayer, nextX, posY, nextZ)
            if (noCollisionYOffset != 0.0)
            {
                mineplexStage = 1
                jumped = true
                networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(nextX, posY, nextZ, true))
                networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(nextX, posY + noCollisionYOffset, nextZ, true))
                thePlayer.setPosition(nextX, posY, nextZ)

                thePlayer.motionY = mineplexHeightValue.get().toDouble()
            }
        }
    }

    override fun onDisable()
    {
        (mc.thePlayer ?: return).strafe(0.2f)
        mc.timer.timerSpeed = 1.0f
    }

    @EventTarget
    fun onMove(@Suppress("UNUSED_PARAMETER") event: MoveEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (vanillaGlassValue.get() && theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)) !is BlockPane) return

        if (!thePlayer.onGround && modeValue.get().equals("oldmineplex", ignoreCase = true)) thePlayer.motionY += if (thePlayer.fallDistance == 0.0f) 0.0499 else 0.05
    }

    @EventTarget
    fun onJump(event: JumpEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (vanillaGlassValue.get() && theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)) !is BlockPane) return

        when (modeValue.get().lowercase(Locale.getDefault()))
        {
            "vanilla" ->
            {
                event.motion *= baseHeightValue.get()
                if (autodisable.get()) state = false
            }

            "oldmineplex" -> event.motion = 0.47f
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (event.packet is S08PacketPlayerPosLook && modeValue.get().equals("Mineplex", ignoreCase = true) && jumped)
        {
            val thePlayer = mc.thePlayer ?: return

            state = false

            thePlayer.zeroXZ()
            thePlayer.jumpMovementFactor = 0.02F

            LiquidBounce.hud.addNotification(Notification(NotificationIcon.CAUTION, "Disabled HighJump", "due setback", 1000L))
        }
    }

    private fun findNoCollisionYOffset(theWorld: World, thePlayer: Entity, x: Double, y: Double, z: Double): Double
    {
        var yOff = -1.5
        var bb: AxisAlignedBB

        do
        {
            bb = AxisAlignedBB(x - 0.3, y + yOff, z - 0.3, x + 0.3, y + 2 + yOff, z + 0.3)
            if (!theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -1.0, 0.0)).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -0.5, 0.0)).isEmpty() && yOff <= -4.5 || yOff <= -9) return yOff
            yOff -= 0.5
        } while (theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty())

        return 0.0
    }

    override val tag: String
        get() = modeValue.get()
}
