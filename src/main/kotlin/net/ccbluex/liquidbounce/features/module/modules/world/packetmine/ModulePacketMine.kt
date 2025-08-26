/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.CivMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.ImmediateMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.NormalMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.AlwaysToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.NeverToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.OnStopToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.PostStartToolMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import kotlin.math.max

/**
 * PacketMine module
 *
 * Automatically mines blocks you click once. Using AutoTool is recommended.
 *
 * @author ccetl
 */
@Suppress("TooManyFunctions")
object ModulePacketMine : ClientModule("PacketMine", Category.WORLD) {

    val mode = choices(
        this,
        "Mode",
        NormalMineMode,
        arrayOf(NormalMineMode, ImmediateMineMode, CivMineMode)
    ).apply {
        tagBy(this)
    }

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChange {
        it.coerceAtLeast(range)
    }

    val keepRange by float("KeepRange", 25f, 0f..200f).onChange {
        it.coerceAtLeast(wallsRange)
    }

    val swingMode by enumChoice("Swing", SwingMode.HIDE_CLIENT)
    val switchMode = choices("Switch",
        OnStopToolMode,
        arrayOf(AlwaysToolMode, PostStartToolMode, OnStopToolMode, NeverToolMode)
    )

    private val rotationMode by enumChoice("Rotate", MineRotationMode.NEVER)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    val breakDamage by float("BreakDamage", 1f, 0f..2f)
    val abortAlwaysDown by boolean("AbortAlwaysDown", false)
    private val selectDelay by int("SelectDelay", 200, 0..400, "ms")

    val targetRenderer = tree(
        PlacementRenderer(
            "TargetRendering", true, this,
            defaultColor = Color4b(255, 255, 0, 90),
            clump = false
        )
    )

    private val chronometer = Chronometer()
    private var rotation: Rotation? = null

    /**
     * The current target of the module.
     *
     * Should never be accessed directly by other modules!
     */
    @Suppress("ObjectPropertyName", "ObjectPropertyNaming")
    var _target: MineTarget? = null // yes "_" because kotlin lacks package private
        set(value) { // and I don't want to offer this to modules using this to mine something
            if (value == field) {
                return
            }

            field?.cleanUp()
            value?.init()
            field = value
        }

    init {
        mode.onChanged {
            if (mc.world != null && mc.player != null) {
                onDisabled()
                onEnabled()
            }
        }
    }

    override fun onEnabled() {
        interaction.cancelBlockBreaking()
    }

    override fun onDisabled() {
        targetRenderer.clearSilently()
        _target = null
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        val mineTarget = _target ?: return@handler
        mineTarget.updateBlockState()
        rotate(mineTarget)
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        val mineTarget = _target ?: return@tickHandler
        if (mineTarget.isInvalidOrOutOfRange()) {
            _target = null
            return@tickHandler
        }

        mineTarget.updateBlockState()
        handleBreaking(mineTarget)
    }

    private fun rotate(mineTarget: MineTarget) {
        val rotate = rotationMode.shouldRotate(mineTarget)

        val raytrace = raytraceBlock(
            player.eyePos,
            mineTarget.targetPos,
            mineTarget.blockState,
            range = range.toDouble(),
            wallsRange = wallsRange.toDouble()
        ) ?: run {
            // don't do actions when the block is out of range
            mineTarget.abort()
            return
        }

        if (rotate) {
            RotationManager.setRotationTarget(
                raytrace.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotationsConfigurable,
                Priority.IMPORTANT_FOR_USAGE_2,
                ModulePacketMine
            )
        }

        rotation = raytrace.rotation
    }

    private fun handleBreaking(mineTarget: MineTarget) {
        // are we looking at the target?
        val hit = raytraceBlock(
            max(range, wallsRange).toDouble(),
            RotationManager.serverRotation,
            mineTarget.targetPos,
            mineTarget.blockState
        )

        val invalidHit = hit == null || hit.type != HitResult.Type.BLOCK || hit.blockPos != mineTarget.targetPos
        if (invalidHit && rotationMode.getFailProcedure(mineTarget).execute(mineTarget)) {
            return
        }

        mineTarget.direction = raytraceBlock(
            max(range, wallsRange).toDouble() + 1.0,
            rotation = rotation ?: return,
            pos = mineTarget.targetPos,
            state = mineTarget.blockState
        )?.side ?: run {
            // wrong rotations?? this should not happen!
            FailProcedure.ABORT.execute(mineTarget)
            return
        }

        if (player.isCreative) {
            interaction.sendSequencedPacket(net.ccbluex.liquidbounce.utils.client.world) { sequence: Int ->
                interaction.breakBlock(mineTarget.targetPos)
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    mineTarget.targetPos,
                    mineTarget.direction,
                    sequence
                )
            }

            swingMode.swing(Hand.MAIN_HAND)
            return
        }

        val switchMode = switchMode.activeChoice
        val slot = switchMode.getSlot(mineTarget.blockState)
        if (!mineTarget.started) {
            startBreaking(slot, mineTarget)
        } else if (mode.activeChoice.shouldUpdate(mineTarget, slot)) {
            updateBreakingProgress(mineTarget, slot)
            if (mineTarget.progress >= breakDamage && !mineTarget.finished) {
                mode.activeChoice.finish(mineTarget)
                switchMode.getSwitchingMethod().switchBack()
            }
        }

        switchMode.getSwitchingMethod().reset()
    }

    private fun startBreaking(slot: IntObjectImmutablePair<ItemStack>?, mineTarget: MineTarget) {
        switch(slot, mineTarget)
        if (switchMode.activeChoice.syncOnStart) {
            interaction.syncSelectedSlot()
        }

        mode.activeChoice.start(mineTarget)
        mineTarget.started = true
    }

    private fun updateBreakingProgress(mineTarget: MineTarget, slot: IntObjectImmutablePair<ItemStack>?) {
        val switchMode = switchMode.activeChoice
        mineTarget.progress += switchMode.getBlockBreakingDelta(
            mineTarget.targetPos,
            mineTarget.blockState,
            slot?.second()
        )

        switch(slot, mineTarget)
        if (switchMode.getSwitchingMethod().shouldSync) {
            interaction.syncSelectedSlot()
        }

        val f = if (breakDamage > 0f) {
            val breakDamageD = breakDamage.toDouble()
            mineTarget.progress.toDouble().coerceIn(0.0..breakDamageD) / breakDamageD / 2.0
        } else {
            0.5
        }

        val box = mineTarget.targetPos.outlineBox
        val lengthX = box.lengthX
        val lengthY = box.lengthY
        val lengthZ = box.lengthZ
        targetRenderer.updateBox(
            mineTarget.targetPos,
            box.expand(
                -(lengthX / 2) + lengthX * f,
                -(lengthY / 2) + lengthY * f,
                -(lengthZ / 2) + lengthZ * f
            )
        )
    }

    fun switch(slot: IntObjectImmutablePair<ItemStack>?, mineTarget: MineTarget) {
        if (slot == null) {
            return
        }

        val switchMode = switchMode.activeChoice
        if (switchMode.shouldSwitch(mineTarget)) {
            switchMode.getSwitchingMethod().switch(slot, mineTarget)
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        val openScreen = mc.currentScreen != null
        val unchangeableActive = !mode.activeChoice.canManuallyChange && _target != null
        if (openScreen || unchangeableActive || !player.abilities.allowModifyWorld) {
            return@handler
        }

        val isLeftClick = event.button == 0
        // without adding a little delay before being able to unselect / select again, selecting would be impossible
        val hasTimePassed = chronometer.hasElapsed(selectDelay.toLong())
        val hitResult = mc.crosshairTarget
        if (!isLeftClick || !hasTimePassed || hitResult == null || hitResult !is BlockHitResult) {
            return@handler
        }

        val blockPos = hitResult.blockPos
        val state = blockPos.getState()!!

        val shouldTargetBlock = mode.activeChoice.shouldTarget(blockPos, state)
        // stop when the block is clicked again
        val isCancelledByUser = blockPos.equals(_target?.targetPos)

        _target = if (shouldTargetBlock && world.worldBorder.contains(blockPos) && !isCancelledByUser) {
            MineTarget(blockPos)
        } else {
            null
        }

        chronometer.reset()
    }

    @Suppress("unused")
    private val blockAttackHandler = handler<BlockAttackEvent> {
        it.cancelEvent()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        _target = null
    }

    @Suppress("unused")
    private val blockUpdateHandler = handler<PacketEvent> {
        if (!mode.activeChoice.stopOnStateChange) {
            return@handler
        }

        when (val packet = it.packet) {
            is BlockUpdateS2CPacket -> {
                mc.renderTaskQueue.add { updatePosOnChange(packet.pos, packet.state) }
            }

            is ChunkDeltaUpdateS2CPacket -> {
                mc.renderTaskQueue.add {
                    packet.visitUpdates { pos, state -> updatePosOnChange(pos, state) }
                }
            }
        }
    }

    private fun updatePosOnChange(pos: BlockPos, state: BlockState) {
        if (pos == _target?.targetPos && state.isAir) {
            _target = null
        }
    }

    fun setTarget(blockPos: BlockPos) {
        if (_target?.finished != false && mode.activeChoice.canManuallyChange || _target == null) {
            _target = MineTarget(blockPos)
        }
    }

    @Suppress("FunctionNaming", "FunctionName")
    fun _resetTarget() {
        _target = null
    }

}
