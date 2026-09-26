<script lang="ts">
    import {fade} from "svelte/transition";

    export let name: string;
    export let selected: boolean;

    $: iconPath = `img/hud/tabgui/${name.toLowerCase()}.svg`;
</script>

<div class="category" class:selected>
    <div class="icon">
        <span
            class="category-icon"
            aria-hidden="true"
            transition:fade={{ duration: 200 }}
            style={`mask-image: url('${location.origin}${location.pathname}/${iconPath}');`}
        >
            <img class="category-icon-size" src={iconPath} alt="" />
        </span>
    </div>
    <div class="name">
        {name}
    </div>
</div>

<style lang="scss">

    .name {
        font-weight: 500;
        color: var(--tabgui-text-color);
        font-size: 14px;
        width: 100%;
        padding: 7px 12px 7px 12px;

        background: linear-gradient(
            to left,
            var(--tabgui-category-background-color) 50%,
            var(--tabgui-category-active-background-color) 50%
        );
        background-size: 200% 100%;
        background-position: right bottom;
        will-change: background-position;
        transition: background-position 0.2s ease-out;
        overflow: hidden;
    }

    .category {
        display: flex;

        &.selected .icon {
            color: var(--accent-color);
        }

        &.selected .name {
            background-position: left bottom;
        }
    }

    .icon {
        background-color: var(--tabgui-icon-background-color);
        color: var(--tabgui-text-color);
        width: 62px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: color 0.2s ease-out;
    }

    .category-icon {
        display: inline-block;
        background-color: currentColor;
        mask-position: center;
        mask-repeat: no-repeat;
        mask-size: contain;
    }

    .category-icon-size {
        display: block;
        visibility: hidden;
    }
</style>
