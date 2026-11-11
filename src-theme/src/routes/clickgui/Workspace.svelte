<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";

    /**
     * The one window every area but the HUD editor shares. An area fills the body and decides its own columns.
     */
    let {tabs, search, children}: {
        tabs: Snippet;
        search?: Snippet;
        children: Snippet;
    } = $props();
</script>

<div class="window" transition:fade|global={{duration: 120}}>
    <header class="header">
        <div class="brand">
            <img src="img/clickgui/icon-client.svg" alt="">
            <span>LiquidBounce</span>
        </div>
        {@render tabs()}
        <div class="search">
            {@render search?.()}
        </div>
    </header>
    <div class="body">
        {@render children()}
    </div>
</div>

<style lang="scss">
  .window {
    container: workspace / inline-size;
    position: absolute;
    left: 50%;
    top: 50%;
    translate: -50% -50%;
    width: min(1280px, 100% - 32px);
    height: min(760px, 100% - 32px);
    display: flex;
    flex-direction: column;
    background-color: var(--clickgui-window-background-color);
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    user-select: none;
    color: var(--clickgui-text-color);
  }

  // Thin bars in the accent colour, as the panels had
  .window :global(::-webkit-scrollbar) {
    width: 4px;
    height: 4px;
  }

  .window :global(::-webkit-scrollbar-thumb) {
    border-radius: 2px;
  }

  .header {
    flex: none;
    height: 54px;
    display: flex;
    align-items: center;
    gap: 18px;
    padding: 0 18px;
    background-color: var(--clickgui-window-header-background-color);
    border-bottom: 2px solid var(--clickgui-window-header-border-color);
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 160px;
    font-size: 15px;
    font-weight: 600;

    img {
      width: 18px;
      height: 18px;
    }
  }

  .search {
    flex: 1;
    min-width: 0;
    display: flex;
    justify-content: flex-end;
  }

  .body {
    flex: 1;
    min-height: 0;
    display: grid;
    grid-template: minmax(0, 1fr) / minmax(0, 1fr);
  }

  @container workspace (max-width: 1099px) {
    .brand {
      width: auto;

      span {
        display: none;
      }
    }
  }

  @container workspace (max-width: 699px) {
    .header {
      gap: 10px;
      padding: 0 10px;
    }
  }
</style>
