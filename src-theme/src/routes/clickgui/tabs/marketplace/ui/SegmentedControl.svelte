<script lang="ts">
    let {options, value, onchange}: {
        options: string[];
        value: string;
        onchange: (value: string) => void;
    } = $props();
</script>

<div class="segmented">
    {#each options as option (option)}
        <button type="button" class="option" class:active={option === value} onclick={() => onchange(option)}>
            {option}
        </button>
    {/each}
</div>

<style lang="scss">
  .segmented {
    flex-shrink: 0;
    display: flex;
    gap: 3px;
    padding: 4px;
    border-radius: 999px;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
  }

  .option {
    font-family: "Inter", sans-serif;
    background: transparent;
    color: var(--clickgui-text-dimmed-color);
    padding: 5px 13px;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    border-radius: 999px;
    cursor: pointer;
    border: solid 1px transparent;
    transition: ease background-color .2s, ease color .2s;

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
