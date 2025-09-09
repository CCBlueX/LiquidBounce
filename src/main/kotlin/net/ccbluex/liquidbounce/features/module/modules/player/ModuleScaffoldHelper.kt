package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.updateRenderCount
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.VoidFallPrediction

object ModuleScaffoldHelper : ClientModule("ScaffoldHelper", Category.WORLD, aliases = arrayOf("AutoScaffold")) {

    private val voidFallPrediction = tree(VoidFallPrediction(this))
    private val keepTick by int("KeepTick",15,10..30,"tick")
    private val scaffoldOnlyReceiveHit by boolean("OnlyReceiveHit", true)
    private val scaffoldOnlyDuringCombat by boolean("OnlyDuringCombat", false)
    private val notCondition by multiEnumChoice("Not", NotCondition.WHILE_SNEAKING)

    private var scaffolding = false
    private var keepTickLeft = 0

    override val running: Boolean
        get() = super.running
            && passesRequirements()
            && !(ModuleFreeze.running || ModuleAutoStuck.scaffoldBlocked)

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
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler

        if (shouldEnableScaffold() && passesRequirements()) {
            keepTickLeft = keepTick
            if (!ModuleScaffold.enabled) {
                ModuleScaffold.enabled = true
                scaffolding = true
            }
            if ((player.isOnGround && scaffolding) || ModuleAutoStuck.scaffoldBlocked) {
                ModuleScaffold.enabled = false
                scaffolding = false
                keepTickLeft = 0
            }
        } else if (scaffolding) {
            if (keepTickLeft > 0) {
                keepTickLeft--
            } else {
                ModuleScaffold.enabled = false
                scaffolding = false
            }
        }
    }



    override fun onDisabled() {
        scaffolding = false
        updateRenderCount()
        super.onDisabled()
    }
    @Suppress("unused")
    private enum class NotCondition(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        WHILE_USING_ITEM("WhileUsingItem", { !player.isUsingItem }),
        WHILE_SNEAKING("WhileSneaking", { !player.isSneaking }),
    }
}
