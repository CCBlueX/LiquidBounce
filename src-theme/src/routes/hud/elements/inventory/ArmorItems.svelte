<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {PlayerArmorInventoryEvent} from "../../../../integration/events";
    import type {ItemStack} from "../../../../integration/types";
    import ItemStackView from "./ItemStackView.svelte";

    let stacks: ItemStack[] = [];

    listen("playerArmorInventory", (data: PlayerArmorInventoryEvent) => {
        stacks = data.stacks;
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
