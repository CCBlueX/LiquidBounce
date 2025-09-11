package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleEagle.wasSneaking
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.updateRenderCount
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.ccbluex.liquidbounce.utils.entity.isInVoid
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.item.Items

object ModuleScaffoldHelper : ClientModule("ScaffoldHelper", Category.WORLD, aliases = arrayOf("AutoScaffold")) {
    private val AutoJumpOnVoidEdge by boolean("AutoJumpAtVoidEdge",true)
    private val preserveInVoid by boolean("PreserveInVoid",false)
    private val voidDisableTick by int("VoidDisableDelay", 50, 5..100, "tick")
    private val ticksToPredict by int("TicksToPredict", 10, 2..20,"tick")
    private val scaffoldBlockedTick by int("BlockedForFreeze",15,5..30,"tick")
    private val keepTick by int("KeepEnabledTicks",16,10..30,"tick")
    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val notCondition by multiEnumChoice("Not", NotCondition.WHILE_SNEAKING)

    private var stuckTicks = 0
    private var lastPlacedTick = 0
    private var scaffoldActiveUntil = 0
    private var voidStayTicks = 0

    var isHelping = false


    override val running: Boolean
        get() = super.running
            && passesRequirements()

    private fun noBlockNearby(): Boolean{
        return !player.pos.add(0.0, -1.0, 0.0).searchBlocksInRadius(4.5f) { _, state -> !state.isAir }.any()

    }

    private fun shouldEnableScaffold(): Boolean {
        return voidFallPrediction.isVoidFallImminent  && !noBlockNearby()
    }

    private fun passesRequirements(): Boolean {
        if (!inGame || isDestructed) return false
        return notCondition.all { it.testCondition() }
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<MovementInputEvent> { event ->
        val nextTick = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(ticksToPredict)
        val simulatedPlayer = PlayerSimulationCache.getSimulationForLocalPlayer()
        val shouldJump = player.isOnGround && player.moving && AutoJumpOnVoidEdge &&
            isInVoid(nextTick.pos, 0)&&
            !isHelping &&
            !wasSneaking &&
            !player.isSneaking &&
            !mc.options.sneakKey.isPressed &&
            !mc.options.jumpKey.isPressed &&
            !simulatedPlayer.getSnapshotAt(1).onGround

        if (shouldJump) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (player.mainHandStack.item != Items.ENDER_PEARL || !ModuleScaffold.enabled || ModuleStuck.enabled) {
            return@handler
        }
        if (packet is PlayerInteractItemC2SPacket) {
            event.cancelEvent()
            sendPacketSilently(
                PlayerMoveC2SPacket.LookAndOnGround(
                    player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                )
            )
            sendPacketSilently(
                PlayerInteractItemC2SPacket(
                    packet.hand, packet.sequence, player.yaw, player.pitch
                )
            )
        }
    }

    fun notifyBlockPlaced() {
        val currentTick = mc.world!!.time.toInt()
        lastPlacedTick = currentTick
        scaffoldActiveUntil = currentTick + keepTick
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val currentTick = mc.world!!.time.toInt()

        if (ModuleStuck.running) stuckTicks++ else stuckTicks = 0
        val freezeBlocking = stuckTicks > scaffoldBlockedTick

        if (freezeBlocking && isHelping) {
            ModuleScaffold.enabled = false
            isHelping = false
            updateRenderCount()
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

        val canEnableNow = shouldEnableScaffold() && !freezeBlocking

        if (canEnableNow && !isHelping) {
            ModuleScaffold.enabled = true
            isHelping = true
            lastPlacedTick = currentTick
            scaffoldActiveUntil = currentTick + keepTick
        }

        if (isHelping && currentTick >= scaffoldActiveUntil) {
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
        WHILE_SNEAKING("WhileSneaking", { !player.isSneaking }),
        WHILE_ON_GROUND("WhileOnGround",{ !player.isOnGround }),
        WhileReceiveHit("WhileReceiveHit", { !CombatManager.isReceiveHit }),
        WhileDuringCombat("WhileDuringCombat", { !CombatManager.isInCombat }),
        WHILE_KILLAURA("WhileKillAura",{ ModuleKillAura.targetTracker.target == null }),
    }
}
