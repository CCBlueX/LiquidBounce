package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleEagle.shouldBeActive
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.VoidFallPredictor
import net.ccbluex.liquidbounce.utils.entity.isInVoid
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.entity.player.PlayerEntity

object ModuleScaffoldHelper : ClientModule("ScaffoldHelper", Category.WORLD, aliases = arrayOf("AutoScaffold")) {
    private val AutoJumpOnVoidEdge by boolean("AutoJumpAtVoidEdge",true)
    private val AutoDisable by boolean("AutoDisable", true)
    private val preserveInVoid by boolean("PreserveInVoid",false)
    private val voidFallPrediction = tree(VoidFallPredictor( ticksToPredict = 3, voidThreshold = -64))
    private val voidDisableTick by int("VoidDisableDelay", 50, 5..100, "tick")
    private val scaffoldBlockedTick by int("BlockedForFreeze",15,5..30,"tick")
    private val keepTick by int("KeepEnabledTicks",16,10..30,"tick")
    private val SafeLandBlocks by int("SafeLandBlocks",3,2..5)
    private val notCondition by multiEnumChoice("Not", NotCondition.WHILE_SNEAKING)

    private var stuckTicks = 0
    private var lastPlacedTick = 0
    private var scaffoldActiveUntil = 0
    private var voidStayTicks = 0

    var isHelping = false


    private fun noBlockNearby(): Boolean{
        return !player.pos.add(0.0, -1.0, 0.0).searchBlocksInRadius(4.5f) { _, state -> !state.isAir }.any()

    }


    private fun passesRequirements(): Boolean {
        if (!inGame || isDestructed) return false
        return notCondition.all { it.testCondition() }
    }

    private fun isImminentVoid(): Boolean {
        val nextTick = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(3)
        return isInVoid(nextTick.pos,checkOnGround = false)
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<MovementInputEvent> { event ->
        val simulatedPlayer = PlayerSimulationCache.getSimulationForLocalPlayer()
        val shouldJump = player.isOnGround && player.moving && AutoJumpOnVoidEdge && isImminentVoid()
            &&
            !isHelping &&
            !shouldBeActive &&
            !player.isSneaking &&
            !mc.options.sneakKey.isPressed &&
            !mc.options.jumpKey.isPressed &&
            !simulatedPlayer.getSnapshotAt(1).onGround

        if (shouldJump) {
            event.jump = true
        }
    }

    fun notifyBlockPlaced() {
        val currentTick = mc.world!!.time.toInt()
        lastPlacedTick = currentTick
        scaffoldActiveUntil = currentTick + keepTick
    }
    private fun hasUsableBlocks(): Boolean {
        val inv = player.inventory
        return (0 until inv.size()).any { i ->
            val stack = inv.getStack(i)
            ScaffoldBlockItemSelection.isValidBlock(stack)
        }
    }

    private fun hasSolidBelow(player: PlayerEntity, blocks: Int = SafeLandBlocks): Boolean {
        val startY = player.blockPos.y - 1
        for (yOffset in 0..(startY - world.bottomY - blocks + 1)) {
            var solidCount = 0
            for (i in 0 until blocks) {
                val state = player.blockPos.down(yOffset + i + 1).getState() ?: continue
                if (!state.isAir) solidCount++ else break
            }
            if (solidCount == blocks) return true
        }
        return false
    }
    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (player.isSpectator||player.abilities.flying||mc.world?.bottomY?.let { it1 -> player.y <= (it1) } == true){
            return@handler
        }
        val currentTick = mc.world!!.time.toInt()

        if (ModuleStuck.running) stuckTicks++ else stuckTicks = 0
        val freezeBlocking = stuckTicks > scaffoldBlockedTick

        if (freezeBlocking && isHelping) {
            ModuleScaffold.enabled = false
            isHelping = false
            return@handler
        }

        if (noBlockNearby() && preserveInVoid && isInVoid(player.pos) ) {
            voidStayTicks++
            if (voidStayTicks >= voidDisableTick && ModuleScaffold.enabled) {
                ModuleScaffold.enabled = false
                isHelping = false
                notification(
                    "ScaffoldHelper",
                    "Disabled due to prolonged void exposure.",
                    NotificationEvent.Severity.ERROR
                )
                return@handler
            }
        } else {
            voidStayTicks = 0
        }

        val canEnableNow = voidFallPrediction.isVoidFallImminent
            && !freezeBlocking && !noBlockNearby() && passesRequirements() && hasUsableBlocks()

        if (canEnableNow && !isHelping) {
            ModuleScaffold.enabled = true
            isHelping = true
            lastPlacedTick = currentTick
            scaffoldActiveUntil = currentTick + keepTick
        }

        val solidBelow3x3 = (-1..1).all { dx ->
            (-1..1).all { dz ->
                !player.blockPos.add(dx, -1, dz).getState()!!.isAir
            }
        }

        val shouldAutoDisableScaffold = currentTick >= scaffoldActiveUntil ||
            (AutoDisable && (
                (player.isOnGround && solidBelow3x3) || !passesRequirements() ||
                    (!player.isOnGround && hasSolidBelow(player))
                ))

        if (isHelping && shouldAutoDisableScaffold) {
            ModuleScaffold.enabled = false
            isHelping = false
        }

    }

    override fun onDisabled() {
        isHelping = false
        stuckTicks = 0
        super.onDisabled()
    }

    @Suppress("unused")
    private enum class NotCondition(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        WHILE_USING_ITEM("WhileUsingItem", { !player.isUsingItem }),
        WHILE_SNEAKING("WhileSneaking", { !mc.options.sneakKey.isPressed }),
        WhileReceiveHit("WhileReceiveHit", { !CombatManager.isReceiveHit }),
        WhileDuringCombat("WhileDuringCombat", { !CombatManager.isInCombat }),
    }
}
