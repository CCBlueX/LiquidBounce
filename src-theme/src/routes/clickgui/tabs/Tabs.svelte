<script lang="ts">
    import type {Component} from "svelte";
    import {fly} from "svelte/transition";

    type Tab = {
        title: string;
        content: Component;
    };

    let {tabs, activeTab = $bindable(0)} = $props<{
        tabs: Tab[];
        activeTab?: number;
    }>();

    const Active = $derived(tabs[activeTab]?.content);
</script>

<div class="tabs" transition:fly={{duration: 150, y: -15}}>
    <div class="available-tabs">
        {#each tabs as tab, index (tab.title)}
            <button
                    class="tab-button"
                    class:active={index === activeTab}
                    onclick={() => (activeTab = index)}
                    type="button"
            >
                {tab.title}
            </button>
        {/each}
    </div>

    <div class="content">
        {#if Active}
            {@render Active()}
        {/if}
    </div>
</div>

<style lang="scss">

  .available-tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 4px;
    padding: 5px;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-pill);
    background-color: var(--clickgui-tabs-background-color);
    backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
    box-shadow: var(--elevation-sm) var(--clickgui-tabs-shadow-color);
    z-index: 9999999999;
  }

  .tab-button {
    background: transparent;
    color: var(--clickgui-text-dimmed-color);
    padding: 7px 16px;
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.01em;
    border-radius: var(--radius-pill);
    cursor: pointer;
    transition: background-color var(--duration-fast) var(--ease-standard),
    color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard);
    border: solid 1px transparent;

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--accent-border-strong-color);
    }
  }
</style>
