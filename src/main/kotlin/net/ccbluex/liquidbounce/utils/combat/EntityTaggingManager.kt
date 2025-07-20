package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.entity.Entity
import java.util.concurrent.ConcurrentHashMap

object EntityTaggingManager: EventListener {
    private val cache = ConcurrentHashMap<Entity, EntityTag>()

    @Suppress("unused")
    val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
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
