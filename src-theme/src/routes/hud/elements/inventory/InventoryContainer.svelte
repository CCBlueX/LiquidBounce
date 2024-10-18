<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerMainInventoryEvent} from "../../../../integration/events";
    import ItemStackView from "./ItemStackView.svelte";

    let stacks: ItemStack[] = [];

    listen("playerMainInventory", (data: PlayerMainInventoryEvent) => {
        stacks = data.stacks.slice(9);
    });
</script>

<div class="container">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  @import "../../../../colors";

  .container {
    background-color: rgba($hotbar-base-color, 0.34);
    padding: 4px;
    border-radius: 4px;
    display: grid;
    grid-template-columns: repeat(9, minmax(0, 1fr));
    gap: 0.5rem;
  }
</style>
