package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ProgressEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.text.Text

/**
 * TreasureManager module
 *
 * Automatically stores valuable items from inventory into a chest for BedWars.
 */
@Suppress("TooManyFunctions")
object ModuleTreasureManager : ClientModule("TreasureManager", Category.PLAYER) {

    private val inventoryConstrains = tree(InventoryConstraints())
    private val selectionMode by enumChoice("SelectionMode", SelectionMode.PRIORITY)
    private val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)

    private object OnlyCurrency : ToggleableConfigurable(this, "OnlyCurrency", true) {
        val iron by boolean("Iron", true)
        val gold by boolean("Gold", true)
        val diamond by boolean("Diamond", true)
        val emerald by boolean("Emerald", true)
    }

    private val showProgress by boolean("ShowProgress", true)
    private val checkTitle by boolean("CheckTitle", true)
    private val checkGaming by boolean("CheckGaming", true)
    private val onlyValuableItems by boolean ( "OnlyValuableItems", true)
    private var storingStartTime: Long = 0L
    private var initialItemCount: Int = 0
    private var remainingItems: Int = 0
    private var isInBedWars: Boolean = false

    private val itemPriorities = mapOf(
        Items.ENDER_PEARL to 0,
        Items.EMERALD to 1,
        Items.DIAMOND to 2,
        Items.DIAMOND_SWORD to 3,
        Items.DIAMOND_PICKAXE to 3,
        Items.DIAMOND_AXE to 3,
        Items.DIAMOND_SHOVEL to 3,
        Items.DIAMOND_HOE to 3,
        Items.TNT to 4,
        Items.BOW to 5,
        Items.POTION to 6,
        Items.SPLASH_POTION to 6,
        Items.LINGERING_POTION to 6,
        Items.GOLDEN_APPLE to 7,
        Items.ENCHANTED_GOLDEN_APPLE to 7,
        Items.BLAZE_ROD to 8,
        Items.FEATHER to 9,
        Items.EGG to 10,
        Items.IRON_INGOT to 11,
        Items.GOLD_INGOT to 12
    )

    private val excludedItems = setOf(
        Items.SHEARS,
        Items.WOODEN_SWORD,
        Items.WOODEN_PICKAXE,
        Items.WOODEN_AXE,
        Items.WOODEN_SHOVEL,
        Items.WOODEN_HOE,
        Items.COMPASS
    )

    private val bedWarsKeywords = listOf(
        "BedWars", "起床战争",
    )

    init {
        tree(OnlyCurrency)

        handler<PacketEvent> { event ->
            if (!checkGaming) return@handler
            val scoreboard = mc.world?.scoreboard ?: return@handler
            val obj = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return@handler
            val title = obj.displayName.string.lowercase()
            isInBedWars = bedWarsKeywords.any { title.contains(it.lowercase()) }
        }
    }

    override fun onDisabled() {
        initialItemCount = 0
        remainingItems = 0
        isInBedWars = false
        super.onDisabled()
    }


    @Suppress("unused")
    val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        if (checkGaming && !isInBedWars) {
            return@handler
        }

        val screen = getChestScreen()
        if (screen == null) {
            if (initialItemCount > 0) {
                initialItemCount = 0
                remainingItems = 0
                if (showProgress) {
                    EventManager.callEvent(
                        ProgressEvent(
                            title = "TreasureManager",
                            progress = 1f,
                            maxProgress = 1f
                        )
                    )
                }
            }
            return@handler
        }

        val itemsToStore = findValuableItemsInInventory()
            .filter { it.itemStack.item !in excludedItems }
            .distinctBy { it.getIdForServer(screen) }

        if (itemsToStore.isNotEmpty()) {
            if (initialItemCount == 0) {
                storingStartTime = System.currentTimeMillis()
                initialItemCount = itemsToStore.size
                remainingItems = itemsToStore.size
            }
            updateRemainingItems(itemsToStore.size)
        } else if (initialItemCount > 0) {
            updateRemainingItems(0)
        }

        val sortedItemsToStore = selectionMode.processor(itemsToStore)
        val stillRequiredSpace = getStillRequiredSpace(itemsToStore.size, screen)

        for (slot in sortedItemsToStore) {
            if (!hasChestSpace(screen) && stillRequiredSpace > 0) {
                break
            }

            val emptySlot = findEmptySlotsInContainer(screen).firstOrNull() ?: break
            val actions = getActionsForMove(screen, from = slot, to = emptySlot)

            event.schedule(
                inventoryConstrains,
                actions,
                mapIntToPriority(getItemPriority(slot.itemStack.item))
            )
            break
        }
    }

    private fun updateRemainingItems(count: Int) {
        remainingItems = count
        if (!showProgress) return

        val progress = if (initialItemCount <= 0) {
            0f
        } else {
            val storedItems = (initialItemCount - remainingItems).coerceAtLeast(0)
            (storedItems.toFloat() / initialItemCount).coerceIn(0f..1f)
        }

        if (initialItemCount <= 0 || remainingItems <= 0) return

        val elapsed = System.currentTimeMillis() - storingStartTime
        val timePerItem = elapsed.toFloat() / (initialItemCount - remainingItems)
        if (progress > 0 || (initialItemCount > 0 && remainingItems == 0)) {
            EventManager.callEvent(
                ProgressEvent(
                    title = "TreasureManager",
                    progress = progress,
                    maxProgress = 1f,
                    timeRemaining = if (progress < 1) (timePerItem * remainingItems).toLong() else null
                )
            )
        }
    }

    private fun getActionsForMove(
        screen: GenericContainerScreen,
        from: ItemSlot,
        to: ItemSlot
    ): List<InventoryAction.Click> {
        return when (itemMoveMode) {
            ItemMoveMode.QUICK_MOVE -> listOf(InventoryAction.Click.performQuickMove(screen, from))
            ItemMoveMode.DRAG_AND_DROP -> listOf(
                InventoryAction.Click.performPickup(screen, from),
                InventoryAction.Click.performPickup(screen, to),
            )
        }
    }

    private fun getStillRequiredSpace(slotsToStore: Int, screen: GenericContainerScreen): Int {
        val freeSlotsInChest = findEmptySlotsInContainer(screen).size
        return (slotsToStore - freeSlotsInChest).coerceAtLeast(0)
    }

    private fun isScreenTitleChest(screen: GenericContainerScreen): Boolean {
        val titleString = screen.title.string
        return arrayOf(
            "container.enderchest",
        ).any { Text.translatable(it).string == titleString }
    }


    private fun getChestScreen(): GenericContainerScreen? {
        return mc.currentScreen?.takeIf { it.canBeStore() } as GenericContainerScreen?

    }

    private fun Screen.canBeStore(): Boolean {
        return running && this is GenericContainerScreen && (!checkTitle || isScreenTitleChest(
            this
        ))
    }

    private fun findValuableItemsInInventory(): List<ItemSlot> {
        val slots = findNonEmptySlotsInInventory()
        return when {
            OnlyCurrency.enabled -> slots.filter { slot ->
                when (slot.itemStack.item) {
                    Items.IRON_INGOT -> OnlyCurrency.iron
                    Items.GOLD_INGOT -> OnlyCurrency.gold
                    Items.DIAMOND -> OnlyCurrency.diamond
                    Items.EMERALD -> OnlyCurrency.emerald
                    else -> false
                }
            }
            onlyValuableItems -> slots.filter { slot ->
                itemPriorities.containsKey(slot.itemStack.item)
            }
            else -> slots // Store all non-excluded items if OnlyValuableItems is disabled
        }
    }

    private fun findEmptySlotsInContainer(screen: GenericContainerScreen): List<ContainerItemSlot> {
        val slots = mutableListOf<ContainerItemSlot>()
        val containerSize = screen.screenHandler.rows * 9
        for (slotId in 0 until containerSize) {
            val stack = screen.screenHandler.slots[slotId].stack
            if (stack.isEmpty) {
                slots.add(ContainerItemSlot(slotId))
            }
        }
        return slots
    }

    private fun getItemPriority(item: Item): Int {
        return itemPriorities[item] ?: Int.MAX_VALUE
    }

    private fun mapIntToPriority(priority: Int): Priority {
        return when {
            priority <= 2 -> Priority.IMPORTANT_FOR_USAGE_1 // Ender Pearl, Emerald, Diamond
            priority <= 5 -> Priority.IMPORTANT_FOR_USAGE_3 // Diamond Tools, TNT, Bow
            priority <= 10 -> Priority.IMPORTANT_FOR_USAGE_2 // Potions, Golden Apples, Blaze Rod, Feather, Egg
            else -> Priority.NORMAL // Iron Ingot, Gold Ingot, others
        }
    }

    private fun hasChestSpace(screen: GenericContainerScreen): Boolean {
        return findEmptySlotsInContainer(screen).isNotEmpty()
    }

    @Suppress("unused")
    private enum class SelectionMode(
        override val choiceName: String,
        val processor: (List<ItemSlot>) -> List<ItemSlot>
    ) : NamedChoice {
        PRIORITY("Priority", { list ->
            list.sortedBy { getItemPriority(it.itemStack.item) }
        }),
        INDEX("Index", { list -> list.sortedBy { it.getIdForServer(null) ?: Int.MAX_VALUE } }),
        RANDOM("Random", List<ItemSlot>::shuffled),
    }

    private enum class ItemMoveMode(override val choiceName: String) : NamedChoice {
        QUICK_MOVE("QuickMove"),
        DRAG_AND_DROP("DragAndDrop"),
    }
}
