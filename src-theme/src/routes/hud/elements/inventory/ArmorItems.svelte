<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {ClientPlayerDataEvent, PlayerInventory, ClientPlayerInventoryEvent} from "../../../../integration/events";
    import type {PlayerData, ItemStack} from "../../../../integration/types";
    import {onMount} from "svelte";
    import {getPlayerInventory, getPlayerData} from "../../../../integration/rest";
    import {fly} from "svelte/transition";
    import {expoInOut} from "svelte/easing";
    import {armorDurabilityStore, totemCount} from '../island/Island';
    import {get} from 'svelte/store';
    import ArmorItem from "../../common/ItemView/ArmorItem.svelte";
    import Line from "../../common/Trims/Line.svelte";

    function createEmptySlot(): ItemStack {
        return {
            identifier: "minecraft:air",
            count: 0,
            damage: 0,
            maxDamage: 0,
            displayName: "Air",
            hasEnchantment: false,
            hasDyedColor: false
        };
    }

    export let armorSlots: ItemStack[] = Array(4).fill(0).map(() => createEmptySlot());

    let offHandSlots: ItemStack[] = [createEmptySlot()];


    function countTotems(inventory: PlayerInventory, offHand: ItemStack[]): number {
        let total = 0;

        for (const slot of inventory.main) {
            if (slot.identifier === "minecraft:totem_of_undying") {
                total += slot.count;
            }
        }

        for (const slot of offHand) {
            if (slot.identifier === "minecraft:totem_of_undying") {
                total += slot.count;
            }
        }

        return total;
    }

    function getDurabilityObj(item?: ItemStack | null) {
        return item && item.identifier !== 'minecraft:air' ? {
            identifier: item.identifier,
            displayName: item.displayName,
            durability: item.maxDamage - item.damage,
            maxDurability: item.maxDamage
        } : null;
    }

    function updateArmorDurability() {
        const next = {
            helmet: getDurabilityObj(armorSlots[0]),
            chestplate: getDurabilityObj(armorSlots[1]),
            leggings: getDurabilityObj(armorSlots[2]),
            boots: getDurabilityObj(armorSlots[3])
        };
        const current = get(armorDurabilityStore);

        if (JSON.stringify(current) !== JSON.stringify(next)) {
            armorDurabilityStore.set(next);
        }
    }

    function updatePlayerData(newData: PlayerData) {
        const newOffHand = newData.offHandStack ? {...newData.offHandStack} : createEmptySlot();
        offHandSlots = [newOffHand];


        setTimeout(() => {
            getPlayerInventory().then(inventory => {
                totemCount.set(countTotems(inventory, offHandSlots));
            });
        }, 100);
    }


    function updateInventory(inventory: PlayerInventory) {
        const newArmor = inventory.armor.map(slot => slot ? {...slot} : createEmptySlot());

        setTimeout(() => {
            totemCount.set(countTotems(inventory, offHandSlots));
        }, 100);

        if (JSON.stringify(armorSlots) !== JSON.stringify(newArmor)) {
            armorSlots = newArmor;
            updateArmorDurability();
        }
    }

    function shouldShowSlot(stack: ItemStack): boolean {
        return stack.identifier !== "minecraft:air" && stack.count > 0;
    }

    listen("clientPlayerInventory", (event: ClientPlayerInventoryEvent) => {
        updateInventory(event.inventory);

    });

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });

    onMount(async () => {
        const [inventory, playerData] = await Promise.all([
            getPlayerInventory(),
            getPlayerData()
        ]);
        updateInventory(inventory);
        updatePlayerData(playerData);
    });


</script>
<div class="armoritems-hud hud-container" id="armoritemshud" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="inventory-hud"></div>

    <div class="title">
        <img alt="armor" class="icon" src="img/hud/inventory/armor.svg"/>
        <span>ArmorItems</span>
    </div>
    <Line/>
    <div class="armor-items">
        {#each [...armorSlots].reverse() as stack (stack)}
            {#if shouldShowSlot(stack)}
                <ArmorItem {stack}/>
            {:else}
                <div class="empty-slot"></div>
            {/if}
        {/each}
        {#each offHandSlots as stack (stack)}
            {#if shouldShowSlot(stack)}
                <ArmorItem {stack}/>
            {:else}
                <div class="empty-slot"></div>
            {/if}
        {/each}
    </div>
    <div class="totem-counter" class:hidden={$totemCount === 0}>
        {$totemCount}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .hidden {
    display: none;
  }

  .armoritems-hud {
    position: relative;
    min-width: 225px;
    width: fit-content;
    padding: 6px 10px;
    user-select: none;
    white-space: nowrap;
    flex-direction: column;
  }

  .armor-items {
    display: flex;
    gap: 8px;
    padding: 4px 6px;
    border-radius: 6px;
  }

  .empty-slot {
    width: 32px;
    height: 32px;
    opacity: 0;
  }

  .totem-counter {
    position: absolute;
    bottom: 0;
    right: 0;
    padding: 4px 6px;
    color: gold;
    font-size: 12px;
    font-weight: bold;
    text-shadow: 1px 1px black;
  }

  .title {
    display: flex;
    align-items: center;
    font-size: 1em;
    font-weight: bold;
    letter-spacing: 0.1em;
    margin-bottom: 0.5em;
    color: white;

    .icon {
      margin: 0 0.3em;
      height: 1em;
      width: auto;
    }
  }

</style>
