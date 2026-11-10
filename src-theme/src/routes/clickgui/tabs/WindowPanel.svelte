<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";

    /**
     * A window that fills the screen's height leaves padding and scrolling to its content.
     */
    let {title, icon, width = 820, fill = false, actions, children} = $props<{
        title: string;
        icon?: string;
        width?: number;
        fill?: boolean;
        actions?: Snippet;
        children: Snippet;
    }>();
</script>

<div class="window" class:fill style:--window-width="{width}px" transition:fade|global={{duration: 200, easing: quintOut}}>
    <div class="title">
        {#if icon}
            <img
                    class="icon"
                    src="img/clickgui/icon-{icon}.svg"
                    alt="icon"
            />
        {/if}
        <span class="title-text">{title}</span>
        {#if actions}
            <div class="actions">
                {@render actions()}
            </div>
        {/if}
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
    width: min(var(--window-width), 92%);
    max-height: 70vh;
    display: flex;
    flex-direction: column;
    background-color: var(--clickgui-window-background-color);
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    user-select: none;

    &.fill {
      height: calc(100% - 95px);
      max-height: none;

      .content {
        display: flex;
        padding: 0;
        overflow: hidden;
      }
    }
  }

  .title {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
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

  .actions {
    display: flex;
    align-items: center;
    column-gap: 8px;
    grid-column: 3;
  }

  .content {
    flex: 1;
    min-height: 0;
    padding: 12px 22px 18px;
    overflow: auto;
  }
</style>
