<script lang="ts">
    import {REST_BASE} from "../../../../integration/host";
    import type {ItemStack} from "../../../../integration/types";

    export let itemStack: ItemStack;

    let damage = Math.ceil(10 - (itemStack.damage / itemStack.maxDamage * 10));
    $: damage = Math.ceil(10 - (itemStack.damage / itemStack.maxDamage * 10));
</script>

<div class="armor-status">
    <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={itemStack.identifier}" alt={itemStack.identifier} />
    <div class="durability">
        {#each Array.from({ length: 10 }, (x, i) => 10 - i) as index}
            <div class="point" class:active={index <= damage}></div>
        {/each}
    </div>
</div>

<style lang="scss">
    @use "../../../../colors.scss" as *;

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
            background-color: rgba($targethud-base-color, 0.3);
            height: 3px;
            width: 5px;
            border-radius: 1px;
            transition: ease background-color 0.7s;

            &.active {
                background-color: $accent-color;
            }
        }
    }
</style>
