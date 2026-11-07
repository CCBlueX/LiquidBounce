<script lang="ts">
    /**
     * A plain label, or a toggle when it has an [onclick].
     */
    let {text, active = false, onclick}: {
        text: string;
        active?: boolean;
        onclick?: () => void;
    } = $props();
</script>

{#if onclick}
    <button type="button" class="chip" class:active {onclick}>{text}</button>
{:else}
    <span class="tag">{text}</span>
{/if}

<style lang="scss">
  .tag {
    font-size: 11px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    background-color: var(--clickgui-global-settings-divider-color);
    padding: 2px 8px;
    border-radius: 999px;
    white-space: nowrap;
  }

  .chip {
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
    color: var(--clickgui-selection-chip-color);
    background-color: var(--clickgui-global-settings-divider-color);
    border: none;
    border-radius: 3px;
    padding: 4px 8px;
    cursor: pointer;
    transition: ease color .2s;

    &:hover {
      color: var(--clickgui-text-color);
    }

    &.active {
      color: var(--clickgui-selection-chip-selected-color);
      background-color: var(--clickgui-selection-chip-selected-background-color);
    }
  }
</style>
