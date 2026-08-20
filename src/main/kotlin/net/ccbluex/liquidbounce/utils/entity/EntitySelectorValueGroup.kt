package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.utils.collection.asComparator
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.SequencedSet
import java.util.TreeSet

class EntitySelectorValueGroup(name: String) : ValueGroup(name, valueType = ValueType.ENTITY_SELECTOR) {

    private val entityTypes by entityTypes(name = "EntityTypes", default = defaultEntityTypes())
    private val playerMode by enumChoice("PlayerMode", PlayerMode.ALLOW_ALL)
    private val usernames by textList("Usernames", TreeSet(String.CASE_INSENSITIVE_ORDER))

    fun matches(entity: LivingEntity): Boolean {
        if (entity.type !in entityTypes) {
            return false
        }

        if (entity !is Player) {
            return true
        }

        return matchesPlayer(
            mode = playerMode,
            name = entity.gameProfile.name,
            isFriend = FriendManager.isFriend(entity),
            usernames = usernames,
        )
    }

    enum class PlayerMode(override val tag: String) : Tagged {
        ALLOW_ALL("AllowAll"),
        WHITELIST("Whitelist"),
        BLACKLIST("Blacklist"),
        FRIENDS_ONLY("FriendsOnly"),
        NON_FRIENDS_ONLY("NonFriendsOnly"),
    }

    private companion object {
        fun defaultEntityTypes(): SequencedSet<EntityType<*>> = objectRBTreeSetOf(
            BuiltInRegistries.ENTITY_TYPE.asComparator()
        ).apply {
            addAll(BuiltInRegistries.ENTITY_TYPE)
        }
    }
}

internal fun matchesPlayer(
    mode: EntitySelectorValueGroup.PlayerMode,
    name: String,
    isFriend: Boolean,
    usernames: Set<String>,
): Boolean = when (mode) {
    EntitySelectorValueGroup.PlayerMode.ALLOW_ALL -> true
    EntitySelectorValueGroup.PlayerMode.WHITELIST -> name in usernames
    EntitySelectorValueGroup.PlayerMode.BLACKLIST -> name !in usernames
    EntitySelectorValueGroup.PlayerMode.FRIENDS_ONLY -> isFriend
    EntitySelectorValueGroup.PlayerMode.NON_FRIENDS_ONLY -> !isFriend
}
