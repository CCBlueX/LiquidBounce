package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.updateRenderCount
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.ccbluex.liquidbounce.utils.entity.isInVoid
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleScaffoldHelper : ClientModule("ScaffoldHelper", Category.WORLD, aliases = arrayOf("AutoScaffold")) {
    private val AutoJumpOnVoidEdge by boolean("AutoJumpOnVoidEdge",true)
    private val ticksToPredict by int("TicksToPredict", 2, 2..20,"tick")
    private val scaffoldBlockedTick by int("BlockedForFreeze",15,5..30,"tick")
    private val keepTick by int("KeepTick",10,5..20,"tick")
    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val notCondition by multiEnumChoice("Not", NotCondition.WHILE_SNEAKING)

    private var freezeActiveTicks = 0
    private var lastPlaceTick = 0
    private var scaffoldLockUntil = 0

    var helping = false


    override val running: Boolean
        get() = super.running
            && passesRequirements()

    private fun shouldEnableScaffold(): Boolean {
        return voidFallPrediction.isVoidFallImminent &&
            player.pos.add(0.0, -1.0, 0.0).searchBlocksInRadius(4.5f) { _, state ->
                !state.isAir
            }.any()
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
            !helping &&
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
        if (player.mainHandStack.item != Items.ENDER_PEARL) return@handler
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
        lastPlaceTick = currentTick
        scaffoldLockUntil = currentTick + keepTick
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val currentTick = mc.world!!.time.toInt()

        if (ModuleStuck.running) freezeActiveTicks++ else freezeActiveTicks = 0
        val freezeBlocking = freezeActiveTicks > scaffoldBlockedTick

        if (freezeBlocking && helping) {
            ModuleScaffold.enabled = false
            helping = false
            updateRenderCount()
            return@handler
        }

        val canEnableNow = shouldEnableScaffold() && !freezeBlocking

        if (canEnableNow && !helping) {
            ModuleScaffold.enabled = true
            helping = true
            lastPlaceTick = currentTick
            scaffoldLockUntil = currentTick + keepTick
        }

        if (helping && currentTick >= scaffoldLockUntil) {
            ModuleScaffold.enabled = false
            helping = false
        }
    }


    override fun onDisabled() {
        helping = false
        freezeActiveTicks = 0
        super.onDisabled()
    }

    @Suppress("unused")
    private enum class NotCondition(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        WHILE_USING_ITEM("WhileUsingItem", { !player.isUsingItem }),
        WHILE_SNEAKING("WhileSneaking", { !player.isSneaking }),
        WhileReceiveHit("WhileReceiveHit", { !CombatManager.isReceiveHit }),
        WhileDuringCombat("WhileDuringCombat", { !CombatManager.isInCombat }),
    }
}
