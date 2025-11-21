<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {ClientPlayerInventoryEvent, PlayerInventory} from "../../../../integration/events";
    import ItemStackView from "./ItemStackView.svelte";
    import {onMount} from "svelte";
    import {getPlayerInventory} from "../../../../integration/rest";

    export let rowLength: number;
    export let backgroundColor: string = "rgba(0,0,0,0.5)";
    export let gap: string = "0.5rem";
    export let getRenderedStacks: (inventory: PlayerInventory) => ItemStack[];

    let stacks: ItemStack[] = [];

    listen("clientPlayerInventory", (data: ClientPlayerInventoryEvent) => {
        stacks = getRenderedStacks(data.inventory);
    });

    onMount(async () => {
        const inventory = await getPlayerInventory();
        stacks = getRenderedStacks(inventory);
    });
</script>

<div class="inventory" style="
    background-color: {backgroundColor};
    gap: {gap};
    --row-length: {rowLength};
">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  .inventory {
    padding: 4px;
    border-radius: 5px;
    display: grid;
    grid-template-columns: repeat(var(--row-length), 1fr);
  }
</style>
