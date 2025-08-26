<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventory, PlayerInventoryEvent} from "../../../../integration/events";
    import type {ItemStack} from "../../../../integration/types";
    import ItemStackView from "./ItemStackView.svelte";
    import {onMount} from "svelte";
    import {getPlayerInventory} from "../../../../integration/rest";

    let stacks: ItemStack[] = [];

    function updateStacks(inventory: PlayerInventory) {
        stacks = inventory.armor;
    }

    listen("clientPlayerInventory", (data: PlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });

    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
    });
</script>

<div class="armor-items">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  .armor-items {
    position: relative;
    display: flex;
    flex-direction: column-reverse;
    gap: 2px;
  }
</style>
