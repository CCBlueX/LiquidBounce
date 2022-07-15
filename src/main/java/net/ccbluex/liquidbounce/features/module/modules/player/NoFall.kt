/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.AAC3_3_4
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.MLG
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet.*
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.extensions.collideBlock
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.BlockLiquid
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB

/**
 * TODO
 * * https://forum.ccbluex.net/thread.php?id=2530#p14704
 * * https://forums.ccbluex.net/topic/556/switchnofall-betternofall-recode/3
 * * https://forums.ccbluex.net/topic/787/blinkfall-5-1?_=1635005507761&lang=ko
 * * Add web, ladder, bed, boat, minecart, etc. support
 */
@ModuleInfo(name = "NoFall", description = "Prevents you from taking fall damage.", category = ModuleCategory.PLAYER)
object NoFall : Module()
{
    private val nofallModes = arrayOf(SpoofGround(), NoGround(), Packet(), MLG(), AAC3_1_0(), AAC3_3_4(), AAC3_3_11(), AAC3_3_15(), Spartan194(), CubeCraft(), Hypixel(), ACP(), PacketAAC(), BetterAAC(), BetterAAC2(), Verus(), Spigot())

    private val nofallModeMap = mapOf(*nofallModes.map { it.modeName to it }.toTypedArray())

    @JvmField
    val modeValue = object : ListValue("Mode", nofallModeMap.keys.toTypedArray(), "SpoofGround")
    {
        override fun onChange(oldValue: String, newValue: String)
        {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String)
        {
            if (state) onEnable()
        }
    }

    private val noVoidValue = BoolValue("NoVoid", true)

    val noSpoofTicks = object : IntegerValue("NoSpoofTicks", 0, 0, 5)
    {
        override fun showCondition() = modeValue.get().equals("SpoofGround", ignoreCase = true) || modeValue.get().equals("Packet", ignoreCase = true)
    }

    val thresholdFallDistanceValue = FloatValue("ThresholdFallDistance", 1.5f, 0f, 2.9f)

    private val mlgGroup = object : ValueGroup("MLG")
    {
        override fun showCondition() = modeValue.get().equals("MLG", ignoreCase = true)
    }
    val mlgMinFallDistance = FloatValue("MinHeight", 5f, 2f, 50f, "MinMLGHeight")
    val mlgSilentRotationValue = BoolValue("SilentRotation", true, "SilentRotation")

    private val mlgKeepRotationGroup = ValueGroup("KeepRotation")
    val mlgKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
    val mlgKeepRotationTicksValue = IntegerValue("Ticks", 1, 1, 40, "KeepRotationLength")

    internal var jumped = false
    internal var noSpoof = 0
    internal var groundFallDistance = 0f

    init
    {
        mlgKeepRotationGroup.addAll(mlgKeepRotationEnabledValue, mlgKeepRotationTicksValue)
        mlgGroup.addAll(mlgMinFallDistance, mlgSilentRotationValue, mlgKeepRotationGroup)
    }

    private var mode: NoFallMode? = null

    override val tag: String
        get() = "${modeValue.get()}${if (modeValue.get().equals("SpoofGround", ignoreCase = true) || modeValue.get().equals("Packet", ignoreCase = true)) " ${noSpoofTicks.get()}" else ""}"

    @EventTarget(ignoreCondition = true)
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (checkNoVoid(thePlayer) || thePlayer.onGround || thePlayer.motionY > 0)
        {
            noSpoof = 0
            jumped = false
        }

        if (canApplyNoFall(thePlayer, theWorld, thePlayer.entityBoundingBox))
        {
            noSpoof = 0
            return
        }

        if (thePlayer.onGround || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isOnLadder || thePlayer.isInWeb) groundFallDistance = thePlayer.fallDistance - 0.25f

        mode?.onUpdate()
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val packet = event.packet
        val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
        mode?.onPacket(event)
        if (packet is C03PacketPlayer && !checkNoVoid(thePlayer) && !(fly.state && fly.shouldDisableNoFall)) mode?.onMovePacket(packet)?.let { packet.onGround = it }
        if (packet is S08PacketPlayerPosLook) groundFallDistance = thePlayer.fallDistance - 0.25f
    }

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val playerBB = thePlayer.entityBoundingBox
        val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
        if (checkNoVoid(thePlayer) || fly.state && fly.shouldDisableNoFall || theWorld.collideBlock(playerBB) { it.block is BlockLiquid } || theWorld.collideBlock(AxisAlignedBB(playerBB.minX, playerBB.minY - 0.01, playerBB.minZ, playerBB.maxX, playerBB.maxY, playerBB.maxZ)) { it.block is BlockLiquid }) return

        mode?.onMove(event)
    }

    @EventTarget
    private fun onMotionUpdate(event: MotionEvent)
    {
        if (!checkNoVoid(mc.thePlayer ?: return)) mode?.onMotion(event.eventState)
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(@Suppress("UNUSED_PARAMETER") event: JumpEvent)
    {
        jumped = true
    }

    private fun checkNoVoid(thePlayer: EntityPlayerSP): Boolean = noVoidValue.get() && thePlayer.posY < 0

    private fun canApplyNoFall(thePlayer: EntityPlayerSP, theWorld: WorldClient, playerBB: AxisAlignedBB): Boolean
    {
        val moduleManager = LiquidBounce.moduleManager
        val fly = moduleManager[Fly::class.java] as Fly
        return !state || moduleManager[FreeCam::class.java].state || fly.state && fly.shouldDisableNoFall || thePlayer.isSpectator || thePlayer.capabilities.allowFlying || thePlayer.capabilities.disableDamage || theWorld.collideBlock(playerBB) { it.block is BlockLiquid } || theWorld.collideBlock(AxisAlignedBB(playerBB.minX, playerBB.minY - 0.01, playerBB.minZ, playerBB.maxX, playerBB.maxY, playerBB.maxZ)) { it.block is BlockLiquid }
    }
}
