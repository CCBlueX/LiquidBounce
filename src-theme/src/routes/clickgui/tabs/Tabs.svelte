<script lang="ts">
    import type { Component } from "svelte";

    type Tab = {
        title: string;
        content: Component;
    };

    let { tabs, activeTab = $bindable(0) } = $props<{
        tabs: Tab[];
        activeTab?: number;
    }>();

    const Active = $derived(tabs[activeTab]?.content);
</script>

<div class="tabs">
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
  @use "../../../colors.scss" as *;

  .available-tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 5px;
    padding: 6px;
    border-radius: 999px;
    background-color: rgba($clickgui-base-color, 0.85);
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    z-index: 9999999999;
  }

  .tab-button {
    background: transparent;
    color: $clickgui-text-dimmed-color;
    padding: 6px 14px;
    font-size: 14px;
    font-weight: 600;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color 0.2s, ease color 0.2s;
    border: solid 1px transparent;

    &:hover {
      color: $clickgui-text-color;
      background-color: rgba($clickgui-base-color, 0.7);
    }

    &.active {
      color: $clickgui-text-color;
      background-color: rgba($accent-color, 0.15);
      border: 1px solid rgba($accent-color, 0.8);
    }
  }
</style>
