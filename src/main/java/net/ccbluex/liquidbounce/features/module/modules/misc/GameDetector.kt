package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.entity.boss.IBossDisplayData
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.potion.Potion
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard

@ModuleInfo("GameDetector", "Detects if you are in an ongoing game.", ModuleCategory.MISC, Keyboard.CHAR_NONE, false)
object GameDetector: Module() {
    private val gameModeValue = BoolValue("GameModeCheck", true)
    private val capabilitiesValue = BoolValue("CapabilitiesCheck", true)
    private val tabListValue = BoolValue("TabListCheck", true)
    private val teamsValue = BoolValue("TeamsCheck", true)
    private val invisibilityValue = BoolValue("InvisibilityCheck", true)
    private val entityValue = BoolValue("EntityCheck", false)

    private val whitelist = setOf(":", "Vazio!", "§6§lRumble Box", "§5§lDivine Drop")

    private var playing = false

    fun isInGame() = mc.thePlayer != null && playing

    @EventTarget
    fun onUpdate(updateEvent: UpdateEvent) {
        playing = false

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        val netHandler = mc.netHandler ?: return
        val capabilities = player.capabilities

        if (gameModeValue.get() && mc.playerController.currentGameType != WorldSettings.GameType.SURVIVAL)
            return

        if (capabilitiesValue.get() &&
            (!capabilities.allowEdit || capabilities.allowFlying || capabilities.isFlying || capabilities.disableDamage))
            return

        if (tabListValue.get() && netHandler.playerInfoMap.size <= 1)
            return

        if (teamsValue.get() && player.team?.allowFriendlyFire == false && world.scoreboard.teams.size == 1)
            return

        if (invisibilityValue.get() && player.getActivePotionEffect(Potion.invisibility)?.isPotionDurationMax == true)
            return

        if (entityValue.get() && world.loadedEntityList.any { e ->
                (e is IBossDisplayData || e is EntityArmorStand) && e.customNameTag?.let { tag ->
                    //Check if entity name doesn't contain any of whitelisted substrings
                    whitelist.none { it in tag }
                } == true
            })
            return

        playing = true
    }

    override fun handleEvents(): Boolean {
        return true
    }
}