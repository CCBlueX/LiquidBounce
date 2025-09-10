package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.updateRenderCount
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction
import net.minecraft.item.Items

import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleScaffoldHelper : ClientModule("ScaffoldHelper", Category.WORLD, aliases = arrayOf("AutoScaffold")) {
    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val keepTick by int("KeepTick",20,10..50,"tick")
    private val scaffoldOnlyReceiveHit by boolean("OnlyReceiveHit", true)
    private val scaffoldOnlyDuringCombat by boolean("OnlyDuringCombat", false)
    private val scaffoldBlockedTick by int("BlockedForFreeze",5,5..30,"tick")
    private val notCondition by multiEnumChoice("Not", NotCondition.WHILE_SNEAKING)

    private var freezeActiveTicks = 0
    private var lastPlaceTick = 0

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
        if (scaffoldOnlyDuringCombat && !CombatManager.isInCombat) return false
        if (scaffoldOnlyReceiveHit && !CombatManager.isReceiveHit) return false
        return notCondition.all { it.testCondition() }
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
    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val currentTick = mc.world!!.time.toInt()

        if (ModuleStuck.running) {
            freezeActiveTicks++
        } else {
            freezeActiveTicks = 0
        }

        val freezeBlocking = freezeActiveTicks > scaffoldBlockedTick

        // 如果卡死，不允许 Scaffold
        if (freezeBlocking && helping) {
            ModuleScaffold.enabled = false
            helping = false
            lastPlaceTick = 0
            updateRenderCount()
            return@handler
        }

        // 危险 → 启动 Scaffold 并记录开始时间
        if (shouldEnableScaffold() && passesRequirements() && !freezeBlocking) {
            if (!helping) {
                ModuleScaffold.enabled = true
                helping = true
                lastPlaceTick = currentTick
            }
        }

        // Scaffold 在放方块 → 刷新时间戳
        if (helping && ModuleScaffold.currentTarget != null) {
            lastPlaceTick = currentTick
        }

        // 超过 keepTick 没有放方块 → 关闭 Scaffold
        if (helping && currentTick - lastPlaceTick > keepTick) {
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
        WHILE_KILLAURA("WhileKillAura",{ModuleKillAura.targetTracker.target == null}),
    }
}
