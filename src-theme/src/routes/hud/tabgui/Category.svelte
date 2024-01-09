<script>
    import {fade} from "svelte/transition";

    export let name;
    export let active;
</script>

<div class="category" class:active>
    <div class="category-icon">
        {#if (active)}
            <img transition:fade="{{ duration: 200 }}" src="img/tabgui/{name.toLowerCase()}-active.svg" alt="icon">
        {:else}
            <img transition:fade="{{ duration: 200 }}" src="img/tabgui/{name.toLowerCase()}.svg" alt="icon">
        {/if}
    </div>
    <div class="category-name">
        {name}
    </div>
</div>

<style>
    .category {
        display: flex;
        flex-direction: row;
    }

    .category-name {
        font-weight: 600;
        color: white;
        font-size: 14px;
        width: 100%;
        padding: 7px 12px 7px 12px;

        background: linear-gradient(to left, rgba(0, 0, 0, 0.5) 50%, #7e00ff 50%);
        background-size: 200% 100%;
        background-position: right bottom;
        will-change: background-position;
        transition: background-position .2s ease-out;
        overflow: hidden;
    }

    .category.active .category-name {
        background-position: left bottom;
    }

    .category-icon {
        background-color: rgba(0, 0, 0, 0.68);
        width: 62px;
        position: relative;
    }

    .category-icon img {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        margin: auto;
    }
</style>
