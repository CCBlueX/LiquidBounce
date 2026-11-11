<script lang="ts" generics="T extends string">
    import {createEventDispatcher} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    /**
     * A few options side by side, one of them chosen.
     */
    export let options: T[];
    export let value: T;
    export let format: ((option: T) => string) | null = null;

    const dispatch = createEventDispatcher();

    $: label = format ?? ((option: string) => $spaceSeperatedNames ? convertToSpacedString(option) : option);

    function choose(option: T) {
        if (option === value) {
            return;
        }

        value = option;
        dispatch("change");
    }
</script>

<div class="segmented" role="radiogroup">
    {#each options as option (option)}
        <button
                type="button"
                role="radio"
                aria-checked={option === value}
                class:active={option === value}
                on:click={() => choose(option)}
        >{label(option)}</button>
    {/each}
</div>

<style lang="scss">
  .segmented {
    display: flex;
    flex: 1 1 auto;
    flex-wrap: wrap;
    gap: 2px;
    min-width: 0;
    padding: 2px;
    border-radius: 4px;
    background-color: var(--clickgui-segmented-background-color);
  }

  button {
    flex: 1 0 auto;
    padding: 4px 9px;
    border: none;
    border-radius: 3px;
    background: none;
    color: var(--clickgui-text-dimmed-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
    cursor: pointer;
    transition: ease color .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-control-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-segmented-active-background-color);
      box-shadow: inset 0 -2px 0 var(--clickgui-segmented-active-border-color);
    }
  }
</style>
