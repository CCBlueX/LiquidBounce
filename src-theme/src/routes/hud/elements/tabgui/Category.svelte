<script lang="ts">
    import {fade} from "svelte/transition";

    export let name: string;
    export let selected: boolean;
</script>

<div class="category" class:selected>
    <div class="icon">
        {#if selected}
            <img
                transition:fade={{ duration: 200 }}
                src="img/hud/tabgui/{name.toLowerCase()}-active.svg"
                alt="icon"
            />
        {:else}
            <img
                transition:fade={{ duration: 200 }}
                src="img/hud/tabgui/{name.toLowerCase()}.svg"
                alt="icon"
            />
        {/if}
    </div>
    <div class="name">
        {name}
    </div>
</div>

<style lang="scss">
    @use "../../../../colors.scss" as *;

    .name {
        font-weight: 500;
        color: $tabgui-text-color;
        font-size: 14px;
        width: 100%;
        padding: 7px 12px 7px 12px;

        background: linear-gradient(
            to left,
            rgba(0, 0, 0, 0.5) 50%,
            $accent-color 50%
        );
        background-size: 200% 100%;
        background-position: right bottom;
        will-change: background-position;
        transition: background-position 0.2s ease-out;
        overflow: hidden;
    }

    .category {
        display: flex;

        &.selected .name {
            background-position: left bottom;
        }
    }

    .icon {
        background-color: rgba($tabgui-base-color, 0.68);
        width: 62px;
        position: relative;

        img {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 0;
            right: 0;
            margin: auto;
        }
    }
</style>
