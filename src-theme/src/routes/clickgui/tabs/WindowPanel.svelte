<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";

    let { title, icon, children } = $props<{
        title: string;
        icon?: string;
        children: Snippet;
    }>();
</script>

<div class="window" transition:fade|global={{duration: 200, easing: quintOut}}>
    <div class="title">
        {#if icon}
            <img
                    class="icon"
                    src="img/clickgui/icon-{icon}.svg"
                    alt="icon"
            />
        {/if}
        <span class="title-text">{title}</span>
    </div>
    <div class="content">
        {@render children()}
    </div>
</div>

<style lang="scss">

  .window {
    position: fixed;
    top: 70px;
    left: 50%;
    transform: translateX(-50%);
    width: min(820px, 92vw);
    --window-max-height: 70vh;
    background-color: var(--clickgui-window-background-color);
    max-height: var(--window-max-height, none);
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    user-select: none;
  }

  .title {
    display: grid;
    grid-template-columns: max-content 1fr;
    align-items: center;
    column-gap: 12px;
    background-color: var(--clickgui-window-header-background-color);
    padding: 16px 22px;
    font-size: 16px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    border-bottom: 2px solid var(--clickgui-window-header-border-color);
  }

  .title-text {
    font-weight: 600;
  }

  .content {
    padding: 12px 22px 18px;
    overflow: auto;
    max-height: calc(var(--window-max-height, 9999px) - 60px);
  }
</style>
