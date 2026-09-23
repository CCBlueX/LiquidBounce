<script lang="ts">
    let {text, active = false, onclick}: {
        text: string;
        active?: boolean;
        onclick?: () => void;
    } = $props();
</script>

{#if onclick}
    <button type="button" class="chip" class:active onclick={onclick}>{text}</button>
{:else}
    <span class="chip" class:active>{text}</span>
{/if}

<style lang="scss">
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
    transition: ease color .2s;

    &.active {
      color: var(--clickgui-selection-chip-selected-color);
      background-color: var(--clickgui-selection-chip-selected-background-color);
    }
  }

  button.chip {
    cursor: pointer;

    &:hover {
      color: var(--clickgui-text-color);
    }

    &.active:hover {
      color: var(--clickgui-selection-chip-selected-color);
    }
  }
</style>
