<script lang="ts">
    let {titles, active = $bindable(0)}: {
        titles: string[];
        active?: number;
    } = $props();
</script>

<div class="tabs" role="tablist">
    {#each titles as title, index (title)}
        <button
                class="tab"
                class:active={index === active}
                onclick={() => (active = index)}
                type="button"
                role="tab"
                aria-selected={index === active}
        >
            {title}
        </button>
    {/each}
</div>

<style lang="scss">
  .tabs {
    display: flex;
    gap: 4px;
    padding: 4px;
    border-radius: 999px;
    background-color: var(--clickgui-segmented-background-color);
  }

  .tab {
    background: transparent;
    color: var(--clickgui-text-dimmed-color);
    padding: 5px 14px;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color .15s, ease color .15s, ease border-color .15s;
    border: solid 1px transparent;

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--clickgui-tab-active-border-color);
    }
  }
</style>
