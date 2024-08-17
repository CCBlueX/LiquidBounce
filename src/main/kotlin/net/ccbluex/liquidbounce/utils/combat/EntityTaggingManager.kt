package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.entity.Entity

object EntityTaggingManager: Listenable {
    private val cache = mutableMapOf<Entity, EntityTag>()

    val tickHandler = handler<GameTickEvent> {
        cache.clear()
    }

    fun getTag(suspect: Entity): EntityTag {
        return this.cache.computeIfAbsent(suspect) {
            val targetingInfo = TagEntityEvent(suspect, EntityTargetingInfo.DEFAULT)

            EventManager.callEvent(targetingInfo)

            return@computeIfAbsent EntityTag(targetingInfo.targetingInfo, targetingInfo.color.value)
        }
    }

}

class EntityTag(
    val targetingInfo: EntityTargetingInfo,
    val color: Color4b?
)
