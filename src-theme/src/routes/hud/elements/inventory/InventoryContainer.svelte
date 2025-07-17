<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventory, PlayerInventoryEvent} from "../../../../integration/events";
    import ItemStackView from "./ItemStackView.svelte";
    import {onMount} from "svelte";
    import {getPlayerInventory} from "../../../../integration/rest";

    let stacks: ItemStack[] = [];

    function updateStacks(inventory: PlayerInventory) {
        stacks = inventory.main.slice(9);
    }

    listen("clientPlayerInventory", (data: PlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });

    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
    });
</script>

<div class="container">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .container {
    background-color: rgba($hotbar-base-color, 0.5);
    padding: 4px;
    border-radius: 5px;
    display: grid;
    grid-template-columns: repeat(9, 1fr);
    gap: 0.5rem;
  }
</style>
