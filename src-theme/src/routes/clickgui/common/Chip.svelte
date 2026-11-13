<script lang="ts">
    import type {Snippet} from "svelte";

    /**
     * A label, or with [onclick] a choice that is on while [active].
     */
    let {active = false, onclick, children}: {
        active?: boolean;
        onclick?: () => void;
        children: Snippet;
    } = $props();
</script>

{#if onclick}
    <button type="button" class="chip choice" class:active aria-pressed={active} {onclick}>
        {@render children()}
    </button>
{:else}
    <span class="chip" class:active>{@render children()}</span>
{/if}

<style lang="scss">
  .chip {
    display: inline-block;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    line-height: 1.2;
    color: var(--clickgui-chip-color);
    background-color: var(--clickgui-chip-background-color);
    border: none;
    border-radius: 3px;
    padding: 3px 6px;
    white-space: nowrap;

    &.active {
      background-color: var(--clickgui-chip-active-background-color);
      color: var(--clickgui-chip-active-color);
    }

    &.choice {
      cursor: pointer;
      transition: ease color 0.2s, ease background-color 0.2s;

      &:hover:not(.active) {
        color: var(--clickgui-text-color);
      }

      &:focus-visible {
        outline: solid 1px var(--clickgui-text-color);
      }
    }
  }
</style>
