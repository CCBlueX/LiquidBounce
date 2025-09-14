<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventory, ClientPlayerInventoryEvent} from "../../../../integration/events";
    import {getPlayerInventory} from "../../../../integration/rest";
    import ItemStackView from "../../common/ItemView/ItemStackView.svelte";
    import {onMount} from "svelte";
    import {expoInOut} from "svelte/easing";
    import {fly} from "svelte/transition";
    import {emptySlotCount} from "../island/Island";
    import Line from "../../common/Trims/Line.svelte";

    let stacks: ItemStack[] = [];
    export let settings: { [name: string]: any };

    function updateStacks(inventory: PlayerInventory) {
        stacks = inventory.main.slice(9);

        const emptySlots = stacks.filter(
            slot => slot.identifier === "air" || slot.identifier === "minecraft:air"
        ).length;


        emptySlotCount.set(emptySlots);
    }

    listen("clientPlayerInventory", (data: ClientPlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });
    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
    });

</script>

<div class="inventory-hud hud-container" id="inventoryhud" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="title">
        <img alt="inventory" class="icon" src="img/hud/inventory/inventory.svg"/>
        <span>Inventory List</span>
    </div>
    {#if settings?.divider}
        <Line gradient={settings?.gradient}/>
    {/if}
    <div class="container">
        {#each stacks as stack (stack)}
            <ItemStackView {stack}/>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .inventory-hud {
    position: relative;
    width: fit-content;
    color: white;
    padding: 6px 10px;
    user-select: none;
  }

  .title {
    display: flex;
    align-items: center;
    font-size: 1em;
    font-weight: bold;
    letter-spacing: 0.1em;
    margin-bottom: 0.5em;

    .icon {
      margin: 0 0.3em;
      height: 1em;
      width: auto;
    }
  }

  .container {
    display: grid;
    grid-template-columns: repeat(9, 32px);
    gap: 4px;
    min-height: 104px;
  }


</style>
