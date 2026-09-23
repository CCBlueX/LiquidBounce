<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import {typing} from "./marketplace";

    let {open = $bindable(), title, width = 560, children, footer}: {
        open: boolean;
        title: string;
        width?: number;
        children: Snippet;
        footer?: Snippet;
    } = $props();
</script>

{#if open}
    <div class="overlay" transition:fade={{duration: 200, easing: quintOut}}>
        <div class="window" style="width: {width}px;" use:typing>
            <div class="title">
                <span>{title}</span>
                <button type="button" class="close" aria-label="Close" onclick={() => open = false}></button>
            </div>
            <div class="content">
                {@render children()}
            </div>
            {#if footer}
                <div class="footer">
                    {@render footer()}
                </div>
            {/if}
        </div>
    </div>
{/if}

<style lang="scss">
  .overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--clickgui-overlay-background-color);
    z-index: 1000;
  }

  .window {
    max-width: calc(100% - 80px);
    max-height: calc(100% - 140px);
    display: flex;
    flex-direction: column;
    background-color: var(--clickgui-base-color);
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
  }

  .title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: var(--clickgui-window-header-background-color);
    padding: 13px 18px;
    font-size: 15px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    border-bottom: solid 2px var(--clickgui-window-header-border-color);
  }

  .close {
    width: 14px;
    height: 14px;
    border: none;
    cursor: pointer;
    background: url("/img/clickgui/icon-cross.svg") center / contain no-repeat;
    opacity: .6;
    transition: ease opacity .2s;

    &:hover {
      opacity: 1;
    }
  }

  .content {
    padding: 4px 18px 16px;
    overflow: auto;
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
  }

  .footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    column-gap: 8px;
    padding: 12px 18px 16px;
    border-top: solid 1px var(--clickgui-global-settings-divider-color);
  }
</style>
