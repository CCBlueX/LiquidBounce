/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.CivMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.ImmediateMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.NormalMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.AlwaysToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.NeverToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.OnStopToolMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool.PostStartToolMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.max

/**
 * PacketMine module
 *
 * Automatically mines blocks you click once. Using AutoTool is recommended.
 *
 * @author ccetl
 */
@Suppress("TooManyFunctions")
object ModulePacketMine : ClientModule("PacketMine", ModuleCategories.WORLD) {

    val mode = modes(
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
    private val rotations = tree(RotationsValueGroup(this))
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
            if (mc.level != null && mc.player != null) {
                onDisabled()
                onEnabled()
            }
        }
    }

    override fun onEnabled() {
        interaction.stopDestroyBlock()
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

        val raytrace = raytraceBlockRotation(
            player.eyePosition,
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
                valueGroup = rotations,
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
        )?.direction ?: run {
            // wrong rotations?? this should not happen!
            FailProcedure.ABORT.execute(mineTarget)
            return
        }

        if (player.isCreative) {
            interaction.startPrediction(net.ccbluex.liquidbounce.utils.client.world) { sequence: Int ->
                interaction.destroyBlock(mineTarget.targetPos)
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    mineTarget.targetPos,
                    mineTarget.direction!!,
                    sequence
                )
            }

            swingMode.swing(InteractionHand.MAIN_HAND)
            return
        }

        val switchMode = switchMode.activeMode
        val slot = switchMode.getSlot(mineTarget.blockState)
        if (!mineTarget.started) {
            startBreaking(slot, mineTarget)
        } else if (mode.activeMode.shouldUpdate(mineTarget, slot)) {
            updateBreakingProgress(mineTarget, slot)
            if (mineTarget.progress >= breakDamage && !mineTarget.finished) {
                mode.activeMode.finish(mineTarget)
                switchMode.getSwitchingMethod().switchBack()
            }
        }

        switchMode.getSwitchingMethod().reset()
    }

    private fun startBreaking(slot: IntObjectImmutablePair<ItemStack>?, mineTarget: MineTarget) {
        switch(slot, mineTarget)
        if (switchMode.activeMode.syncOnStart) {
            interaction.ensureHasSentCarriedItem()
        }

        mode.activeMode.start(mineTarget)
        mineTarget.started = true
    }

    private fun updateBreakingProgress(mineTarget: MineTarget, slot: IntObjectImmutablePair<ItemStack>?) {
        val switchMode = switchMode.activeMode
        mineTarget.progress += switchMode.getBlockBreakingDelta(
            mineTarget.targetPos,
            mineTarget.blockState,
            slot?.second()
        )

        switch(slot, mineTarget)
        if (switchMode.getSwitchingMethod().shouldSync) {
            interaction.ensureHasSentCarriedItem()
        }

        val f = if (breakDamage > 0f) {
            val breakDamageD = breakDamage.toDouble()
            mineTarget.progress.toDouble().coerceIn(0.0..breakDamageD) / breakDamageD / 2.0
        } else {
            0.5
        }

        val box = mineTarget.targetPos.outlineBox
        val lengthX = box.xsize
        val lengthY = box.ysize
        val lengthZ = box.zsize
        targetRenderer.updateBox(
            mineTarget.targetPos,
            box.inflate(
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

        val switchMode = switchMode.activeMode
        if (switchMode.shouldSwitch(mineTarget)) {
            switchMode.getSwitchingMethod().switch(slot, mineTarget)
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        val openScreen = mc.screen != null
        val unchangeableActive = !mode.activeMode.canManuallyChange && _target != null
        if (openScreen || unchangeableActive || !player.abilities.mayBuild) {
            return@handler
        }

        val isLeftClick = event.button == 0
        // without adding a little delay before being able to unselect / select again, selecting would be impossible
        val hasTimePassed = chronometer.hasElapsed(selectDelay.toLong())
        val hitResult = mc.hitResult
        if (!isLeftClick || !hasTimePassed || hitResult == null || hitResult !is BlockHitResult) {
            return@handler
        }

        val blockPos = hitResult.blockPos
        val state = blockPos.getState()!!

        val shouldTargetBlock = mode.activeMode.shouldTarget(blockPos, state)
        // stop when the block is clicked again
        val isCancelledByUser = blockPos == _target?.targetPos

        _target = if (shouldTargetBlock && world.worldBorder.isWithinBounds(blockPos) && !isCancelledByUser) {
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
        if (!mode.activeMode.stopOnStateChange) {
            return@handler
        }

        when (val packet = it.packet) {
            is ClientboundBlockUpdatePacket -> {
                mc.submit { updatePosOnChange(packet.pos, packet.blockState) }
            }

            is ClientboundSectionBlocksUpdatePacket -> {
                mc.submit {
                    packet.runUpdates { pos, state -> updatePosOnChange(pos, state) }
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
        if (_target?.finished != false && mode.activeMode.canManuallyChange || _target == null) {
            _target = MineTarget(blockPos)
        }
    }

    @Suppress("FunctionNaming", "FunctionName")
    fun _resetTarget() {
        _target = null
    }

}
