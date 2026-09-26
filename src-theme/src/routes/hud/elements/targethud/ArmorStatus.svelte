<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {itemTextureUrl} from "../../../../integration/rest";

    export let itemStack: ItemStack;

    let damage = Math.ceil(10 - (itemStack.damage / itemStack.maxDamage * 10));
    $: damage = Math.ceil(10 - (itemStack.damage / itemStack.maxDamage * 10));
</script>

<div class="armor-status">
    <img class="icon" src={itemTextureUrl(itemStack.identifier)} alt={itemStack.identifier} />
    <div class="durability">
        {#each Array.from({ length: 10 }, (x, i) => 10 - i) as index}
            <div class="point" class:active={index <= damage}></div>
        {/each}
    </div>
</div>

<style lang="scss">

    .armor-status {
        display: flex;
        align-items: center;
        column-gap: 5px;
    }

    .icon {
        height: 30px;
        width: 30px;
        image-rendering: pixelated;
    }

    .durability {
        display: flex;
        flex-direction: column;
        row-gap: 1px;

        .point {
            background-color: var(--targethud-armor-point-background-color);
            height: 3px;
            width: 5px;
            border-radius: 1px;
            transition: ease background-color 0.7s;

            &.active {
                background-color: var(--targethud-armor-point-active-color);
            }
        }
    }
</style>
