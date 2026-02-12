/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.inventory

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.asComparator
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.text.StringMatchMode
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.EnumSet
import java.util.function.Predicate


/**
 * Constraints for inventory actions.
 * This can be used to ensure that the player is not moving or rotating while interacting with the inventory.
 * It Also allows setting delays for opening, clicking and closing the inventory.
 */
open class InventoryConstraints : ValueGroup("Constraints") {

    internal val startDelay by intRange("StartDelay", 1..2, 0..20, "ticks")
    internal val clickDelay by intRange("ClickDelay", 2..4, 0..20, "ticks")
    internal val closeDelay by intRange("CloseDelay", 1..2, 0..20, "ticks")
    internal val missChance by intRange("MissChance", 0..0, 0..100, "%")

    internal val requirements by multiEnumChoice<InventoryRequirements>(
        "Requires",
        default = enumSetOf(),
        choices = requirementChoices(),
    )

    protected open fun requirementChoices(): EnumSet<InventoryRequirements> = enumSetOf(
        InventoryRequirements.NO_MOVEMENT,
        InventoryRequirements.NO_ROTATION
    )

    /**
     * Whether the constraints are met, this will be checked before any inventory actions are performed.
     */
    fun passesRequirements(action: InventoryAction) =
        requirements.all { it.test(action) }

}

/**
 * Additional constraints for the player inventory. This should be used when interacting with the player inventory
 * instead of a generic container.
 */
class PlayerInventoryConstraints : InventoryConstraints() {
    val requiresOpenInventory get() = InventoryRequirements.OPEN_INVENTORY in requirements

    override fun requirementChoices(): EnumSet<InventoryRequirements> {
        return super.requirementChoices().also { it += InventoryRequirements.OPEN_INVENTORY }
    }
}

enum class InventoryRequirements(
    override val tag: String,
) : Tagged, Predicate<InventoryAction> {
    NO_MOVEMENT("NoMovement"),

    NO_ROTATION("NoRotation"),

    /**
     * When this option is not enabled, the inventory will be opened silently
     * depending on the Minecraft version chosen using ViaFabricPlus.
     *
     * If the protocol contains [com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1]
     * and the client status packet is supported,
     * the inventory will be opened silently using [openInventorySilently].
     * Otherwise, the inventory will not have any open tracking and
     * the server will only know when clicking in the inventory.
     *
     * Closing will still be required to be done for any version.
     * Sad.
     * :(
     */
    OPEN_INVENTORY("InventoryOpen");

    override fun test(action: InventoryAction): Boolean = when (this) {
        NO_MOVEMENT -> player.input.moveVector.isLikelyZero && !player.jumping
        NO_ROTATION -> RotationManager.rotationMatchesPreviousRotation()
        OPEN_INVENTORY -> !action.requiresPlayerInventoryOpen() || InventoryManager.isInventoryOpen
    }
}

class CheckScreenHandlerTypeValueGroup(
    parent: EventListener,
) : ToggleableValueGroup(parent, "CheckScreenHandlerType", enabled = true) {
    private val types by registryList(
        "Types",
        objectRBTreeSetOf(
            BuiltInRegistries.MENU.asComparator(),
            MenuType.GENERIC_9x3, MenuType.GENERIC_9x6, MenuType.SHULKER_BOX,
        ),
        ValueType.MENU
    )
    private val filter by enumChoice("Filter", Filter.WHITELIST)

    fun isValid(screen: AbstractContainerScreen<*>): Boolean {
        return !running || filter(screen.menu.typeOrNull, types)
    }
}

class CheckScreenTitleValueGroup(
    parent: EventListener,
) : ToggleableValueGroup(parent, "CheckScreenTitle", enabled = true, aliases = listOf("CheckTitle")) {
    private val titles by multiEnumChoice(
        "Titles",
        enumSetOf(
            ContainerTitle.CHEST, ContainerTitle.LARGE_CHEST,
            ContainerTitle.SHULKER_BOX, ContainerTitle.BARREL,
            ContainerTitle.CHEST_MINECART,
        ),
    )
    private val customTitles by textList("Custom", ObjectRBTreeSet())
    private val filter by enumChoice("Filter", Filter.WHITELIST)

    fun isValid(screen: Screen): Boolean {
        if (!running) return true

        val titleString = screen.title.string
        val matches = titles.any {
            Component.translatable(it.translatableKey).string == titleString
        } || titleString in customTitles

        return when (filter) {
            Filter.WHITELIST -> matches
            Filter.BLACKLIST -> !matches
        }
    }

    @Suppress("unused")
    private enum class ContainerTitle(override val tag: String, val translatableKey: String) : Tagged {
        BARREL("Barrel", "container.barrel"),
        BEACON("Beacon", "container.beacon"),
        BLAST_FURNACE("BlastFurnace", "container.blast_furnace"),
        BREWING_STAND("BrewingStand", "container.brewing"),
        CHEST("Chest", "container.chest"),
        LARGE_CHEST("LargeChest", "container.chestDouble"),
        DISPENSER("Dispenser", "container.dispenser"),
        DROPPER("Dropper", "container.dropper"),
        ENDER_CHEST("EnderChest", "container.enderchest"),
        FURNACE("Furnace", "container.furnace"),
        HOPPER("Hopper", "container.hopper"),
        SHULKER_BOX("ShulkerBox", "container.shulkerBox"),
        SMOKER("Smoker", "container.smoker"),
        CHEST_MINECART("ChestMinecart", "entity.minecraft.chest_minecart"),
        HOPPER_MINECART("HopperMinecart", "entity.minecraft.hopper_minecart"),
    }
}

sealed class SingleItemStackPickMode(
    final override val parent: ModeValueGroup<*>,
    name: String,
) : Mode(name), Predicate<ItemStack> {

    abstract override fun test(itemStack: ItemStack): Boolean

    class ByName(parent: ModeValueGroup<*>) : SingleItemStackPickMode(parent, "Name") {
        private val names by textList("Names", objectRBTreeSetOf("Paper"))
        private val mode by enumChoice("Mode", StringMatchMode.EQUALS)

        override fun test(itemStack: ItemStack): Boolean {
            val string = itemStack.hoverName.string
            return names.any { mode.test(string, it) }
        }
    }

    class ByItem(parent: ModeValueGroup<*>) : SingleItemStackPickMode(parent, "Item") {
        private val items by items("Items", itemSortedSetOf(Items.PAPER))

        override fun test(itemStack: ItemStack): Boolean = itemStack.item in items
    }

}
