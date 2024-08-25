package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.misc.StringUtils.contains
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.boss.IBossDisplayData
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion

object GameDetector: Module("GameDetector", Category.MISC, gameDetecting = false, hideModule = false) {
    // Check if player's gamemode is Survival or Adventure
    private val gameMode by BoolValue("GameModeCheck", true)

    // Check if player doesn't have unnatural capabilities
    private val capabilities by BoolValue("CapabilitiesCheck", true)

    // Check if there are > 1 players in tablist
    private val tabList by BoolValue("TabListCheck", true)

    // Check if there are > 1 teams or if friendly fire is enabled
    private val teams by BoolValue("TeamsCheck", true)

    // Check if player doesn't have infinite invisibility effect
    private val invisibility by BoolValue("InvisibilityCheck", true)

    // Check if player has compass inside their inventory
    private val compass by BoolValue("CompassCheck", false)

    // Check for compass inside inventory. If false, then it should only check for selected slot
    private val checkAllSlots by BoolValue("CheckAllSlots", true) { compass }
    private val slot by IntegerValue("Slot", 1, 1..9) { compass && !checkAllSlots }

    // Check for any hub-like BossBar or ArmorStand entities
    private val entity by BoolValue("EntityCheck", false)

    // Check for strings in scoreboard that could signify that the game is waiting for players or if you are in a lobby
    // Needed on Gamster
    private val scoreboard by BoolValue("ScoreboardCheck", false)

    private val WHITELISTED_SUBSTRINGS = arrayOf(":", "Vazio!", "§6§lRumble Box", "§5§lDivine Drop")

    private var isPlaying = false

    private val LOBBY_SUBSTRINGS = arrayOf("lobby", "hub", "waiting", "loading", "starting")

    fun isInGame() = !state || isPlaying

    @EventTarget(priority = 1)
    fun onUpdate(updateEvent: UpdateEvent) {
        isPlaying = false

        val thePlayer = mc.player ?: return
        val theWorld = mc.world ?: return
        val netHandler = mc.netHandler ?: return
        val capabilities = thePlayer.capabilities

        val slots = slot - 1
        val itemSlot = mc.player.inventory.getStackInSlot(slots)

        if (gameMode && !mc.interactionManager.gameIsSurvivalOrAdventure())
            return

        if (this.capabilities &&
            (!capabilities.allowEdit || capabilities.allowFlying || capabilities.isFlying || capabilities.disableDamage))
            return

        if (tabList && netHandler.playerInfoMap.size <= 1)
            return

        if (teams && thePlayer.team?.allowFriendlyFire == false && theWorld.scoreboard.teams.size == 1)
            return

        if (invisibility && thePlayer.getActivePotionEffect(Potion.invisibility)?.isPotionDurationMax == true)
            return

        if (compass) {
            if (checkAllSlots && mc.player.inventory.hasItemStack(ItemStack(Items.compass)))
                return

            if (!checkAllSlots && itemSlot?.item == Items.compass)
                return
        }

        if (scoreboard) {
            if (LOBBY_SUBSTRINGS in theWorld.scoreboard.getObjectiveInDisplaySlot(1)?.displayName)
                return

            if (theWorld.scoreboard.objectiveNames.any { LOBBY_SUBSTRINGS in it })
                return

            if (theWorld.scoreboard.teams.any { LOBBY_SUBSTRINGS in it.colorPrefix })
                return
        }

        if (entity) {
            for (entity in theWorld.loadedEntityList) {
                if (entity !is IBossDisplayData && entity !is EntityArmorStand)
                    continue

                val name = entity.customNameTag ?: continue

                // If an unnatural entity has been found, break the loop if its name includes a whitelisted substring
                if (WHITELISTED_SUBSTRINGS in name) break
                else return
            }
        }

        isPlaying = true
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        isPlaying = false
    }

    override fun handleEvents() = true
}