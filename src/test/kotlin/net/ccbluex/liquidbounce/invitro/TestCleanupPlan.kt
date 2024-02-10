package net.ccbluex.liquidbounce.invitro

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.integrationtest.util.DummyItemSlot
import net.ccbluex.liquidbounce.integrationtest.util.tenaccAssert
import net.ccbluex.tenacc.api.TACCTest
import net.ccbluex.tenacc.api.TACCTestClass
import net.ccbluex.tenacc.api.common.TACCSequenceAdapter
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

@TACCTestClass("TestCleanupPlan")
class TestCleanupPlan {
    private val woodenSwordSlot =
        DummyItemSlot(ItemStack(Items.WOODEN_SWORD, 1), ItemSlotType.INVENTORY, "wood sword")
    private val diamondAxeSlot =
        DummyItemSlot(ItemStack(Items.DIAMOND_AXE, 1), ItemSlotType.INVENTORY, "diamond axe")

    @TACCTest(name = "testCommonScenarios", scenary = "generic/one_block_platform.nbt")
    fun testValidAndUnfavourableBlocks(adapter: TACCSequenceAdapter) {
        adapter.startSequence {
            client {
                testSwordsWeaponsAndToolAssigments()
            }

            sync()
        }
    }

    private fun genericHotbarSlot(name: String) = DummyItemSlot(ItemStack.EMPTY, ItemSlotType.HOTBAR, name)

    @Suppress("LongMethod")
    private fun testSwordsWeaponsAndToolAssigments() {
        val expectedSwordSlot = genericHotbarSlot("sword target")
        val expectedAxeSlot = genericHotbarSlot("axe target")

        testSlotAssignment(
            template = hashMapOf(
                Pair(expectedSwordSlot, ItemSortChoice.SWORD),
                Pair(expectedAxeSlot, ItemSortChoice.AXE),
            ),
            availableSlots = listOf(expectedSwordSlot, expectedAxeSlot, woodenSwordSlot, diamondAxeSlot),
            expectedSwaps = arrayOf(
                InventorySwap(woodenSwordSlot, expectedSwordSlot),
                InventorySwap(diamondAxeSlot, expectedAxeSlot)
            ),
            expectedUsefulItems = arrayOf(
                woodenSwordSlot,
                diamondAxeSlot
            )
        )

        testSlotAssignment(
            template = hashMapOf(
                Pair(expectedSwordSlot, ItemSortChoice.WEAPON),
                Pair(expectedAxeSlot, ItemSortChoice.AXE),
            ),
            availableSlots = listOf(expectedSwordSlot, expectedAxeSlot, woodenSwordSlot, diamondAxeSlot),
            expectedSwaps = arrayOf(
                InventorySwap(woodenSwordSlot, expectedSwordSlot),
                InventorySwap(diamondAxeSlot, expectedAxeSlot)
            ),
            expectedUsefulItems = arrayOf(
                woodenSwordSlot,
                diamondAxeSlot
            )
        )

        testSlotAssignment(
            template = hashMapOf(
                Pair(expectedSwordSlot, ItemSortChoice.NONE),
                Pair(expectedAxeSlot, ItemSortChoice.AXE),
            ),
            availableSlots = listOf(expectedSwordSlot, expectedAxeSlot, woodenSwordSlot, diamondAxeSlot),
            expectedSwaps = arrayOf(
                InventorySwap(diamondAxeSlot, expectedAxeSlot)
            ),
            expectedUsefulItems = arrayOf(
                woodenSwordSlot,
                diamondAxeSlot
            )
        )

        testSlotAssignment(
            template = hashMapOf(
                Pair(expectedSwordSlot, ItemSortChoice.SWORD),
                Pair(expectedAxeSlot, ItemSortChoice.NONE),
            ),
            availableSlots = listOf(expectedSwordSlot, expectedAxeSlot, woodenSwordSlot, diamondAxeSlot),
            expectedSwaps = arrayOf(
                InventorySwap(woodenSwordSlot, expectedSwordSlot),
            ),
            expectedUsefulItems = arrayOf(
                woodenSwordSlot,
                diamondAxeSlot
            )
        )

        testSlotAssignment(
            template = hashMapOf(
                Pair(expectedSwordSlot, ItemSortChoice.WEAPON),
                Pair(expectedAxeSlot, ItemSortChoice.NONE),
            ),
            availableSlots = listOf(expectedSwordSlot, expectedAxeSlot, woodenSwordSlot, diamondAxeSlot),
            expectedSwaps = arrayOf(
                InventorySwap(diamondAxeSlot, expectedSwordSlot)
            ),
            expectedUsefulItems = arrayOf(
                woodenSwordSlot,
                diamondAxeSlot
            )
        )
    }

    private fun testSlotAssignment(
        template: HashMap<ItemSlot, ItemSortChoice>,
        availableSlots: List<DummyItemSlot>,
        expectedSwaps: Array<InventorySwap>,
        expectedUsefulItems: Array<DummyItemSlot>
    ) {
        val cleanupTemplate = CleanupPlanPlacementTemplate(template, HashMap(), true)

        val plan = CleanupPlanGenerator(cleanupTemplate, availableSlots).generatePlan()

        for (expectedSwap in expectedSwaps) {
            tenaccAssert(
                plan.swaps.contains(expectedSwap),
                "Expected a swap from ${expectedSwap.from} to ${expectedSwap.to}"
            )
        }

        tenaccAssert(
            plan.swaps.size == expectedSwaps.size,
            "Expected ${expectedSwaps.size} swaps, but actually got ${plan.swaps.size} (${plan.swaps})"
        )

        for (usefulItem in expectedUsefulItems) {
            tenaccAssert(
                plan.usefulItems.contains(usefulItem),
                "Expected slot $usefulItem to be useful!"
            )
        }

        tenaccAssert(
            plan.usefulItems.size == expectedUsefulItems.size,
            "Expected ${expectedUsefulItems.size} useful items, but actually got ${plan.usefulItems.size}" +
                " (${plan.usefulItems})"
        )
    }
}
