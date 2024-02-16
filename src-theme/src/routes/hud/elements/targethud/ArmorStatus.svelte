<script lang="ts">
    export let durability: number;

    const maxDurability = 10;
</script>

<div class="armor-status">
    <div class="icon"></div>
    <div class="durability">
        {#each Array.from({ length: maxDurability }, (x, i) => maxDurability - i) as index}
            <div class="point" class:active={index <= durability}></div>
        {/each}
    </div>
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .armor-status {
        display: flex;
        align-items: center;
        column-gap: 5px;
    }

    .icon {
        height: 25px;
        width: 25px;
        border-radius: 50%;
        border: solid 3px $accent-color;
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

            &.active {
                background-color: $accent-color;
            }
        }
    }
</style>
