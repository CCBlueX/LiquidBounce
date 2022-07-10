/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.canBeClicked
import net.ccbluex.liquidbounce.utils.extensions.distanceToCenter
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.searchBlocks
import net.ccbluex.liquidbounce.utils.extensions.times
import net.ccbluex.liquidbounce.utils.extensions.vec
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BlockValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatRangeValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.RGBAColorValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "ChestAura", description = "Automatically opens chests around you.", category = ModuleCategory.WORLD)
object ChestAura : Module()
{
    private val chestGroup = ValueGroup("Chest")
    private val chestFirstValue = BlockValue("First", Block.getIdFromBlock(Blocks.chest), "Chest")
    private val chestSecondValue = BlockValue("Second", Block.getIdFromBlock(Blocks.air))
    private val chestThirdValue = BlockValue("Third", Block.getIdFromBlock(Blocks.air))

    private val rangeValue = FloatValue("Range", 5F, 1F, 6F)
    private val priorityValue = ListValue("Priority", arrayOf("Distance", "ServerDirection", "ClientDirection"), "Distance")
    private val delayValue = IntegerRangeValue("Delay", 100, 100, 50, 200, "MaxDelay" to "MinDelay")

    private val rotationGroup = ValueGroup("Rotation")
    private val rotationEnabledValue = BoolValue("Enabled", true, "Rotations")
    private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
    private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

    private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
    private val rotationKeepRotationEnabledValue = BoolValue("Enabled", true, "KeepRotation")
    private val rotationKeepRotationTicksValue = IntegerRangeValue("Ticks", 20, 30, 0, 60, "MaxKeepRotationTicks" to "MinKeepRotationTicks")

    private val throughWallsValue = BoolValue("ThroughWalls", true)
    private val visualSwing = BoolValue("VisualSwing", true)
    private val noHitValue = BoolValue("KillAuraBypass", true, "NoHit")

    private val visualMarkGroup = ValueGroup("Mark")
    private val visualMarkEnabledValue = BoolValue("Enabled", false)
    private val visualMarkLineWidthValue = FloatValue("LineWidth", 1f, 0.5f, 2f)
    private val visualMarkAccuracyValue = FloatValue("Accuracy", 10F, 0.5F, 20F)
    private val visualMarkFadeSpeedValue = IntegerValue("FadeSpeed", 5, 1, 9)
    private val visualMarkColorValue = RGBAColorValue("Color", 255, 128, 0, 255)

    var currentBlock: BlockPos? = null

    private val timer = MSTimer()
    private var delay = delayValue.getRandomLong()

    val clickedBlocks = mutableListOf<BlockPos>()

    private var facesBlock = false

    private var easingRange = 0f

    init
    {
        chestGroup.addAll(chestFirstValue, chestSecondValue, chestThirdValue)
        rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationTicksValue)
        rotationGroup.addAll(rotationEnabledValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationKeepRotationGroup)
        visualMarkGroup.addAll(visualMarkEnabledValue, visualMarkLineWidthValue, visualMarkAccuracyValue, visualMarkFadeSpeedValue, visualMarkColorValue)
    }

    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        val moduleManager = LiquidBounce.moduleManager
        if (moduleManager[Blink::class.java].state || (noHitValue.get() && (moduleManager[KillAura::class.java] as KillAura).hasTarget)) return

        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        when (event.eventState)
        {
            EventState.PRE ->
            {
                if (mc.currentScreen is GuiContainer) timer.reset() // No delay re-randomize code here because the performance impact is more than your think.

                val range = rangeValue.get()
                val radius = range + 1
                val eyesPos = Vec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)

                val throughWalls = throughWallsValue.get()

                val prioritySelector = { blockPos: BlockPos ->
                    when (priorityValue.get().toLowerCase())
                    {
                        "serverdirection" -> RotationUtils.getServerRotationDifference(thePlayer, blockPos, false, RotationUtils.MinMaxPair.ZERO)
                        "clientdirection" -> RotationUtils.getClientRotationDifference(thePlayer, blockPos, false, RotationUtils.MinMaxPair.ZERO)
                        else -> thePlayer.distanceToCenter(blockPos)
                    }
                }

                currentBlock = theWorld.searchBlocks(thePlayer, radius.toInt()).asSequence().filter { (_, block) ->
                    val id = Block.getIdFromBlock(block)
                    arrayOf(chestFirstValue.get(), chestSecondValue.get(), chestThirdValue.get()).filterNot { it == 0 }.any { it == id }
                }.filter { !clickedBlocks.contains(it.key) }.filter { thePlayer.distanceToCenter(it.key) < range }.run {
                    if (!throughWalls) this else filter { (pos, _) -> (theWorld.rayTraceBlocks(eyesPos, pos.vec, false, true, false) ?: return@filter false).blockPos == pos }
                }.minByOrNull { prioritySelector(it.key) }?.key

                if (rotationEnabledValue.get())
                {
                    val currentBlock = currentBlock ?: return
                    val vecRotation = RotationUtils.faceBlock(theWorld, thePlayer, currentBlock) ?: return
                    val rotation = vecRotation.rotation
                    val posVec = vecRotation.vec

                    val keepRotationTicks = if (rotationKeepRotationEnabledValue.get()) rotationKeepRotationTicksValue.getRandom() else 0

                    if (rotationTurnSpeedValue.getMin() < 180)
                    {
                        val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, rotationTurnSpeedValue.getRandomStrict(), 0.0F)
                        RotationUtils.setTargetRotation(limitedRotation, keepRotationTicks)
                        RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

                        facesBlock = false

                        if (!theWorld.canBeClicked(currentBlock)) return

                        if (!throughWalls && (eyesPos.squareDistanceTo(posVec) > 18.0 || run {
                                val rayTrace = theWorld.rayTraceBlocks(eyesPos, posVec, false, true, false)

                                rayTrace == null || rayTrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || rayTrace.blockPos != currentBlock
                            })) return

                        val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
                        val vector = eyesPos + rotationVector * range.toDouble()
                        val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)

                        if (rayTrace != null && (rayTrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || rayTrace.blockPos != currentBlock)) return

                        facesBlock = true
                    }
                    else
                    {
                        RotationUtils.setTargetRotation(rotation, keepRotationTicks)
                        RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

                        facesBlock = true
                    }
                }
            }

            EventState.POST -> if (currentBlock != null && (!rotationEnabledValue.get() || facesBlock) && timer.hasTimePassed(delay))
            {
                val currentBlock = currentBlock ?: return

                CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

                if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.heldItem, currentBlock, EnumFacing.DOWN, currentBlock.vec))
                {
                    if (visualSwing.get()) thePlayer.swingItem() else mc.netHandler.addToSendQueue(C0APacketAnimation())

                    clickedBlocks.add(currentBlock)
                    this.currentBlock = null
                    timer.reset()
                    delay = delayValue.getRandomLong()
                }
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        if (!visualMarkEnabledValue.get() || !state && easingRange <= 0f) return

        GL11.glPushMatrix()
        RenderUtils.drawRadius(easingRange, visualMarkAccuracyValue.get(), visualMarkLineWidthValue.get(), visualMarkColorValue.get())
        GL11.glPopMatrix()

        easingRange = easeOutCubic(easingRange, if (state) rangeValue.get() else 0f, visualMarkFadeSpeedValue.get())
        if (!state && easingRange <= 0.1f) easingRange = 0f
    }

    override fun onDisable()
    {
        clickedBlocks.clear()
        facesBlock = false
    }

    override val tag: String
        get() = "${rangeValue.get()}"
}
