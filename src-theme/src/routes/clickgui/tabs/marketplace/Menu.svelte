<script lang="ts">
    import type {Snippet} from "svelte";
    import {fly} from "svelte/transition";
    import type {MenuEntry} from "./marketplace";

    let {entries, trigger, active = false}: {
        entries: MenuEntry[];
        trigger: Snippet;
        active?: boolean;
    } = $props();

    let open = $state(false);
    let element: HTMLElement;

    function windowClick(e: MouseEvent) {
        if (open && !element.contains(e.target as Node)) {
            open = false;
        }
    }

    function choose(entry: MenuEntry) {
        open = false;
        entry.onclick();
    }
</script>

<svelte:window onclick={windowClick}/>

<div class="menu" bind:this={element}>
    <button type="button" class="trigger" class:active class:open onclick={e => { e.stopPropagation(); open = !open; }}>
        {@render trigger()}
        <span class="arrow"></span>
    </button>

    {#if open}
        <div class="entries" transition:fly={{y: -6, duration: 150}}>
            {#each entries as entry (entry.title)}
                <button type="button" class="entry" class:danger={entry.danger}
                        onclick={e => { e.stopPropagation(); choose(entry); }}>
                    <span class="title">{entry.title}</span>
                    {#if entry.hint}
                        <span class="hint">{entry.hint}</span>
                    {/if}
                </button>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .menu {
    position: relative;
  }

  .trigger {
    font-family: "Inter", sans-serif;
    display: flex;
    align-items: center;
    column-gap: 6px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-tabs-background-color);
    border: solid 1px var(--clickgui-global-settings-divider-color);
    padding: 6px 12px 6px 14px;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color .2s;

    &:hover {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.active {
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--clickgui-tab-active-border-color);
    }
  }

  .arrow {
    width: 9px;
    height: 9px;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    background-size: contain;
    opacity: .6;
    transition: ease transform .2s;
  }

  .open .arrow {
    transform: rotate(180deg);
  }

  .entries {
    position: absolute;
    right: 0;
    top: calc(100% + 6px);
    min-width: 260px;
    display: flex;
    flex-direction: column;
    padding: 4px 0;
    background-color: var(--clickgui-dropdown-background-color);
    border: solid 1px var(--clickgui-dropdown-border-color);
    border-radius: 5px;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    z-index: 100;
  }

  .entry {
    font-family: "Inter", sans-serif;
    display: flex;
    flex-direction: column;
    row-gap: 2px;
    text-align: left;
    background: transparent;
    border: none;
    padding: 7px 14px;
    cursor: pointer;
    transition: ease background-color .2s;

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
    }

    .title {
      font-size: 12px;
      font-weight: 600;
      color: var(--clickgui-dropdown-option-hover-color);
    }

    .hint {
      font-size: 11px;
      font-weight: 500;
      color: var(--clickgui-dropdown-option-color);
    }

    &.danger .title {
      color: var(--clickgui-selection-chip-remove-color);
    }
  }
</style>
