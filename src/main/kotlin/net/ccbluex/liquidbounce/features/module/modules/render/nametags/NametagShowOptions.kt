package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.objectLinkedSetOf
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack

// TODO: Split this into detailed configuration
internal enum class NametagShowOptions(
    override val choiceName: String
) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    PING("Ping"),
    ENCHANTMENTS("Enchantments"),
    BORDER("Border");

    fun isShowing() = this in ModuleNametags.show
}

internal object NametagEquipment : ToggleableConfigurable(ModuleNametags, "Equipment", true) {

    private val slots by multiEnumChoice(
        "Slots",
        objectLinkedSetOf(
            NEquipmentSlot.MAINHAND, NEquipmentSlot.HEAD, NEquipmentSlot.CHEST,
            NEquipmentSlot.LEGS, NEquipmentSlot.FEET, NEquipmentSlot.OFFHAND,
        ),
    )
    private val skipEmptySlot by boolean("SkipEmptySlot", true)
    val showInfo by boolean("ShowInfo", true)

    /**
     * Creates a list of items that should be rendered above the name tag.
     */
    fun createItemList(entity: LivingEntity): List<ItemStack> {
        val stacks = slots.mapToArray {
            entity.getEquippedStack(it.slot)
        }

        return if (skipEmptySlot) {
            stacks.filterNot { it.isEmpty }
        } else {
            stacks.asList()
        }
    }

    private enum class NEquipmentSlot(
        override val choiceName: String,
        val slot: EquipmentSlot,
    ) : NamedChoice {
        MAINHAND("Mainhand", EquipmentSlot.MAINHAND),
        OFFHAND("Offhand", EquipmentSlot.OFFHAND),
        FEET("Feet", EquipmentSlot.FEET),
        LEGS("Legs", EquipmentSlot.LEGS),
        CHEST("Chest", EquipmentSlot.CHEST),
        HEAD("Head", EquipmentSlot.HEAD),
        BODY("Body", EquipmentSlot.BODY),
        SADDLE("Saddle", EquipmentSlot.SADDLE),
    }
}
