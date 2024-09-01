package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.misc.StringUtils.contains
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.boss.BossBarProvider
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.Items
import net.minecraft.item.ItemStack

object GameDetector: Module("GameDetector", Category.MISC, gameDetecting = false, hideModule = false) {
    // Check if player's gamemode is Survival or Adventure
    private val gameMode by BoolValue("GameModeCheck", true)

    // Check if player doesn't have unnatural abilities
    private val abilities by BoolValue("CapabilitiesCheck", true)

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

        val player = mc.player ?: return
        val theWorld = mc.world ?: return
        val netHandler = mc.networkHandler ?: return
        val abilities = player.abilities

        val slots = slot - 1
        val itemSlot = mc.player.inventory.getInvStack(slots)

        if (gameMode && !mc.interactionManager.currentGameMode.isSurvivalLike)
            return

        if (this.abilities &&
            (!abilities.allowModifyWorld || abilities.allowFlying || abilities.flying || abilities.invulnerable))
            return

        if (tabList && netHandler.playerList.size <= 1)
            return

        if (teams && player.team?.allowFriendlyFire == false && theWorld.scoreboard.teams.size == 1)
            return

        if (invisibility && player.hasStatusEffect(StatusEffect.INVISIBILITY))
            return

        if (compass) {
            if (checkAllSlots && mc.player.inventory.contains(ItemStack(Items.COMPASS)))
                return

            if (!checkAllSlots && itemSlot?.item == Items.COMPASS)
                return
        }

        if (scoreboard) {
            if (LOBBY_SUBSTRINGS in theWorld.scoreboard.getObjectiveForSlot(1)?.displayName)
                return

            if (theWorld.scoreboard.objectiveNames.any { LOBBY_SUBSTRINGS in it })
                return

            if (theWorld.scoreboard.teams.any { LOBBY_SUBSTRINGS in it.prefix })
                return
        }

        if (entity) {
            for (entity in theWorld.entities) {
                if (entity !is BossBarProvider && entity !is ArmorStandEntity)
                    continue

                val name = entity.customName ?: continue

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