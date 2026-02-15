<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2026 CCBlueX
  -
  - LiquidBounce is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - LiquidBounce is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
  -->

<script lang="ts">
    import GenericPlayerInventory from "./GenericPlayerInventory.svelte";
    import type {PlayerInventory} from "../../../../integration/events";
    import type {ItemStack} from "../../../../integration/types";

    export let settings: { [name: string]: any };

    const cSettings = settings as HudInventoryStatisticsSettings;

    const getInventoryStatisticsStacks = (inventory: PlayerInventory): ItemStack[] => {
        const selectedItems = cSettings.items;
        if (!Array.isArray(selectedItems) || selectedItems.length === 0) {
            return [];
        }

        const counts = new Map<string, number>();

        for (const identifier of selectedItems) {
            counts.set(identifier, 0);
        }

        for (const stack of inventory.main) {
            if (stack.count <= 0) {
                continue;
            }

            const identifier = stack.identifier.toLowerCase();
            const currentCount = counts.get(identifier);

            if (currentCount !== undefined) {
                counts.set(identifier, currentCount + stack.count);
            }
        }

        const mergedStacks = selectedItems
            .map((identifier) => ({
                identifier,
                count: counts.get(identifier) ?? 0,
                damage: 0,
                maxDamage: 0,
                displayName: identifier,
            }));

        return cSettings.showEmpty ? mergedStacks : mergedStacks.filter(it => it.count);
    };
</script>

<GenericPlayerInventory
        rowLength={cSettings.rowLength}
        backgroundColor="transparent"
        gap="2px"
        getRenderedStacks={it => getInventoryStatisticsStacks(it)}
/>
